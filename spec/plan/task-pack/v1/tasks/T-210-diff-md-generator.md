# T-210 diff.md 生成器

## Goal

实现字段名称映射表生成器（DiffMdGenerator），基于中间 JSON Tree 生成 `diff.md` 文件，记录所有字段从原始名称到规范化名称的映射关系，支持审计追溯和人工审查。

## In Scope / Out of Scope

**In Scope**:
- DiffMdGenerator 类实现
- Markdown 格式的字段映射表生成
- 原始名称 -> camelCase 名称映射记录
- 来源信息记录（sheetName、rowIndex）
- 按 Sheet 分组展示（Shared Header、Request、Response）
- 生成元数据（时间戳、源文件、版本）
- 确定性输出（相同输入 -> 相同输出）
- 单元测试

**Out of Scope**:
- JSON Tree 生成（T-108 已完成）
- 命名规范化逻辑（T-106 已完成）
- 审计日志记录（T-308）
- 跨制品一致性验证（T-304）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.4 节（diff.md 生成器）
- T-108 JSON Tree 序列化（MessageModel、FieldNode 数据结构）
- T-004 数据模型定义（FieldNode、SourceMetadata）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/
└── DiffMdGenerator.java

src/test/java/com/rtm/mq/tool/generator/
└── DiffMdGeneratorTest.java

生成的制品（运行时）:
{output-root}/diff.md
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-108 | DeterministicJsonWriter、JSON Tree 结构定义 |
| T-004 | FieldNode、MessageModel、SourceMetadata 数据模型 |

## Implementation Notes

### 输出文件格式

根据架构文档 6.4.4，diff.md 格式如下：

```markdown
# 字段名称映射表

生成时间: 2026-01-04T10:00:00Z
源文件: /path/to/spec.xlsx
解析器版本: 1.0.0

---

## Shared Header

| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |
|---------|---------------|-----------|------|
| Operation Name | operationName | SharedHeader | 2 |
| Version | version | SharedHeader | 3 |

---

## Request

| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |
|---------|---------------|-----------|------|
| CreateApp:CreateApplication | createApp | Request | 9 |
| Domicle Branche | domicleBranche | Request | 10 |
| Product Del:ProductDetails | productDel | Request | 15 |

---

## Response

| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |
|---------|---------------|-----------|------|
| Response Code | responseCode | Response | 9 |
| Error Message | errorMessage | Response | 10 |

---

共计 7 个字段映射
```

### DiffMdGenerator 类

```java
package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * diff.md 生成器
 * 生成字段名称映射表，记录原始名称到 camelCase 的转换
 */
public class DiffMdGenerator {

    private static final String LINE_SEP = System.lineSeparator();

    /**
     * 生成 diff.md 文件
     *
     * @param model 消息模型（包含完整的 JSON Tree）
     * @param outputPath 输出文件路径
     */
    public void generate(MessageModel model, Path outputPath) throws IOException {
        String content = generateContent(model);

        // 确保父目录存在
        Files.createDirectories(outputPath.getParent());

        // UTF-8 编码写入
        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    }

    /**
     * 生成 Markdown 内容
     */
    public String generateContent(MessageModel model) {
        StringBuilder md = new StringBuilder();
        int totalCount = 0;

        // 标题和元数据
        md.append("# 字段名称映射表").append(LINE_SEP).append(LINE_SEP);
        md.append("生成时间: ").append(model.getMetadata().getParseTimestamp()).append(LINE_SEP);
        md.append("源文件: ").append(model.getMetadata().getSourceFile()).append(LINE_SEP);
        md.append("解析器版本: ").append(model.getMetadata().getParserVersion()).append(LINE_SEP);
        md.append(LINE_SEP);
        md.append("---").append(LINE_SEP).append(LINE_SEP);

        // Shared Header 部分
        if (model.getSharedHeader() != null &&
            !model.getSharedHeader().getFields().isEmpty()) {
            md.append("## Shared Header").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getSharedHeader().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // Request 部分
        if (model.getRequest() != null &&
            !model.getRequest().getFields().isEmpty()) {
            md.append("## Request").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getRequest().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // Response 部分
        if (model.getResponse() != null &&
            !model.getResponse().getFields().isEmpty()) {
            md.append("## Response").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getResponse().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // 汇总
        md.append("共计 ").append(totalCount).append(" 个字段映射").append(LINE_SEP);

        return md.toString();
    }

    /**
     * 添加字段映射表格
     * 递归处理嵌套字段
     *
     * @return 处理的字段数量
     */
    private int appendFieldTable(StringBuilder md, List<FieldNode> fields) {
        // 表头
        md.append("| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |").append(LINE_SEP);
        md.append("|---------|---------------|-----------|------|").append(LINE_SEP);

        // 递归添加所有字段
        return appendFieldRows(md, fields);
    }

    /**
     * 递归添加字段行
     */
    private int appendFieldRows(StringBuilder md, List<FieldNode> fields) {
        int count = 0;
        for (FieldNode field : fields) {
            // 添加当前字段
            appendFieldRow(md, field);
            count++;

            // 递归处理子字段
            if (!field.getChildren().isEmpty()) {
                count += appendFieldRows(md, field.getChildren());
            }
        }
        return count;
    }

    /**
     * 添加单个字段行
     */
    private void appendFieldRow(StringBuilder md, FieldNode field) {
        String originalName = escapeMarkdown(field.getOriginalName());
        String camelCaseName = field.getCamelCaseName() != null ?
            field.getCamelCaseName() : "-";
        String sheetName = field.getSource() != null ?
            field.getSource().getSheetName() : "-";
        int rowIndex = field.getSource() != null ?
            field.getSource().getRowIndex() : 0;

        md.append("| ")
          .append(originalName)
          .append(" | ")
          .append(camelCaseName)
          .append(" | ")
          .append(sheetName)
          .append(" | ")
          .append(rowIndex)
          .append(" |")
          .append(LINE_SEP);
    }

    /**
     * 转义 Markdown 特殊字符
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "-";
        }
        // 转义管道符和反斜杠
        return text.replace("\\", "\\\\")
                   .replace("|", "\\|");
    }

    /**
     * 统计总字段数（包括嵌套）
     */
    public int countTotalFields(MessageModel model) {
        int count = 0;
        if (model.getSharedHeader() != null) {
            count += countFields(model.getSharedHeader().getFields());
        }
        if (model.getRequest() != null) {
            count += countFields(model.getRequest().getFields());
        }
        if (model.getResponse() != null) {
            count += countFields(model.getResponse().getFields());
        }
        return count;
    }

    /**
     * 递归统计字段数
     */
    private int countFields(List<FieldNode> fields) {
        int count = 0;
        for (FieldNode field : fields) {
            count++;
            count += countFields(field.getChildren());
        }
        return count;
    }
}
```

### 确定性保证

1. **字段顺序**: 按 JSON Tree 中的顺序输出（与 Excel 一致）
2. **换行符**: 使用 `System.lineSeparator()` 确保跨平台一致
3. **编码**: UTF-8
4. **时间戳**: 来自 MessageModel.metadata.parseTimestamp（解析时已固定）

### 输出位置

根据架构文档 6.4.3：
```
{output-root}/
├── diff.md          <-- 本任务生成
├── xml/
├── java/
├── openapi/
└── audit/
```

### 递归处理嵌套字段

嵌套对象和数组的子字段也需要记录到映射表中：

```markdown
| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |
|---------|---------------|-----------|------|
| CreateApp:CreateApplication | createApp | Request | 9 |
| Domicle Branche | domicleBranche | Request | 10 |    <-- CreateApp 的子字段
| Account Number | accountNumber | Request | 11 |       <-- CreateApp 的子字段
| Product Del:ProductDetails | productDel | Request | 15 |
| Card Type | cardType | Request | 16 |                  <-- ProductDetails 的子字段
```

### 空字段组处理

如果某个 FieldGroup 为空（如没有 Response），则跳过该部分，不输出空表格。

## Acceptance Criteria

1. [ ] DiffMdGenerator 类编译通过
2. [ ] 生成的 diff.md 格式正确（符合 Markdown 规范）
3. [ ] 包含完整元数据（时间戳、源文件、版本）
4. [ ] 按 Sheet 分组展示（Shared Header、Request、Response）
5. [ ] 字段顺序与 JSON Tree 一致
6. [ ] 递归处理嵌套字段（子字段也记录）
7. [ ] 正确显示原始名称和 camelCase 名称
8. [ ] 正确显示来源信息（sheetName、rowIndex）
9. [ ] 空字段组不输出空表格
10. [ ] Markdown 特殊字符正确转义
11. [ ] 确定性输出（相同输入 -> 相同输出）
12. [ ] UTF-8 编码
13. [ ] 字段总数统计正确
14. [ ] 单元测试覆盖所有场景

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 生成基本 diff.md | 格式正确 |
| 单元测试 | 元数据输出 | 包含时间戳、源文件、版本 |
| 单元测试 | Request 字段列表 | 按顺序输出 |
| 单元测试 | Response 字段列表 | 按顺序输出 |
| 单元测试 | Shared Header 字段列表 | 按顺序输出 |
| 单元测试 | 嵌套字段递归 | 子字段也被记录 |
| 单元测试 | 空 Response | 跳过 Response 部分 |
| 单元测试 | Markdown 特殊字符 | 正确转义 |
| 单元测试 | 字段总数统计 | 数值正确 |
| 单元测试 | 确定性验证 | 两次生成结果相同 |
| 集成测试 | 写入文件 | 文件正确创建 |
| Golden 测试 | 对比参考 diff.md | 内容一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.*;
import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DiffMdGeneratorTest {

    private DiffMdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DiffMdGenerator();
    }

    @Test
    void shouldGenerateBasicDiffMd() {
        // Given
        MessageModel model = createTestModel();

        // When
        String content = generator.generateContent(model);

        // Then
        assertTrue(content.contains("# 字段名称映射表"));
        assertTrue(content.contains("## Request"));
        assertTrue(content.contains("| 原始名称 | camelCase 名称 |"));
    }

    @Test
    void shouldIncludeMetadata() {
        // Given
        MessageModel model = createTestModel();

        // When
        String content = generator.generateContent(model);

        // Then
        assertTrue(content.contains("生成时间:"));
        assertTrue(content.contains("源文件:"));
        assertTrue(content.contains("解析器版本:"));
    }

    @Test
    void shouldIncludeFieldMappings() {
        // Given
        MessageModel model = createTestModel();

        // When
        String content = generator.generateContent(model);

        // Then
        assertTrue(content.contains("| CreateApp | createApp |"));
        assertTrue(content.contains("| Request | 9 |"));
    }

    @Test
    void shouldIncludeNestedFields() {
        // Given
        MessageModel model = createModelWithNestedFields();

        // When
        String content = generator.generateContent(model);

        // Then
        // 父字段
        assertTrue(content.contains("| ParentField | parentField |"));
        // 子字段
        assertTrue(content.contains("| ChildField | childField |"));
    }

    @Test
    void shouldSkipEmptyResponse() {
        // Given
        MessageModel model = createTestModel();
        model.setResponse(new FieldGroup()); // 空 Response

        // When
        String content = generator.generateContent(model);

        // Then
        assertFalse(content.contains("## Response"));
    }

    @Test
    void shouldEscapeMarkdownCharacters() {
        // Given
        FieldNode field = FieldNode.builder()
            .originalName("Field|With|Pipes")
            .camelCaseName("fieldWithPipes")
            .source(new SourceMetadata("Request", 10))
            .build();

        MessageModel model = createTestModel();
        model.getRequest().getFields().add(field);

        // When
        String content = generator.generateContent(model);

        // Then
        assertTrue(content.contains("Field\\|With\\|Pipes"));
    }

    @Test
    void shouldCountTotalFields() {
        // Given
        MessageModel model = createModelWithNestedFields();

        // When
        int count = generator.countTotalFields(model);

        // Then
        assertEquals(4, count); // 2 顶层 + 2 嵌套
    }

    @Test
    void shouldProduceDeterministicOutput() {
        // Given
        MessageModel model = createTestModel();

        // When
        String content1 = generator.generateContent(model);
        String content2 = generator.generateContent(model);

        // Then
        assertEquals(content1, content2);
    }

    private MessageModel createTestModel() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setSourceFile("/path/to/spec.xlsx");
        metadata.setParserVersion("1.0.0");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
            .originalName("CreateApp")
            .camelCaseName("createApp")
            .source(new SourceMetadata("Request", 9))
            .build());
        model.setRequest(request);

        model.setResponse(new FieldGroup());
        model.setSharedHeader(new FieldGroup());

        return model;
    }

    private MessageModel createModelWithNestedFields() {
        MessageModel model = createTestModel();

        FieldNode parent = FieldNode.builder()
            .originalName("ParentField")
            .camelCaseName("parentField")
            .source(new SourceMetadata("Request", 10))
            .children(Arrays.asList(
                FieldNode.builder()
                    .originalName("ChildField")
                    .camelCaseName("childField")
                    .source(new SourceMetadata("Request", 11))
                    .build()
            ))
            .build();

        model.getRequest().addField(parent);

        return model;
    }
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| originalName 包含 Markdown 特殊字符 | 表格格式破坏 | 转义管道符和反斜杠 |
| camelCaseName 为 null | 显示异常 | 使用 "-" 作为占位符 |
| SourceMetadata 为 null | NullPointerException | 防御性检查 |
| 超深嵌套 | 递归栈溢出 | 设置最大深度（与 T-104 一致） |
| 大量字段 | 文件过大 | 无影响（Markdown 支持） |
| 换行符不一致 | 跨平台差异 | 使用 System.lineSeparator() |
| 编码问题 | 中文乱码 | 强制 UTF-8 |
| 字段顺序不一致 | 审计困难 | 使用 List 保证顺序 |

---

**文档结束**
