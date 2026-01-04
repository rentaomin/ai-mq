# T-202 Inbound XML 生成器

## Goal

实现 Inbound XML 生成器，将中间 JSON Tree（Response 部分）转换为完整的 `inbound-converter.xml` 文件，用于 MQ 响应报文的序列化/反序列化配置。

## In Scope / Out of Scope

**In Scope**:
- InboundXmlGenerator 类实现
- 调用 XmlTemplateEngine 生成 Inbound XML 内容
- 文件输出到指定位置
- 正确处理命名空间配置
- 字段顺序与 Excel/JSON Tree（Response Sheet）保持一致
- transitory 字段（groupId、occurrenceCount）正确生成
- 处理 Response 为空的情况
- 错误处理与退出码

**Out of Scope**:
- Outbound XML 生成（T-111 已完成）
- XML 类型映射逻辑（T-109 已完成）
- XML 模板引擎内部实现（T-110 已完成）
- XML Bean 验证（T-301）
- 原子输出管理（T-307）
- Response Sheet 解析（T-201 已完成）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.1 节（XML Bean 生成器）
- T-201 Response Sheet 解析结果（MessageModel.response）
- T-111 OutboundXmlGenerator（提供参考实现和复用模式）
- T-110 XmlTemplateEngine 类（提供 XML 生成能力）
- T-108 DeterministicJsonWriter（提供 JSON Tree 读取）
- T-004 MessageModel、FieldGroup（数据模型）
- T-003 Config（配置加载，包括命名空间、输出路径）
- T-005 ParseException、ExitCodes（错误处理）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/xml/
└── InboundXmlGenerator.java

src/test/java/com/rtm/mq/tool/generator/xml/
└── InboundXmlGeneratorTest.java
```

**生成文件输出**（运行时产物）:
```
{output-root}/xml/
└── inbound-converter.xml
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-201 | Response Sheet 解析（MessageModel.response 正确填充） |
| T-111 | OutboundXmlGenerator（参考实现、复用 XmlGenerator 接口） |
| T-110 | XmlTemplateEngine 类 |
| T-109 | XmlTypeMapper 类 |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器 |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心实现

InboundXmlGenerator 与 OutboundXmlGenerator 结构高度相似，主要区别在于：
1. 处理 Response FieldGroup 而非 Request
2. 生成 `fix-length-inbound-converter` 而非 `fix-length-outbound-converter`
3. 使用 `id="resp_converter"` 而非 `id="req_converter"`
4. forType 使用 `{ClassName}Response` 命名

### InboundXmlGenerator 类

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.exception.ExitCodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Inbound XML Bean 生成器
 *
 * 生成 inbound-converter.xml 文件，用于 MQ 响应报文的
 * 序列化/反序列化配置。
 */
public class InboundXmlGenerator implements XmlGenerator {

    private static final String OUTPUT_FILENAME = "inbound-converter.xml";

    private final Config config;
    private final XmlTemplateEngine templateEngine;
    private String generatedContent;

    public InboundXmlGenerator(Config config) {
        this.config = config;
        this.templateEngine = new XmlTemplateEngine(config);
    }

    /**
     * 从 MessageModel 生成 Inbound XML
     */
    @Override
    public void generate(MessageModel model) throws ParseException {
        // 1. 获取 Response FieldGroup
        FieldGroup response = model.getResponse();

        // Response 可以为空（某些消息只有 Request）
        if (response == null || response.getFields().isEmpty()) {
            // 生成空的 inbound-converter.xml（仅包含根元素）
            this.generatedContent = templateEngine.generateEmptyInbound();
            writeToFile();
            return;
        }

        // 2. 获取 operationId
        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new ParseException(
                "Operation ID is required for XML generation",
                ExitCodes.GENERATION_ERROR
            );
        }

        // 3. 调用模板引擎生成 XML 内容
        this.generatedContent = templateEngine.generateInbound(response, operationId);

        // 4. 写入文件
        writeToFile();
    }

    /**
     * 从 JSON Tree 文件生成 Inbound XML
     */
    public void generateFromJsonTree(Path jsonTreePath) throws ParseException {
        MessageModel model = loadMessageModel(jsonTreePath);
        generate(model);
    }

    /**
     * 写入 XML 文件到输出目录
     */
    private void writeToFile() throws ParseException {
        Path outputDir = Path.of(config.getOutput().getRoot(), "xml");
        Path outputFile = outputDir.resolve(OUTPUT_FILENAME);

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, generatedContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParseException(
                "Failed to write inbound XML: " + e.getMessage(),
                ExitCodes.IO_ERROR
            );
        }
    }

    /**
     * 加载 MessageModel 从 JSON Tree 文件
     */
    private MessageModel loadMessageModel(Path jsonTreePath) throws ParseException {
        try {
            String json = Files.readString(jsonTreePath, StandardCharsets.UTF_8);
            return MessageModel.fromJson(json);
        } catch (IOException e) {
            throw new ParseException(
                "Failed to read JSON Tree: " + e.getMessage(),
                ExitCodes.IO_ERROR
            );
        }
    }

    @Override
    public String getGeneratedContent() {
        return generatedContent;
    }

    /**
     * 获取输出文件路径
     */
    public Path getOutputPath() {
        return Path.of(config.getOutput().getRoot(), "xml", OUTPUT_FILENAME);
    }

    /**
     * 检查是否有 Response 数据
     */
    public boolean hasResponseData(MessageModel model) {
        FieldGroup response = model.getResponse();
        return response != null && !response.getFields().isEmpty();
    }
}
```

### XmlTemplateEngine 扩展

需要确保 T-110 的 XmlTemplateEngine 提供以下方法：

```java
/**
 * 生成 Inbound XML 内容
 */
public String generateInbound(FieldGroup response, String operationId);

/**
 * 生成空的 Inbound XML（当 Response 为空时）
 */
public String generateEmptyInbound();
```

### 输出 XML 结构示例

根据架构文档 6.1 节，生成的 `inbound-converter.xml` 结构如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns="http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0"
             xmlns:beans="http://www.springframework.org/schema/beans">
  <fix-length-inbound-converter id="resp_converter" codeGen="true">
    <message forType="com.rtm.test.CreateApplicationResponse">
      <!-- transitory groupId 字段 -->
      <field type="DataField" length="10" fixedLength="true" transitory="true"
             defaultValue="CREATEAPP" converter="stringFieldConverter" />

      <!-- 返回码字段 -->
      <field name="returnCode" type="DataField" length="4"
             nullPad=" " converter="stringFieldConverter" />

      <!-- 返回消息字段 -->
      <field name="returnMessage" type="DataField" length="100"
             nullPad=" " converter="stringFieldConverter" />

      <!-- 嵌套对象 -->
      <field name="responseData" type="CompositeField"
             forType="com.rtm.test.ResponseData">
        <field name="applicationId" type="DataField" length="20"
               nullPad=" " converter="stringFieldConverter" />
      </field>

      <!-- 数组字段 -->
      <field name="errorDetails" type="RepeatingField" fixedCount="10"
             forType="com.rtm.test.ErrorDetail">
        <field name="errorCode" type="DataField" length="10"
               nullPad=" " converter="stringFieldConverter" />
      </field>
    </message>
  </fix-length-inbound-converter>
</beans:beans>
```

### 与 OutboundXmlGenerator 的主要区别

| 属性/元素 | Outbound | Inbound |
|----------|----------|---------|
| 根元素 | `fix-length-outbound-converter` | `fix-length-inbound-converter` |
| id 属性 | `req_converter` | `resp_converter` |
| forType 后缀 | `Request` | `Response` |
| 数据来源 | MessageModel.request | MessageModel.response |
| 空数据处理 | 抛出异常 | 生成空文件 |

### 关键实现要点

1. **命名空间配置**:
   - 使用与 Outbound 相同的命名空间 `http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0`
   - 从 Config 读取命名空间配置

2. **forType 命名规则**:
   - 格式: `{groupId}.{artifactId}.{ClassName}Response`
   - 示例: `com.rtm.test.CreateApplicationResponse`

3. **transitory 字段生成**:
   - groupId 字段: `transitory="true"`, `defaultValue="{groupId}"`
   - occurrenceCount 字段: `transitory="true"`, `pad="0"`, `alignRight="true"`

4. **字段顺序**:
   - 必须与 Excel/JSON Tree（Response Sheet）中的顺序完全一致
   - 使用 LinkedHashMap 或 List 保证顺序

5. **空 Response 处理**:
   - Response 为空时生成仅包含根元素的 XML 文件
   - 不抛出异常（与 Outbound 不同）

## Acceptance Criteria

1. [ ] 生成的 XML 文件符合 Spring beans XML 格式规范
2. [ ] 根元素为 `<beans:beans>`，包含正确的命名空间声明
3. [ ] `<fix-length-inbound-converter>` 元素包含 `id="resp_converter"` 和 `codeGen="true"` 属性
4. [ ] `<message>` 元素的 `forType` 属性格式为 `{groupId}.{artifactId}.{ClassName}Response`
5. [ ] 字段顺序与输入 JSON Tree（Response 部分）完全一致
6. [ ] transitory groupId 字段正确生成（无 name 属性，transitory="true"）
7. [ ] transitory occurrenceCount 字段正确生成（counterFieldConverter）
8. [ ] CompositeField 正确生成嵌套结构
9. [ ] RepeatingField 正确生成数组结构，包含 fixedCount 属性
10. [ ] 文件输出到 `{output-root}/xml/inbound-converter.xml`
11. [ ] 使用 UTF-8 编码
12. [ ] XML 声明正确: `<?xml version="1.0" encoding="UTF-8"?>`
13. [ ] Response 为空时生成空 XML 文件（仅根元素，不抛异常）
14. [ ] 缺少 operationId 且 Response 非空时抛出 ParseException
15. [ ] XmlGenerator 接口正确实现

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 简单 Response 生成 | XML 结构正确 |
| 单元测试 | 嵌套对象生成 | CompositeField 正确 |
| 单元测试 | 数组字段生成 | RepeatingField 正确，fixedCount 正确 |
| 单元测试 | transitory groupId 字段 | transitory="true"，无 name |
| 单元测试 | transitory occurrenceCount 字段 | counterFieldConverter |
| 单元测试 | 字段顺序验证 | 顺序与输入一致 |
| 单元测试 | 空 Response 处理 | 生成空 XML，不抛异常 |
| 单元测试 | 缺少 operationId（非空 Response） | 抛出 ParseException |
| 单元测试 | inbound-converter vs outbound-converter | id 和根元素名称不同 |
| 集成测试 | 从 JSON Tree 文件生成 | 文件正确写入 |
| 集成测试 | 与 XmlTemplateEngine 集成 | 内容正确 |
| 集成测试 | 与 T-201 Response 解析集成 | 端到端正确 |
| Golden 测试 | 对比参考 inbound-converter.xml | 结构一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.*;
import com.rtm.mq.tool.exception.ParseException;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InboundXmlGeneratorTest {

    private InboundXmlGenerator generator;
    private Config config;

    @BeforeEach
    void setUp() {
        config = TestConfig.createDefault();
        generator = new InboundXmlGenerator(config);
    }

    @Test
    void shouldGenerateSimpleResponse() throws ParseException {
        // Given
        MessageModel model = createSimpleResponseModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertNotNull(content);
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(content.contains("fix-length-inbound-converter"));
        assertTrue(content.contains("id=\"resp_converter\""));
    }

    @Test
    void shouldGenerateCompositeField() throws ParseException {
        // Given
        MessageModel model = createModelWithNestedObject();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("type=\"CompositeField\""));
    }

    @Test
    void shouldGenerateRepeatingField() throws ParseException {
        // Given
        MessageModel model = createModelWithArray();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("type=\"RepeatingField\""));
        assertTrue(content.contains("fixedCount=\"10\""));
    }

    @Test
    void shouldHandleEmptyResponse() throws ParseException {
        // Given
        MessageModel model = new MessageModel();
        model.setResponse(new FieldGroup()); // empty
        model.setMetadata(new Metadata("TestOp"));

        // When
        generator.generate(model);

        // Then - 应生成空 XML，不抛异常
        String content = generator.getGeneratedContent();
        assertNotNull(content);
        assertTrue(content.contains("fix-length-inbound-converter"));
        // 空 response 不应有 message 元素
    }

    @Test
    void shouldThrowExceptionForMissingOperationIdWhenResponseNotEmpty() {
        // Given
        MessageModel model = new MessageModel();
        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
            .camelCaseName("testField")
            .build());
        model.setResponse(response);
        model.setMetadata(new Metadata(null)); // no operationId

        // When & Then
        ParseException ex = assertThrows(ParseException.class,
            () -> generator.generate(model));
        assertTrue(ex.getMessage().contains("Operation ID"));
    }

    @Test
    void shouldUseCorrectConverterIdForInbound() throws ParseException {
        // Given
        MessageModel model = createSimpleResponseModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("id=\"resp_converter\""));
        assertFalse(content.contains("id=\"req_converter\""));
    }

    @Test
    void shouldUseCorrectForTypeSuffixForResponse() throws ParseException {
        // Given
        MessageModel model = createSimpleResponseModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("Response"));
    }

    @Test
    void shouldPreserveFieldOrder() throws ParseException {
        // Given
        MessageModel model = createModelWithMultipleFields();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        int pos1 = content.indexOf("name=\"field1\"");
        int pos2 = content.indexOf("name=\"field2\"");
        int pos3 = content.indexOf("name=\"field3\"");
        assertTrue(pos1 < pos2 && pos2 < pos3, "Field order must be preserved");
    }

    // Helper methods
    private MessageModel createSimpleResponseModel() {
        MessageModel model = new MessageModel();
        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("String")
            .length(4)
            .build());
        model.setResponse(response);
        model.setMetadata(new Metadata("CreateApplication"));
        return model;
    }

    private MessageModel createModelWithNestedObject() {
        // ... implementation
        return null;
    }

    private MessageModel createModelWithArray() {
        // ... implementation
        return null;
    }

    private MessageModel createModelWithMultipleFields() {
        // ... implementation
        return null;
    }
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Response 为空 | 生成无效/空 XML | 生成空根元素，文档说明 |
| 深层嵌套结构 | XML 格式混乱 | 递归测试，限制嵌套深度警告 |
| 特殊字符在字段名/值中 | XML 解析失败 | 使用 XmlFormatter 转义 |
| 文件写入失败 | 部分文件残留 | 依赖 T-307 原子输出管理器 |
| 命名空间配置缺失 | XML 无法被下游组件加载 | 提供默认命名空间配置 |
| forType 包名拼接错误 | Java 类找不到 | 严格按照配置格式化 |
| 与 OutboundXmlGenerator 不一致 | 验证失败 | 共享 XmlTemplateEngine |
| Response 有数据但 operationId 缺失 | 生成失败 | 明确错误信息 |
| 并发生成 | 文件覆盖 | 单线程生成或加锁 |

---

**文档结束**
