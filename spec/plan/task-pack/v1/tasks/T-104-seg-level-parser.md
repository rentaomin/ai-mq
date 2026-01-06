# T-104 Seg lvl 嵌套算法

## Goal

实现基于 Seg lvl（Segment Level）的层级嵌套解析算法，使用栈结构维护父子关系，支持无限层级嵌套。

## In Scope / Out of Scope

**In Scope**:
- SegLevelParser 类实现
- 栈结构维护层级关系
- 嵌套深度验证
- Seg lvl 跳跃检测

**Out of Scope**:
- 对象/数组检测（T-105）
- 字段命名规范化（T-106）
- 行数据提取

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.4 节（Seg lvl 嵌套算法）
- T-103 元数据提取器
- T-004 数据模型定义

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── SegLevelParser.java
└── NestingDepthValidator.java

src/test/java/com/rtm/mq/tool/parser/
├── SegLevelParserTest.java
└── NestingDepthValidatorTest.java
```

## Dependencies

- T-103 元数据提取器
- T-004 数据模型定义（FieldNode 类）
- T-005 错误处理框架

## Implementation Notes

### 算法描述

根据架构文档 5.4.1：

1. **维护栈结构**: 记录每个层级的当前父节点
2. **逐行处理**: 从 Row 9 开始顺序处理
3. **层级判断**:
   - 如果 `segLevel == 当前栈深度 + 1`: 添加到栈顶节点的 children
   - 如果 `segLevel <= 当前栈深度`: 弹出栈直到找到正确的父节点
4. **对象识别**: 如果是对象定义行，将该对象压入栈

### NestingDepthValidator 类

```java
package com.rtm.mq.tool.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 嵌套深度验证器
 * 默认最大深度 50 层，超过时记录警告但不中断
 */
public class NestingDepthValidator {
    private static final Logger logger = LoggerFactory.getLogger(NestingDepthValidator.class);
    private static final int DEFAULT_MAX_DEPTH = 50;

    private final int maxDepth;

    public NestingDepthValidator() {
        this(DEFAULT_MAX_DEPTH);
    }

    public NestingDepthValidator(int maxDepth) {
        this.maxDepth = maxDepth > 0 ? maxDepth : DEFAULT_MAX_DEPTH;
    }

    /**
     * 验证嵌套深度
     *
     * @param currentDepth 当前深度
     * @param rowIndex 行号
     * @param fieldName 字段名
     * @return true 如果在允许范围内，false 如果超出但继续处理
     */
    public boolean validateDepth(int currentDepth, int rowIndex, String fieldName) {
        if (currentDepth > maxDepth) {
            logger.warn("Nesting depth {} exceeds recommended maximum {} at row {} (field: {}). " +
                "This may impact performance and readability.",
                currentDepth, maxDepth, rowIndex, fieldName);
            return false;
        }
        return true;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
```

### SegLevelParser 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.*;

/**
 * Seg lvl 嵌套解析器
 * 使用栈结构维护层级关系
 */
public class SegLevelParser {

    private static final int DATA_START_ROW = 8;  // Row 9 (0-indexed = 8)

    private final NestingDepthValidator depthValidator;
    private final Map<String, Integer> columnMap;
    private final String sheetName;

    public SegLevelParser(Map<String, Integer> columnMap, String sheetName) {
        this(columnMap, sheetName, new NestingDepthValidator());
    }

    public SegLevelParser(Map<String, Integer> columnMap, String sheetName,
                          NestingDepthValidator depthValidator) {
        this.columnMap = columnMap;
        this.sheetName = sheetName;
        this.depthValidator = depthValidator;
    }

    /**
     * 解析 Sheet 中的字段
     *
     * @param sheet Excel Sheet
     * @return 顶层字段列表（保持顺序）
     */
    public List<FieldNode> parseFields(Sheet sheet) {
        List<FieldNode> rootFields = new ArrayList<>();
        Deque<FieldNode> stack = new ArrayDeque<>();

        int previousLevel = 0;

        for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            // 创建基础 FieldNode
            FieldNode node = createBasicFieldNode(row, i + 1);  // 行号 1-based
            if (node == null) {
                continue;
            }

            int segLevel = node.getSegLevel();

            // 验证 Seg lvl
            validateSegLevel(segLevel, previousLevel, i + 1, node.getOriginalName());

            // 验证嵌套深度
            depthValidator.validateDepth(segLevel, i + 1, node.getOriginalName());

            // 调整栈深度到正确的父级
            while (!stack.isEmpty() && stack.peek().getSegLevel() >= segLevel) {
                stack.pop();
            }

            // 添加到正确位置
            if (stack.isEmpty()) {
                // 顶层字段
                rootFields.add(node);
            } else {
                // 添加到父节点的 children
                stack.peek().getChildren().add(node);
            }

            // 如果是对象或数组定义，压入栈（等待后续处理确定 isObject/isArray）
            // 这里先压入所有节点，由 T-105 来确定类型后决定是否保留
            if (isContainerCandidate(node)) {
                stack.push(node);
            }

            previousLevel = segLevel;
        }

        return rootFields;
    }

    /**
     * 判断是否可能是容器（对象/数组）候选
     * 暂时根据 FieldName 包含 ":" 判断
     */
    private boolean isContainerCandidate(FieldNode node) {
        String fieldName = node.getOriginalName();
        return fieldName != null && fieldName.contains(":");
    }

    /**
     * 验证 Seg lvl 有效性
     */
    private void validateSegLevel(int segLevel, int previousLevel, int rowIndex, String fieldName) {
        if (segLevel <= 0) {
            throw new ParseException("Invalid Seg lvl '" + segLevel + "'. Must be a positive integer.")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }

        // 允许从任意层级跳回到更低层级
        // 但不允许跳跃（如从 1 直接到 3）
        if (segLevel > previousLevel + 1 && previousLevel > 0) {
            throw new ParseException(
                "Seg lvl jump from " + previousLevel + " to " + segLevel + ". Missing intermediate level.")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }
    }

    /**
     * 创建基础 FieldNode
     * 仅填充从 Excel 直接读取的字段
     */
    private FieldNode createBasicFieldNode(Row row, int rowIndex) {
        String fieldName = getCellValue(row, ColumnNames.FIELD_NAME);
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return null;  // 跳过空行
        }

        int segLevel = parseSegLevel(getCellValue(row, ColumnNames.SEG_LVL), rowIndex, fieldName);
        String description = getCellValue(row, ColumnNames.DESCRIPTION);
        String length = getCellValue(row, ColumnNames.LENGTH);
        String dataType = getCellValue(row, ColumnNames.MESSAGING_DATATYPE);
        String optionality = getCellValue(row, ColumnNames.OPTIONALITY);

        // 创建 SourceMetadata
        SourceMetadata source = new SourceMetadata();
        source.setSheetName(sheetName);
        source.setRowIndex(rowIndex);

        return FieldNode.builder()
            .originalName(fieldName.trim())
            .segLevel(segLevel)
            .length(parseLength(length))
            .dataType(dataType != null ? dataType.trim() : null)
            .optionality(optionality != null ? optionality.trim() : null)
            // description 在 T-105 中用于提取 groupId/occurrenceCount
            .source(source)
            .build();
    }

    /**
     * 解析 Seg lvl 值
     */
    private int parseSegLevel(String value, int rowIndex, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParseException("Seg lvl is empty")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid Seg lvl format '" + value + "'")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }
    }

    /**
     * 解析 Length 值
     */
    private Integer parseLength(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;  // 对象定义时 Length 为空
        }
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Row row, String columnName) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            return null;
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((int) num);
                }
                return String.valueOf(num);
            default:
                return null;
        }
    }

    /**
     * 判断是否为空行
     */
    private boolean isEmptyRow(Row row) {
        String fieldName = getCellValue(row, ColumnNames.FIELD_NAME);
        return fieldName == null || fieldName.trim().isEmpty();
    }
}
```

## Acceptance Criteria

1. [ ] 正确解析层级关系（父子嵌套）
2. [ ] Seg lvl = 1 的字段成为顶层字段
3. [ ] Seg lvl > 1 的字段正确添加到父节点 children
4. [ ] Seg lvl 跳跃检测（如 1 → 3）
5. [ ] 嵌套深度超限时记录警告
6. [ ] 保持字段顺序（按 Excel 行顺序）
7. [ ] 空行正确跳过
8. [ ] 单元测试覆盖各种嵌套场景
