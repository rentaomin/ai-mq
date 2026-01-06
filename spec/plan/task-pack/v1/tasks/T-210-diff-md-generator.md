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
---

**文档结束**
