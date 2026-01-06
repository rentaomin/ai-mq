# T-207 OpenAPI Schema 映射

## Goal

实现 OpenAPI 类型映射器（OpenApiTypeMapper），负责将中间 JSON Tree 中的 `dataType` 属性映射为符合 OpenAPI 3.x 规范的 Schema 定义。此组件为 OpenAPI 主文件生成器（T-208）提供类型映射支持。

## In Scope / Out of Scope

**In Scope**:
- OpenApiTypeMapper 类实现
- Excel dataType 到 OpenAPI Schema 的映射规则
- 数组类型 Schema 生成（type: array, items）
- 对象引用生成（$ref）
- required 列表生成（基于 optionality）
- maxLength 约束生成（基于 length）
- default 值生成（基于 defaultValue）
- format 属性映射（如 decimal）
- transitory 字段过滤（排除 groupId、occurrenceCount）
- 单元测试

**Out of Scope**:
- OpenAPI 主文件生成（T-208）
- Schema 拆分与 $ref 路径管理（T-209）
- Java 类型映射（T-203）
- XML 类型映射（T-109）
- OpenAPI 验证（T-303）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.3.5 节（Schema 生成映射规则）
- T-108 JSON Tree 序列化（MessageModel、FieldNode 数据结构）
- T-004 数据模型定义（FieldNode 各属性）
- T-003 配置加载器（Config.java）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/openapi/
└── OpenApiTypeMapper.java

```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-108 | DeterministicJsonWriter、JSON Tree 结构定义 |
| T-004 | FieldNode、MessageModel 数据模型 |
| T-003 | Config 配置类 |

## Implementation Notes

### OpenAPI Schema 映射规则表

根据架构文档 6.3.5：

| FieldNode 属性 | OpenAPI Schema | 说明 |
|---------------|----------------|------|
| `dataType == "String"` | `type: string` | 字符串 |
| `dataType == "AN"` | `type: string` | 字母数字 |
| `dataType == "Number"` | `type: string` | 保持字符串 |
| `dataType == "N"` | `type: string` | 数字字符串 |
| `dataType == "Unsigned Integer"` | `type: string` | 无符号整数 |
| `dataType == "Amount"` | `type: string`, `format: decimal` | 金额 |
| `dataType == "Currency"` | `type: string`, `format: decimal` | 货币 |
| `dataType == "Date"` | `type: string`, `format: date` | 日期 |
| `isArray == true` | `type: array`, `items: {$ref}` | 数组 |
| `isObject == true` | `$ref: './ClassName.yaml'` | 对象引用 |
| `optionality == "M"` | 添加到 `required` 列表 | 必填字段 |
| `length != null` | `maxLength: {length}` | 最大长度 |
| `defaultValue != null` | `default: "{value}"` | 默认值 |

### OpenApiTypeMapper 类

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;

import java.util.*;

/**
 * OpenAPI Schema 类型映射器
 * 将 FieldNode 映射为 OpenAPI 3.x Schema 定义
 */
public class OpenApiTypeMapper {

    private final Config config;

    public OpenApiTypeMapper(Config config) {
        this.config = config;
    }

    /**
     * 将 FieldNode 映射为 OpenAPI Schema Map
     * 返回的 Map 可直接序列化为 YAML
     *
     * @param field 字段节点
     * @return Schema 定义（Map 结构）
     */
    public Map<String, Object> mapToSchema(FieldNode field) {
        Map<String, Object> schema = new LinkedHashMap<>();

        // 数组类型
        if (field.isArray()) {
            schema.put("type", "array");
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("$ref", "./" + field.getClassName() + ".yaml");
            schema.put("items", items);

            // 数组 maxItems（基于 occurrenceCount 解析，如 0..9）
            Integer maxItems = parseMaxItems(field.getOccurrenceCount());
            if (maxItems != null) {
                schema.put("maxItems", maxItems);
            }
            return schema;
        }

        // 对象类型（使用 $ref 引用）
        if (field.isObject()) {
            schema.put("$ref", "./" + field.getClassName() + ".yaml");
            return schema;
        }

        // 基本类型映射
        String dataType = field.getDataType();
        mapPrimitiveType(dataType, schema);

        // maxLength
        if (field.getLength() != null && field.getLength() > 0) {
            schema.put("maxLength", field.getLength());
        }

        // default
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            schema.put("default", field.getDefaultValue());
        }

        return schema;
    }

    /**
     * 映射基本类型到 OpenAPI type 和 format
     */
    private void mapPrimitiveType(String dataType, Map<String, Object> schema) {
        if (dataType == null || dataType.isEmpty()) {
            schema.put("type", "string");
            return;
        }

        switch (dataType.toLowerCase()) {
            case "string":
            case "an":
            case "a":
            case "number":
            case "n":
            case "unsigned integer":
                schema.put("type", "string");
                break;

            case "amount":
            case "currency":
                schema.put("type", "string");
                schema.put("format", "decimal");
                break;

            case "date":
                schema.put("type", "string");
                schema.put("format", "date");
                break;

            default:
                schema.put("type", "string");
                break;
        }
    }

    /**
     * 过滤 transitory 字段（groupId、occurrenceCount）
     * 这些字段不应出现在 OpenAPI Schema 中
     */
    public List<FieldNode> filterTransitoryFields(List<FieldNode> fields) {
        List<FieldNode> filtered = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory()) {
                filtered.add(field);
            }
        }
        return filtered;
    }

    /**
     * 收集必填字段列表
     * 基于 optionality == "M"
     */
    public List<String> collectRequiredFields(List<FieldNode> fields) {
        List<String> required = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory() && "M".equals(field.getOptionality())) {
                required.add(field.getCamelCaseName());
            }
        }
        return required;
    }

    /**
     * 生成完整的 Object Schema（包含 properties 和 required）
     */
    public Map<String, Object> generateObjectSchema(List<FieldNode> fields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        // 过滤 transitory 字段
        List<FieldNode> filteredFields = filterTransitoryFields(fields);

        // required 列表
        List<String> required = collectRequiredFields(filteredFields);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        // properties
        Map<String, Object> properties = new LinkedHashMap<>();
        for (FieldNode field : filteredFields) {
            properties.put(field.getCamelCaseName(), mapToSchema(field));
        }
        schema.put("properties", properties);

        return schema;
    }

    /**
     * 解析 occurrenceCount 获取 maxItems
     * 格式如: "0..9", "1..N", "0..N"
     *
     * @return maxItems 值，如果是 N 则返回 null
     */
    private Integer parseMaxItems(String occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount.isEmpty()) {
            return null;
        }

        // 解析 "0..9" 格式
        if (occurrenceCount.contains("..")) {
            String[] parts = occurrenceCount.split("\\.\\.");
            if (parts.length == 2) {
                String max = parts[1].trim();
                if ("N".equalsIgnoreCase(max) || "*".equals(max)) {
                    return null; // 无限制
                }
                try {
                    return Integer.parseInt(max);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * 生成 $ref 路径
     * 用于嵌套对象和数组项的引用
     *
     * @param className 类名
     * @param schemaDir Schema 目录（如 "request", "response", "common"）
     * @return 相对 $ref 路径
     */
    public String generateRefPath(String className, String schemaDir) {
        return "./" + className + ".yaml";
    }

    /**
     * 判断字段是否应包含在 OpenAPI Schema 中
     */
    public boolean shouldIncludeInSchema(FieldNode field) {
        return !field.isTransitory();
    }
}
```

### $ref 引用策略

根据架构文档 6.3.3，使用相对路径引用：
- 同目录引用: `$ref: './ClassName.yaml'`
- 跨目录引用由 T-209 处理

### 字段顺序保证

使用 `LinkedHashMap` 保证字段顺序与 JSON Tree 一致。

### 必填字段处理

```yaml
type: object
required:
  - createApp       # optionality == "M"
  - domicleBranche  # optionality == "M"
properties:
  createApp:
    $ref: './CreateApplication.yaml'
  productDel:       # optionality == "O", 不在 required 中
    $ref: './ProductDetails.yaml'
```

### 数组 maxItems 处理

```yaml
# occurrenceCount == "0..9"
cbaCardArr:
  type: array
  items:
    $ref: './CBACardArray.yaml'
  maxItems: 9
```

## Acceptance Criteria

1. [ ] OpenApiTypeMapper 类编译通过
2. [ ] 所有基本类型正确映射（string, format）
3. [ ] 数组类型正确生成（type: array, items.$ref）
4. [ ] 对象类型正确生成（$ref）
5. [ ] required 列表正确收集（仅 optionality="M"）
6. [ ] maxLength 正确生成
7. [ ] default 值正确生成
8. [ ] transitory 字段被正确过滤（不出现在 Schema 中）
9. [ ] 字段顺序与 JSON Tree 一致
10. [ ] maxItems 正确解析（从 occurrenceCount）

---

**文档结束**
