# T-110 XML 模板引擎

## Goal

实现 XML 模板引擎，将字段映射结果渲染为 XML Bean 格式，支持模板替换和嵌套结构生成。

## In Scope / Out of Scope

**In Scope**:
- XmlTemplateEngine 类实现
- XML 元素生成
- 嵌套结构处理
- 命名空间配置
- 格式化输出

**Out of Scope**:
- 完整 XML 生成器（T-111）
- 模板文件管理

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.1 节（XML Bean 生成器）
- T-109 XML 字段类型映射

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/xml/
├── XmlTemplateEngine.java
├── XmlElement.java
└── XmlFormatter.java
```

## Dependencies

- T-109 XML 字段类型映射
- T-003 配置加载器

## Implementation Notes

### XmlElement 类

```java
package com.rtm.mq.tool.generator.xml;

import java.util.*;

/**
 * XML 元素表示
 */
public class XmlElement {
    private String tagName;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private List<XmlElement> children = new ArrayList<>();
    private String textContent;

    public XmlElement(String tagName) {
        this.tagName = tagName;
    }

    public XmlElement attribute(String name, String value) {
        if (value != null) {
            attributes.put(name, value);
        }
        return this;
    }

    public XmlElement addChild(XmlElement child) {
        children.add(child);
        return this;
    }

    public XmlElement text(String content) {
        this.textContent = content;
        return this;
    }

    public String getTagName() { return tagName; }
    public Map<String, String> getAttributes() { return attributes; }
    public List<XmlElement> getChildren() { return children; }
    public String getTextContent() { return textContent; }
}
```

### XmlFormatter 类

```java
package com.rtm.mq.tool.generator.xml;

/**
 * XML 格式化器
 */
public class XmlFormatter {

    private static final String INDENT = "  ";
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * 格式化 XML 元素为字符串
     */
    public String format(XmlElement root) {
        StringBuilder sb = new StringBuilder();
        sb.append(XML_DECLARATION).append("\n");
        formatElement(sb, root, 0);
        return sb.toString();
    }

    private void formatElement(StringBuilder sb, XmlElement element, int level) {
        String indent = getIndent(level);

        // 开始标签
        sb.append(indent).append("<").append(element.getTagName());

        // 属性
        for (Map.Entry<String, String> attr : element.getAttributes().entrySet()) {
            sb.append(" ").append(attr.getKey()).append("=\"")
              .append(escapeXml(attr.getValue())).append("\"");
        }

        // 子元素或文本
        if (element.getChildren().isEmpty() && element.getTextContent() == null) {
            sb.append(" />\n");
        } else if (element.getTextContent() != null) {
            sb.append(">").append(escapeXml(element.getTextContent()));
            sb.append("</").append(element.getTagName()).append(">\n");
        } else {
            sb.append(">\n");
            for (XmlElement child : element.getChildren()) {
                formatElement(sb, child, level + 1);
            }
            sb.append(indent).append("</").append(element.getTagName()).append(">\n");
        }
    }

    private String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
```

### XmlTemplateEngine 类

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.FieldGroup;

import java.util.List;

/**
 * XML 模板引擎
 */
public class XmlTemplateEngine {

    private final Config config;
    private final XmlTypeMapper typeMapper;
    private final XmlFormatter formatter;

    public XmlTemplateEngine(Config config) {
        this.config = config;
        this.typeMapper = new XmlTypeMapper(config);
        this.formatter = new XmlFormatter();
    }

    /**
     * 生成 Outbound XML (Request)
     */
    public String generateOutbound(FieldGroup request, String operationId) {
        XmlElement root = createRootElement("fix-length-outbound-converter", "req_converter");
        XmlElement message = createMessageElement(operationId + "Request");

        // 添加字段
        addFields(message, request.getFields());

        root.addChild(message);

        return formatter.format(wrapWithBeans(root, config.getXml().getNamespace().getOutbound()));
    }

    /**
     * 生成 Inbound XML (Response)
     */
    public String generateInbound(FieldGroup response, String operationId) {
        XmlElement root = createRootElement("fix-length-inbound-converter", "resp_converter");
        XmlElement message = createMessageElement(operationId + "Response");

        // 添加字段
        addFields(message, response.getFields());

        root.addChild(message);

        return formatter.format(wrapWithBeans(root, config.getXml().getNamespace().getInbound()));
    }

    /**
     * 创建根转换器元素
     */
    private XmlElement createRootElement(String tagName, String id) {
        return new XmlElement(tagName)
            .attribute("id", id)
            .attribute("codeGen", "true");
    }

    /**
     * 创建 message 元素
     */
    private XmlElement createMessageElement(String className) {
        String forType = buildForType(className);
        return new XmlElement("message")
            .attribute("forType", forType);
    }

    /**
     * 添加字段到父元素
     */
    private void addFields(XmlElement parent, List<FieldNode> fields) {
        for (FieldNode field : fields) {
            XmlElement fieldElement = createFieldElement(field);
            parent.addChild(fieldElement);

            // 递归添加子字段
            if (!field.getChildren().isEmpty() &&
                (field.isObject() || field.isArray())) {
                addFields(fieldElement, field.getChildren());
            }
        }
    }

    /**
     * 创建字段元素
     */
    private XmlElement createFieldElement(FieldNode node) {
        XmlFieldAttributes attrs = typeMapper.map(node);

        XmlElement element = new XmlElement("field");
        for (var entry : attrs.getAttributes().entrySet()) {
            element.attribute(entry.getKey(), entry.getValue());
        }

        return element;
    }

    /**
     * 包装 beans 根元素
     */
    private XmlElement wrapWithBeans(XmlElement content, String namespace) {
        XmlElement beans = new XmlElement("beans:beans")
            .attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            .attribute("xmlns", namespace)
            .attribute("xmlns:beans", "http://www.springframework.org/schema/beans");

        beans.addChild(content);
        return beans;
    }

    private String buildForType(String className) {
        String groupId = config.getXml().getProject().getGroupId();
        String artifactId = config.getXml().getProject().getArtifactId();
        return groupId + "." + artifactId + "." + className;
    }
}
```

## Acceptance Criteria

1. [ ] XML 声明正确
2. [ ] 命名空间正确配置
3. [ ] 字段顺序与 JSON Tree 一致
4. [ ] 嵌套结构正确生成
5. [ ] 属性正确转义
6. [ ] 格式化输出（2 空格缩进）
7. [ ] 空元素使用自闭合标签

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 简单字段生成 | DataField 正确 |
| 单元测试 | 嵌套对象生成 | CompositeField 正确 |
| 单元测试 | 数组生成 | RepeatingField 正确 |
| 单元测试 | 特殊字符转义 | 正确转义 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 深层嵌套 | 格式混乱 | 递归测试 |
| 空字段列表 | 空元素 | 验证非空 |
