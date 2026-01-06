# T-105 对象/数组检测

## Goal

实现对象定义和数组检测逻辑，识别 groupId/occurrenceCount 特殊字段，判断字段是对象还是数组。

## In Scope / Out of Scope

**In Scope**:
- ObjectArrayDetector 类实现
- 对象定义识别（`FieldName:ClassName` 格式）
- groupId 字段识别和值提取
- occurrenceCount 字段识别和值提取
- 数组 vs 对象判断（基于 occurrenceCount）
- isTransitory 标记

**Out of Scope**:
- 命名规范化（T-106）
- XML/Java 生成

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.5 节（对象与数组检测）
- T-104 Seg lvl 嵌套算法

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── ObjectArrayDetector.java
├── ObjectDefinition.java
├── ArrayInfo.java
└── OccurrenceCountParser.java

```

## Dependencies

- T-104 Seg lvl 嵌套算法
- T-004 数据模型定义

## Implementation Notes

### ObjectDefinition 类

```java
package com.rtm.mq.tool.parser;

/**
 * 对象定义信息
 */
public class ObjectDefinition {
    private final String fieldName;
    private final String className;

    public ObjectDefinition(String fieldName, String className) {
        this.fieldName = fieldName;
        this.className = className;
    }

    public String getFieldName() { return fieldName; }
    public String getClassName() { return className; }
}
```

### ArrayInfo 类

```java
package com.rtm.mq.tool.parser;

/**
 * 数组信息
 */
public class ArrayInfo {
    private final int min;
    private final int max;
    private final boolean isArray;
    private final boolean isOptional;

    public ArrayInfo(int min, int max, boolean isArray, boolean isOptional) {
        this.min = min;
        this.max = max;
        this.isArray = isArray;
        this.isOptional = isOptional;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }
    public boolean isArray() { return isArray; }
    public boolean isOptional() { return isOptional; }
    public int getFixedCount() { return max; }
}
```

### OccurrenceCountParser 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * occurrenceCount 解析器
 * 解析格式如 "0..9", "1..1", "1..2"
 */
public class OccurrenceCountParser {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.\\.(\\d+)");

    /**
     * 解析 occurrenceCount 值
     *
     * @param occurrenceCount 如 "0..9", "1..1"
     * @return 数组信息
     */
    public ArrayInfo parse(String occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(occurrenceCount.trim());
        if (!matcher.matches()) {
            throw new ParseException("Invalid occurrenceCount format: " + occurrenceCount);
        }

        int min = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));

        // max > 1 表示数组
        boolean isArray = (max > 1);
        // min == 0 表示可选
        boolean isOptional = (min == 0);

        return new ArrayInfo(min, max, isArray, isOptional);
    }

    /**
     * 计算 fixedCount（用于 XML RepeatingField）
     */
    public int calculateFixedCount(String occurrenceCount) {
        ArrayInfo info = parse(occurrenceCount);
        if (info == null) {
            return 1;
        }
        return info.getMax();
    }
}
```

### ObjectArrayDetector 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

/**
 * 对象/数组检测器
 * 识别对象定义、groupId、occurrenceCount
 */
public class ObjectArrayDetector {

    private final OccurrenceCountParser occurrenceParser = new OccurrenceCountParser();
    private final Map<String, Integer> columnMap;

    public ObjectArrayDetector(Map<String, Integer> columnMap) {
        this.columnMap = columnMap;
    }

    /**
     * 检测并增强 FieldNode
     * 设置 isObject, isArray, groupId, occurrenceCount, isTransitory 等属性
     *
     * @param node 基础 FieldNode
     * @param row Excel 行
     * @return 增强后的 FieldNode
     */
    public FieldNode detect(FieldNode node, Row row) {
        String fieldName = node.getOriginalName();
        String description = getCellValue(row, ColumnNames.DESCRIPTION);
        String length = getCellValue(row, ColumnNames.LENGTH);
        String dataType = getCellValue(row, ColumnNames.MESSAGING_DATATYPE);

        // 检测 groupId
        if (isGroupIdField(fieldName)) {
            return enhanceAsGroupId(node, description);
        }

        // 检测 occurrenceCount
        if (isOccurrenceCountField(fieldName)) {
            return enhanceAsOccurrenceCount(node, description);
        }

        // 检测对象定义
        if (isObjectDefinition(fieldName, length, dataType)) {
            return enhanceAsObjectOrArray(node, fieldName);
        }

        // 普通字段
        return node;
    }

    /**
     * 判断是否为 groupId 字段
     */
    public boolean isGroupIdField(String fieldName) {
        return "groupid".equalsIgnoreCase(fieldName);
    }

    /**
     * 判断是否为 occurrenceCount 字段
     * 注意：Excel 中拼写为 "occurenceCount"（少一个 r）
     */
    public boolean isOccurrenceCountField(String fieldName) {
        return "occurenceCount".equalsIgnoreCase(fieldName) ||
               "occurrenceCount".equalsIgnoreCase(fieldName);
    }

    /**
     * 判断是否为对象定义
     * 规则：包含冒号，且 Length 和 Datatype 为空
     */
    public boolean isObjectDefinition(String fieldName, String length, String dataType) {
        return fieldName != null &&
               fieldName.contains(":") &&
               (length == null || length.trim().isEmpty()) &&
               (dataType == null || dataType.trim().isEmpty());
    }

    /**
     * 解析对象定义
     */
    public ObjectDefinition parseObjectDefinition(String fieldName) {
        if (fieldName == null || !fieldName.contains(":")) {
            throw new ParseException("Invalid object definition format: " + fieldName);
        }

        String[] parts = fieldName.split(":", 2);
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            throw new ParseException("Invalid object definition format: " + fieldName +
                ". Expected format: 'fieldName:ClassName'");
        }

        return new ObjectDefinition(parts[0].trim(), parts[1].trim());
    }

    /**
     * 增强为 groupId 字段
     */
    private FieldNode enhanceAsGroupId(FieldNode node, String description) {
        // description 包含 groupId 的值
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(node.getOriginalName().toLowerCase())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .groupId(description != null ? description.trim() : null)
            .isTransitory(true)
            .source(node.getSource())
            .build();
    }

    /**
     * 增强为 occurrenceCount 字段
     */
    private FieldNode enhanceAsOccurrenceCount(FieldNode node, String description) {
        // description 包含 occurrenceCount 的值
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(node.getOriginalName().toLowerCase())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .occurrenceCount(description != null ? description.trim() : null)
            .isTransitory(true)
            .source(node.getSource())
            .build();
    }

    /**
     * 增强为对象或数组
     */
    private FieldNode enhanceAsObjectOrArray(FieldNode node, String fieldName) {
        ObjectDefinition objDef = parseObjectDefinition(fieldName);

        return FieldNode.builder()
            .originalName(fieldName)
            .camelCaseName(null)  // T-106 处理
            .className(objDef.getClassName())
            .segLevel(node.getSegLevel())
            .isObject(true)  // 默认为对象，后续根据 occurrenceCount 调整
            .isArray(false)
            .children(node.getChildren())
            .source(node.getSource())
            .build();
    }

    /**
     * 根据 occurrenceCount 更新容器类型
     * 在解析完子节点后调用
     */
    public void updateContainerType(FieldNode containerNode) {
        // 查找子节点中的 occurrenceCount
        for (FieldNode child : containerNode.getChildren()) {
            if (isOccurrenceCountField(child.getOriginalName())) {
                String occCount = child.getOccurrenceCount();
                if (occCount != null) {
                    ArrayInfo info = occurrenceParser.parse(occCount);
                    if (info != null && info.isArray()) {
                        // 更新为数组类型
                        // 注意：FieldNode 使用 Builder，这里需要重建或使用 setter
                        // 假设有 setter 方法
                        // containerNode.setIsArray(true);
                        // containerNode.setIsObject(false);
                        // containerNode.setOccurrenceCount(occCount);
                    }
                }
                break;
            }
        }
    }

    private String getCellValue(Row row, String columnName) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) return null;

        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            default: return null;
        }
    }
}
```

## Acceptance Criteria

1. [ ] 正确识别 `FieldName:ClassName` 格式的对象定义
2. [ ] 正确识别 groupId 字段并提取值（从 Description 列）
3. [ ] 正确识别 occurrenceCount 字段并提取值（从 Description 列）
4. [ ] 正确解析 occurrenceCount 格式（如 "0..9", "1..1"）
5. [ ] 根据 occurrenceCount 判断数组 vs 对象
6. [ ] groupId/occurrenceCount 字段标记为 isTransitory
7. [ ] 单元测试覆盖各种场景
