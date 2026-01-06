# T-109 XML 字段类型映射

## Goal

实现 XML Bean 的字段类型映射规则，将中间 JSON Tree 的字段类型映射为 XML 元素类型（DataField、CompositeField、RepeatingField）。

## In Scope / Out of Scope

**In Scope**:
- XmlTypeMapper 类实现
- DataField 属性映射
- CompositeField 属性映射
- RepeatingField 属性映射
- converter 类型映射

**Out of Scope**:
- XML 模板渲染（T-110）
- XML 文件生成（T-111）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.1.4 节（字段类型映射规则）
- T-108 JSON Tree 序列化

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/xml/
├── XmlTypeMapper.java
├── XmlFieldType.java
├── XmlFieldAttributes.java
└── ConverterMapper.java
```

## Dependencies

- T-108 JSON Tree 序列化
- T-004 数据模型定义

## Implementation Notes

### 映射规则表

根据架构文档 6.1.4：

| 条件 | XML type | 关键属性 |
|------|---------|---------|
| `isTransitory && groupId != null` | DataField | transitory=true, defaultValue={groupId} |
| `isTransitory && occurrenceCount != null` | DataField | transitory=true, converter=counterFieldConverter |
| `isObject && !isArray` | CompositeField | forType={package}.{className} |
| `isArray` | RepeatingField | fixedCount={max}, forType={package}.{className} |
| `dataType == "String"` | DataField | converter=stringFieldConverter |
| `dataType == "Unsigned Integer"` | DataField | pad="0", alignRight=true |
| `dataType == "Amount"` | DataField | converter=OHcurrencyamountFieldConverter |

### XmlFieldType 枚举

```java
package com.rtm.mq.tool.generator.xml;

public enum XmlFieldType {
    DATA_FIELD("DataField"),
    COMPOSITE_FIELD("CompositeField"),
    REPEATING_FIELD("RepeatingField");

    private final String value;

    XmlFieldType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### XmlFieldAttributes 类

```java
package com.rtm.mq.tool.generator.xml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XML 字段属性集合
 * 使用 LinkedHashMap 保证属性顺序
 */
public class XmlFieldAttributes {
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private XmlFieldType type;

    public XmlFieldAttributes(XmlFieldType type) {
        this.type = type;
        attributes.put("type", type.getValue());
    }

    public XmlFieldAttributes name(String name) {
        if (name != null) attributes.put("name", name);
        return this;
    }

    public XmlFieldAttributes length(Integer length) {
        if (length != null) attributes.put("length", String.valueOf(length));
        return this;
    }

    public XmlFieldAttributes fixedLength(boolean value) {
        if (value) attributes.put("fixedLength", "true");
        return this;
    }

    public XmlFieldAttributes transitory(boolean value) {
        if (value) attributes.put("transitory", "true");
        return this;
    }

    public XmlFieldAttributes defaultValue(String value) {
        if (value != null) attributes.put("defaultValue", value);
        return this;
    }

    public XmlFieldAttributes pad(String value) {
        if (value != null) attributes.put("pad", value);
        return this;
    }

    public XmlFieldAttributes alignRight(boolean value) {
        if (value) attributes.put("alignRight", "true");
        return this;
    }

    public XmlFieldAttributes nullPad(String value) {
        if (value != null) attributes.put("nullPad", value);
        return this;
    }

    public XmlFieldAttributes converter(String value) {
        if (value != null) attributes.put("converter", value);
        return this;
    }

    public XmlFieldAttributes forType(String value) {
        if (value != null) attributes.put("forType", value);
        return this;
    }

    public XmlFieldAttributes fixedCount(int count) {
        attributes.put("fixedCount", String.valueOf(count));
        return this;
    }

    public XmlFieldAttributes floatingNumberLength(int length) {
        attributes.put("floatingNumberLength", String.valueOf(length));
        return this;
    }

    public XmlFieldType getType() {
        return type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
```

### ConverterMapper 类

```java
package com.rtm.mq.tool.generator.xml;

/**
 * Converter 类型映射器
 */
public class ConverterMapper {

    public static final String STRING_CONVERTER = "stringFieldConverter";
    public static final String COUNTER_CONVERTER = "counterFieldConverter";
    public static final String CURRENCY_CONVERTER = "OHcurrencyamountFieldConverter";
    public static final String NLS_CONVERTER = "nlsStringFieldConverter";
    public static final String UNSIGNED_LONG_CONVERTER = "OHunsignedlongFieldConverter";
    public static final String UNSIGNED_INT_CONVERTER = "OHunsignedintFieldConverter";

    /**
     * 根据数据类型获取 converter
     */
    public String getConverter(String dataType) {
        if (dataType == null) return STRING_CONVERTER;

        String normalized = dataType.trim().toLowerCase();

        switch (normalized) {
            case "string":
            case "an":
                return STRING_CONVERTER;

            case "number":
            case "n":
            case "unsigned integer":
                return STRING_CONVERTER;  // 保持字符串

            case "amount":
            case "currency":
                return CURRENCY_CONVERTER;

            default:
                return STRING_CONVERTER;
        }
    }

    /**
     * 获取 forType（对于金额类型）
     */
    public String getForType(String dataType) {
        if (dataType == null) return null;

        String normalized = dataType.trim().toLowerCase();

        if ("amount".equals(normalized) || "currency".equals(normalized)) {
            return "java.math.BigDecimal";
        }

        return null;
    }
}
```

### XmlTypeMapper 类

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.parser.OccurrenceCountParser;

/**
 * XML 字段类型映射器
 */
public class XmlTypeMapper {

    private final Config config;
    private final ConverterMapper converterMapper;
    private final OccurrenceCountParser occurrenceParser;

    public XmlTypeMapper(Config config) {
        this.config = config;
        this.converterMapper = new ConverterMapper();
        this.occurrenceParser = new OccurrenceCountParser();
    }

    /**
     * 映射 FieldNode 到 XML 属性
     */
    public XmlFieldAttributes map(FieldNode node) {
        // 1. transitory groupId 字段
        if (node.isTransitory() && node.getGroupId() != null) {
            return mapGroupIdField(node);
        }

        // 2. transitory occurrenceCount 字段
        if (node.isTransitory() && node.getOccurrenceCount() != null) {
            return mapOccurrenceCountField(node);
        }

        // 3. 数组字段
        if (node.isArray()) {
            return mapRepeatingField(node);
        }

        // 4. 对象字段
        if (node.isObject()) {
            return mapCompositeField(node);
        }

        // 5. 普通数据字段
        return mapDataField(node);
    }

    private XmlFieldAttributes mapGroupIdField(FieldNode node) {
        return new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .length(node.getLength() != null ? node.getLength() : 10)
            .fixedLength(true)
            .transitory(true)
            .defaultValue(node.getGroupId())
            .converter(ConverterMapper.STRING_CONVERTER);
    }

    private XmlFieldAttributes mapOccurrenceCountField(FieldNode node) {
        int count = occurrenceParser.calculateFixedCount(node.getOccurrenceCount());
        return new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .length(node.getLength() != null ? node.getLength() : 4)
            .fixedLength(true)
            .transitory(true)
            .defaultValue(String.valueOf(count))
            .pad("0")
            .alignRight(true)
            .converter(ConverterMapper.COUNTER_CONVERTER);
    }

    private XmlFieldAttributes mapCompositeField(FieldNode node) {
        return new XmlFieldAttributes(XmlFieldType.COMPOSITE_FIELD)
            .name(node.getCamelCaseName())
            .forType(buildForType(node.getClassName()));
    }

    private XmlFieldAttributes mapRepeatingField(FieldNode node) {
        int fixedCount = occurrenceParser.calculateFixedCount(node.getOccurrenceCount());
        return new XmlFieldAttributes(XmlFieldType.REPEATING_FIELD)
            .name(node.getCamelCaseName())
            .fixedCount(fixedCount)
            .forType(buildForType(node.getClassName()));
    }

    private XmlFieldAttributes mapDataField(FieldNode node) {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name(node.getCamelCaseName())
            .length(node.getLength());

        String dataType = node.getDataType();

        // 默认值
        if (node.getDefaultValue() != null) {
            attrs.defaultValue(node.getDefaultValue());
        }

        // 根据数据类型设置属性
        if (isNumericType(dataType)) {
            attrs.pad("0").alignRight(true);
        } else {
            attrs.nullPad(" ");
        }

        // converter
        attrs.converter(converterMapper.getConverter(dataType));

        // forType（金额类型）
        String forType = converterMapper.getForType(dataType);
        if (forType != null) {
            attrs.forType(forType);
        }

        return attrs;
    }

    private String buildForType(String className) {
        String groupId = config.getXml().getProject().getGroupId();
        String artifactId = config.getXml().getProject().getArtifactId();
        return groupId + "." + artifactId + "." + className;
    }

    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String normalized = dataType.trim().toLowerCase();
        return "number".equals(normalized) ||
               "n".equals(normalized) ||
               "unsigned integer".equals(normalized);
    }
}
```

## Acceptance Criteria

1. [ ] groupId 字段正确映射为 transitory DataField
2. [ ] occurrenceCount 字段正确映射为 transitory DataField
3. [ ] 对象正确映射为 CompositeField
4. [ ] 数组正确映射为 RepeatingField
5. [ ] 字符串字段使用 stringFieldConverter
6. [ ] 金额字段使用 OHcurrencyamountFieldConverter
7. [ ] forType 格式正确
8. [ ] 属性顺序固定