# T-102 Sheet 发现与验证

## Goal

实现 Excel Sheet 发现与列验证逻辑，确保 Excel 文件包含必需的 Sheet（Request, Response）和必需的列。

## In Scope / Out of Scope

**In Scope**:
- SheetDiscovery 类实现
- ColumnValidator 类实现
- 必需 Sheet 验证
- 必需列验证
- 列索引映射

**Out of Scope**:
- 数据行解析（T-104）
- 元数据提取（T-103）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.2 节（Sheet 发现与验证）
- T-101 列名规范化器
- T-005 错误处理框架

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── SheetDiscovery.java
├── SheetSet.java
└── ColumnValidator.java

src/test/java/com/rtm/mq/tool/parser/
├── SheetDiscoveryTest.java
└── ColumnValidatorTest.java
```

## Dependencies

- T-101 列名规范化器
- T-005 错误处理框架

## Implementation Notes

### SheetSet 类

```java
package com.rtm.mq.tool.parser;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * Excel Sheet 集合
 */
public class SheetSet {
    private Sheet request;
    private Sheet response;
    private Sheet sharedHeader;

    public Sheet getRequest() { return request; }
    public void setRequest(Sheet request) { this.request = request; }

    public Sheet getResponse() { return response; }
    public void setResponse(Sheet response) { this.response = response; }

    public Sheet getSharedHeader() { return sharedHeader; }
    public void setSharedHeader(Sheet sharedHeader) { this.sharedHeader = sharedHeader; }

    public boolean hasSharedHeader() {
        return sharedHeader != null;
    }
}
```

### SheetDiscovery 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel Sheet 发现器
 */
public class SheetDiscovery {

    private static final String REQUEST_SHEET = "Request";
    private static final String RESPONSE_SHEET = "Response";
    private static final String SHARED_HEADER_SHEET = "Shared Header";

    /**
     * 发现并验证 Sheet
     *
     * @param workbook Excel 工作簿
     * @return Sheet 集合
     * @throws ParseException 如果必需 Sheet 不存在
     */
    public SheetSet discoverSheets(Workbook workbook) {
        SheetSet sheets = new SheetSet();

        // 必需 sheets
        sheets.setRequest(workbook.getSheet(REQUEST_SHEET));
        if (sheets.getRequest() == null) {
            throw new ParseException("Required sheet '" + REQUEST_SHEET + "' not found");
        }

        sheets.setResponse(workbook.getSheet(RESPONSE_SHEET));
        if (sheets.getResponse() == null) {
            throw new ParseException("Required sheet '" + RESPONSE_SHEET + "' not found");
        }

        // 可选 sheet
        sheets.setSharedHeader(workbook.getSheet(SHARED_HEADER_SHEET));

        return sheets;
    }

    /**
     * 从独立文件发现 Shared Header Sheet
     *
     * @param workbook Shared Header 工作簿
     * @return Shared Header Sheet，如果不存在返回 null
     */
    public Sheet discoverSharedHeader(Workbook workbook) {
        return workbook.getSheet(SHARED_HEADER_SHEET);
    }
}
```

### ColumnValidator 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.*;

/**
 * 列验证器
 * 验证必需列存在并建立列索引映射
 */
public class ColumnValidator {

    /** 必需的列名（规范化后） */
    private static final List<String> REQUIRED_COLUMNS = Arrays.asList(
        "Seg lvl",
        "Field Name",
        "Description",
        "Length",
        "Messaging Datatype"
    );

    /** 可选的列名 */
    private static final List<String> OPTIONAL_COLUMNS = Arrays.asList(
        "Opt(O/M)",
        "Null (Y/N)",
        "NLS (Y/N)",
        "Sample Value(s)",
        "Remarks",
        "GMR Physical Name",
        "Test Value"
    );

    /**
     * 验证并映射列
     *
     * @param headerRow Header 行（通常是 Row 8，0-indexed 为 7）
     * @param sheetName Sheet 名称（用于错误消息）
     * @return 列名到索引的映射
     * @throws ParseException 如果必需列缺失
     */
    public Map<String, Integer> validateAndMapColumns(Row headerRow, String sheetName) {
        if (headerRow == null) {
            throw new ParseException("Header row is null")
                .withContext(sheetName, 8);
        }

        Map<String, Integer> columnMap = new LinkedHashMap<>();

        // 遍历所有单元格，建立映射
        for (Cell cell : headerRow) {
            String rawName = getCellValue(cell);
            if (rawName != null && !rawName.isEmpty()) {
                String normalized = ColumnNormalizer.normalize(rawName);
                columnMap.put(normalized, cell.getColumnIndex());
            }
        }

        // 验证必需列
        List<String> missingColumns = new ArrayList<>();
        for (String required : REQUIRED_COLUMNS) {
            if (!columnMap.containsKey(required)) {
                missingColumns.add(required);
            }
        }

        if (!missingColumns.isEmpty()) {
            throw new ParseException(
                "Required column(s) not found: " + String.join(", ", missingColumns))
                .withContext(sheetName, 8);
        }

        return columnMap;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    /**
     * 获取必需列列表
     */
    public static List<String> getRequiredColumns() {
        return Collections.unmodifiableList(REQUIRED_COLUMNS);
    }

    /**
     * 获取可选列列表
     */
    public static List<String> getOptionalColumns() {
        return Collections.unmodifiableList(OPTIONAL_COLUMNS);
    }
}
```

### 列索引常量类

```java
package com.rtm.mq.tool.parser;

/**
 * 列名常量
 */
public final class ColumnNames {
    private ColumnNames() {}

    public static final String SEG_LVL = "Seg lvl";
    public static final String FIELD_NAME = "Field Name";
    public static final String DESCRIPTION = "Description";
    public static final String LENGTH = "Length";
    public static final String MESSAGING_DATATYPE = "Messaging Datatype";
    public static final String OPTIONALITY = "Opt(O/M)";
    public static final String NULL_YN = "Null (Y/N)";
    public static final String NLS_YN = "NLS (Y/N)";
    public static final String SAMPLE_VALUES = "Sample Value(s)";
    public static final String REMARKS = "Remarks";
    public static final String GMR_PHYSICAL_NAME = "GMR Physical Name";
    public static final String TEST_VALUE = "Test Value";
}
```

## Acceptance Criteria

1. [ ] 正确发现 Request、Response Sheet
2. [ ] 可选发现 Shared Header Sheet
3. [ ] 必需 Sheet 缺失时抛出 ParseException
4. [ ] 正确映射所有列到索引
5. [ ] 必需列缺失时抛出 ParseException，列出所有缺失列
6. [ ] 使用 LinkedHashMap 保持列顺序
7. [ ] 单元测试覆盖正常和异常场景

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 发现所有 Sheet | 返回 SheetSet |
| 单元测试 | Request Sheet 缺失 | 抛出 ParseException |
| 单元测试 | Response Sheet 缺失 | 抛出 ParseException |
| 单元测试 | 验证完整列 | 返回正确映射 |
| 单元测试 | 必需列缺失 | 抛出 ParseException |
| 单元测试 | 列名含换行符 | 正确规范化匹配 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Sheet 名称大小写 | 发现失败 | 考虑忽略大小写 |
| 空 Header 行 | 映射失败 | 验证 Header 行非空 |
| 重复列名 | 映射覆盖 | 警告或报错 |
