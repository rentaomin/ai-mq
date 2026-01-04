# T-103 元数据提取器

## Goal

实现 Excel 元数据提取器，从前 7 行提取 Operation Name、Operation ID、Version 等元数据信息。

## In Scope / Out of Scope

**In Scope**:
- MetadataExtractor 类实现
- 从 Row 2-6 提取元数据
- 生成解析时间戳
- 填充 Metadata 对象

**Out of Scope**:
- 数据行解析（T-104）
- 版本号验证

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.3 节（元数据提取）
- T-102 Sheet 发现与验证

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
└── MetadataExtractor.java

src/test/java/com/rtm/mq/tool/parser/
└── MetadataExtractorTest.java
```

## Dependencies

- T-102 Sheet 发现与验证
- T-004 数据模型定义（Metadata 类）
- T-006 版本注册器

## Implementation Notes

### Excel 元数据行结构

根据架构文档 5.1.1，元数据行位置：

```
Row 1: 标题 "Message Specification"
Row 2: "" | "Operation Name " | <值: "Create application from SMP">
Row 3: "" | "Operation ID" | <值: "CreateAppSMP"> | "Version" | <值: "01.00">
Row 4: "" | "Service Category" | <值> | "Service Interface" | <值>
Row 5: "" | "Service Compoment" | <值> | "Service ID" | <值>
Row 6: "" | "Description" | <值>
Row 7: (空行)
Row 8: Header 行
```

### MetadataExtractor 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.Metadata;
import com.rtm.mq.tool.version.VersionRegistry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Excel 元数据提取器
 * 从前 7 行提取 Operation Name、Operation ID、Version 等元数据
 */
public class MetadataExtractor {

    // 行索引（0-based）
    private static final int ROW_OPERATION_NAME = 1;    // Row 2
    private static final int ROW_OPERATION_ID = 2;      // Row 3

    // 列索引（0-based）
    private static final int COL_LABEL = 1;             // Column B
    private static final int COL_VALUE = 2;             // Column C
    private static final int COL_VERSION_LABEL = 3;     // Column D
    private static final int COL_VERSION_VALUE = 4;     // Column E

    /**
     * 从 Sheet 提取元数据
     *
     * @param sheet Excel Sheet（Request 或任意包含元数据的 Sheet）
     * @param sourceFile 源文件路径
     * @param sharedHeaderFile 可选的 Shared Header 文件路径
     * @return 填充好的 Metadata 对象
     */
    public Metadata extract(Sheet sheet, Path sourceFile, Path sharedHeaderFile) {
        Metadata meta = new Metadata();

        // 设置文件路径
        meta.setSourceFile(sourceFile.toAbsolutePath().toString());
        if (sharedHeaderFile != null) {
            meta.setSharedHeaderFile(sharedHeaderFile.toAbsolutePath().toString());
        }

        // 设置解析时间和版本
        meta.setParseTimestamp(Instant.now().toString());
        meta.setParserVersion(VersionRegistry.getParserVersion());

        // 提取 Operation Name (Row 2, Column C)
        String operationName = extractCellValue(sheet, ROW_OPERATION_NAME, COL_VALUE);
        meta.setOperationName(trimOrNull(operationName));

        // 提取 Operation ID (Row 3, Column C)
        String operationId = extractCellValue(sheet, ROW_OPERATION_ID, COL_VALUE);
        meta.setOperationId(trimOrNull(operationId));

        // 提取 Version (Row 3, Column E)
        String version = extractCellValue(sheet, ROW_OPERATION_ID, COL_VERSION_VALUE);
        meta.setVersion(trimOrNull(version));

        return meta;
    }

    /**
     * 提取单元格值
     */
    private String extractCellValue(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
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
                // 处理版本号可能是数字格式
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case FORMULA:
                // 尝试获取公式计算结果
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    /**
     * 去除前后空白，空字符串返回 null
     */
    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 验证必需的元数据
     *
     * @param meta 元数据对象
     * @return 是否有效
     */
    public boolean validate(Metadata meta) {
        // Operation ID 是必需的（用于生成类名）
        return meta.getOperationId() != null && !meta.getOperationId().isEmpty();
    }

    /**
     * 获取缺失的必需字段
     */
    public String getMissingFields(Metadata meta) {
        StringBuilder missing = new StringBuilder();
        if (meta.getOperationId() == null || meta.getOperationId().isEmpty()) {
            missing.append("Operation ID, ");
        }
        // Operation Name 可以从 Operation ID 推导，非必需
        // Version 非必需

        if (missing.length() > 0) {
            return missing.substring(0, missing.length() - 2);
        }
        return "";
    }
}
```

### 测试用例

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.Metadata;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MetadataExtractorTest {

    private MetadataExtractor extractor;
    private Workbook workbook;

    @BeforeEach
    void setUp() {
        extractor = new MetadataExtractor();
        workbook = new XSSFWorkbook();
    }

    @Test
    void extract_withValidMetadata_extractsAll() {
        Sheet sheet = createSheetWithMetadata("Create application from SMP", "CreateAppSMP", "01.00");

        Metadata meta = extractor.extract(sheet, Paths.get("test.xlsx"), null);

        assertEquals("Create application from SMP", meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
        assertEquals("01.00", meta.getVersion());
        assertNotNull(meta.getParseTimestamp());
        assertNotNull(meta.getParserVersion());
    }

    @Test
    void extract_withMissingOperationName_returnsNull() {
        Sheet sheet = createSheetWithMetadata(null, "CreateAppSMP", "01.00");

        Metadata meta = extractor.extract(sheet, Paths.get("test.xlsx"), null);

        assertNull(meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
    }

    @Test
    void validate_withOperationId_returnsTrue() {
        Metadata meta = new Metadata();
        meta.setOperationId("CreateAppSMP");

        assertTrue(extractor.validate(meta));
    }

    @Test
    void validate_withoutOperationId_returnsFalse() {
        Metadata meta = new Metadata();

        assertFalse(extractor.validate(meta));
    }

    private Sheet createSheetWithMetadata(String operationName, String operationId, String version) {
        Sheet sheet = workbook.createSheet("Request");

        // Row 2 (index 1): Operation Name
        Row row2 = sheet.createRow(1);
        row2.createCell(1).setCellValue("Operation Name");
        if (operationName != null) {
            row2.createCell(2).setCellValue(operationName);
        }

        // Row 3 (index 2): Operation ID and Version
        Row row3 = sheet.createRow(2);
        row3.createCell(1).setCellValue("Operation ID");
        if (operationId != null) {
            row3.createCell(2).setCellValue(operationId);
        }
        row3.createCell(3).setCellValue("Version");
        if (version != null) {
            row3.createCell(4).setCellValue(version);
        }

        return sheet;
    }
}
```

## Acceptance Criteria

1. [ ] 正确提取 Operation Name（Row 2, Column C）
2. [ ] 正确提取 Operation ID（Row 3, Column C）
3. [ ] 正确提取 Version（Row 3, Column E）
4. [ ] 正确设置 parseTimestamp（ISO 8601 格式）
5. [ ] 正确设置 parserVersion
6. [ ] 处理空单元格返回 null
7. [ ] 验证必需字段（Operation ID）
8. [ ] 单元测试覆盖率 > 80%

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 提取完整元数据 | 所有字段正确 |
| 单元测试 | 缺失 Operation Name | 返回 null |
| 单元测试 | 数字版本号 | 正确转换 |
| 单元测试 | 验证必需字段 | 正确识别缺失 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 版本号是数字 | 格式错误 | 类型判断转换 |
| 单元格含公式 | 提取失败 | 尝试获取计算值 |
| 行/列位置变化 | 提取错误 | 配置化行列索引 |
