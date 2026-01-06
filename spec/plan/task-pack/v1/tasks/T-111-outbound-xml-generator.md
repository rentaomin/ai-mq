# T-111 Outbound XML 生成器

## Goal

实现 Outbound XML 生成器，将中间 JSON Tree（Request 部分）转换为完整的 `outbound-converter.xml` 文件，用于 MQ 请求报文的序列化/反序列化。

## In Scope / Out of Scope

**In Scope**:
- OutboundXmlGenerator 类实现
- 调用 XmlTemplateEngine 生成 XML 内容
- 文件输出到指定位置
- 正确处理命名空间配置
- 字段顺序与 Excel/JSON Tree 保持一致
- transitory 字段（groupId、occurrenceCount）正确生成
- 错误处理与退出码

**Out of Scope**:
- Inbound XML 生成（T-202）
- XML 类型映射逻辑（T-109）
- XML 模板引擎内部实现（T-110）
- XML Bean 验证（T-301）
- 原子输出管理（T-307）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.1 节（XML Bean 生成器）
- T-110 XmlTemplateEngine 类（提供 XML 生成能力）
- T-108 DeterministicJsonWriter（提供 JSON Tree 读取）
- T-004 MessageModel、FieldGroup（数据模型）
- T-003 Config（配置加载，包括命名空间、输出路径）
- T-005 ParseException、ExitCodes（错误处理）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/xml/
└── OutboundXmlGenerator.java

```

**生成文件输出**（运行时产物）:
```
{output-root}/xml/
└── outbound-converter.xml
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-110 | XmlTemplateEngine 类 |
| T-109 | XmlTypeMapper 类 |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器 |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.generator.Generator;

/**
 * Outbound XML 生成器接口扩展
 */
public interface XmlGenerator extends Generator {
    /**
     * 获取生成的 XML 内容（用于验证）
     */
    String getGeneratedContent();
}
```

### OutboundXmlGenerator 类

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
 * Outbound XML Bean 生成器
 *
 * 生成 outbound-converter.xml 文件，用于 MQ 请求报文的
 * 序列化/反序列化配置。
 */
public class OutboundXmlGenerator implements XmlGenerator {

    private static final String OUTPUT_FILENAME = "outbound-converter.xml";

    private final Config config;
    private final XmlTemplateEngine templateEngine;
    private String generatedContent;

    public OutboundXmlGenerator(Config config) {
        this.config = config;
        this.templateEngine = new XmlTemplateEngine(config);
    }

    /**
     * 从 MessageModel 生成 Outbound XML
     */
    @Override
    public void generate(MessageModel model) throws ParseException {
        // 1. 获取 Request FieldGroup
        FieldGroup request = model.getRequest();
        if (request == null || request.getFields().isEmpty()) {
            throw new ParseException(
                "Request fields are empty, cannot generate outbound XML",
                ExitCodes.GENERATION_ERROR
            );
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
        this.generatedContent = templateEngine.generateOutbound(request, operationId);

        // 4. 写入文件
        writeToFile();
    }

    /**
     * 从 JSON Tree 文件生成 Outbound XML
     */
    public void generateFromJsonTree(Path jsonTreePath) throws ParseException {
        // 读取 JSON Tree 并反序列化为 MessageModel
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
            // 确保目录存在
            Files.createDirectories(outputDir);

            // 写入文件
            Files.writeString(outputFile, generatedContent, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new ParseException(
                "Failed to write outbound XML: " + e.getMessage(),
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
}
```

### 输出 XML 结构示例

根据架构文档 6.1 节和参考文件 A.1，生成的 `outbound-converter.xml` 结构如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns="http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0"
             xmlns:beans="http://www.springframework.org/schema/beans">
  <fix-length-outbound-converter id="req_converter" codeGen="true">
    <message forType="com.rtm.test.CreateApplicationRequest">
      <!-- transitory groupId 字段 -->
      <field type="DataField" length="10" fixedLength="true" transitory="true"
             defaultValue="CREATEAPP" converter="stringFieldConverter" />

      <!-- 普通字段 -->
      <field name="customerId" type="DataField" length="20"
             nullPad=" " converter="stringFieldConverter" />

      <!-- 嵌套对象 -->
      <field name="applicantInfo" type="CompositeField"
             forType="com.rtm.test.ApplicantInfo">
        <field name="firstName" type="DataField" length="30"
               nullPad=" " converter="stringFieldConverter" />
      </field>

      <!-- 数组字段 -->
      <field name="addresses" type="RepeatingField" fixedCount="5"
             forType="com.rtm.test.Address">
        <field name="addressLine1" type="DataField" length="50"
               nullPad=" " converter="stringFieldConverter" />
      </field>
    </message>
  </fix-length-outbound-converter>
</beans:beans>
```

### 关键实现要点

1. **命名空间配置**:
   - 使用 `http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0`
   - 从 Config 读取命名空间配置

2. **forType 命名规则**:
   - 格式: `{groupId}.{artifactId}.{ClassName}`
   - 示例: `com.rtm.test.CreateApplicationRequest`

3. **transitory 字段生成**:
   - groupId 字段: `transitory="true"`, `defaultValue="{groupId}"`
   - occurrenceCount 字段: `transitory="true"`, `pad="0"`, `alignRight="true"`

4. **字段顺序**:
   - 必须与 Excel/JSON Tree 中的顺序完全一致
   - 使用 LinkedHashMap 或 List 保证顺序

## Acceptance Criteria

1. [ ] 生成的 XML 文件符合 Spring beans XML 格式规范
2. [ ] 根元素为 `<beans:beans>`，包含正确的命名空间声明
3. [ ] `<fix-length-outbound-converter>` 元素包含 `id="req_converter"` 和 `codeGen="true"` 属性
4. [ ] `<message>` 元素的 `forType` 属性格式为 `{groupId}.{artifactId}.{ClassName}Request`
5. [ ] 字段顺序与输入 JSON Tree 完全一致
6. [ ] transitory groupId 字段正确生成（无 name 属性，transitory="true"）
7. [ ] transitory occurrenceCount 字段正确生成（counterFieldConverter）
8. [ ] CompositeField 正确生成嵌套结构
9. [ ] RepeatingField 正确生成数组结构，包含 fixedCount 属性
10. [ ] 文件输出到 `{output-root}/xml/outbound-converter.xml`
11. [ ] 使用 UTF-8 编码
12. [ ] XML 声明正确: `<?xml version="1.0" encoding="UTF-8"?>`
13. [ ] 空 Request 输入时抛出 ParseException 并返回 GENERATION_ERROR 退出码
14. [ ] 缺少 operationId 时抛出 ParseException
---

**文档结束**
