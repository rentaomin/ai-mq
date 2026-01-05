package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.Metadata;
import com.rtm.mq.tool.version.VersionRegistry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Excel Metadata Extractor.
 * Extracts Operation Name, Operation ID, Version and other metadata from the first 7 rows.
 *
 * <p>Expected Excel metadata row structure (1-based):
 * <pre>
 * Row 1: Title "Message Specification"
 * Row 2: "" | "Operation Name " | value: "Create application from SMP"
 * Row 3: "" | "Operation ID" | value: "CreateAppSMP" | "Version" | value: "01.00"
 * Row 4: "" | "Service Category" | value | "Service Interface" | value
 * Row 5: "" | "Service Component" | value | "Service ID" | value
 * Row 6: "" | "Description" | value
 * Row 7: (empty row)
 * Row 8: Header row
 * </pre>
 */
public class MetadataExtractor {

    // Row indices (0-based)
    private static final int ROW_OPERATION_NAME = 1;    // Row 2
    private static final int ROW_OPERATION_ID = 2;      // Row 3

    // Column indices (0-based)
    private static final int COL_LABEL = 1;             // Column B
    private static final int COL_VALUE = 2;             // Column C
    private static final int COL_VERSION_LABEL = 3;     // Column D
    private static final int COL_VERSION_VALUE = 4;     // Column E

    /**
     * Extracts metadata from an Excel Sheet.
     *
     * @param sheet           Excel Sheet (Request or any sheet containing metadata)
     * @param sourceFile      path to the source Excel file
     * @param sharedHeaderFile optional path to the shared header file (may be null)
     * @return a populated Metadata object
     */
    public Metadata extract(Sheet sheet, Path sourceFile, Path sharedHeaderFile) {
        Metadata meta = new Metadata();

        // Set file paths
        meta.setSourceFile(sourceFile.toAbsolutePath().toString());
        if (sharedHeaderFile != null) {
            meta.setSharedHeaderFile(sharedHeaderFile.toAbsolutePath().toString());
        }

        // Set parse timestamp (ISO 8601 format) and parser version
        meta.setParseTimestamp(Instant.now().toString());
        meta.setParserVersion(VersionRegistry.getParserVersion());

        // Extract Operation Name (Row 2, Column C)
        String operationName = extractCellValue(sheet, ROW_OPERATION_NAME, COL_VALUE);
        meta.setOperationName(trimOrNull(operationName));

        // Extract Operation ID (Row 3, Column C)
        String operationId = extractCellValue(sheet, ROW_OPERATION_ID, COL_VALUE);
        meta.setOperationId(trimOrNull(operationId));

        // Extract Version (Row 3, Column E)
        String version = extractCellValue(sheet, ROW_OPERATION_ID, COL_VERSION_VALUE);
        meta.setVersion(trimOrNull(version));

        return meta;
    }

    /**
     * Extracts a cell value as a String.
     *
     * @param sheet    the Excel sheet
     * @param rowIndex 0-based row index
     * @param colIndex 0-based column index
     * @return the cell value as a String, or null if empty/missing
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
                // Handle version numbers that may be stored as numeric values
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case FORMULA:
                // Try to get the formula result
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
     * Trims whitespace from the value and returns null for empty strings.
     *
     * @param value the string value to process
     * @return trimmed value, or null if empty or null
     */
    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Validates that required metadata fields are present.
     *
     * @param meta the Metadata object to validate
     * @return true if all required fields are present, false otherwise
     */
    public boolean validate(Metadata meta) {
        // Operation ID is required (used for generating class names)
        return meta.getOperationId() != null && !meta.getOperationId().isEmpty();
    }

    /**
     * Returns a comma-separated list of missing required fields.
     *
     * @param meta the Metadata object to check
     * @return a string listing missing fields, or empty string if none
     */
    public String getMissingFields(Metadata meta) {
        StringBuilder missing = new StringBuilder();
        if (meta.getOperationId() == null || meta.getOperationId().isEmpty()) {
            missing.append("Operation ID, ");
        }
        // Operation Name can be derived from Operation ID, not required
        // Version is optional

        if (missing.length() > 0) {
            return missing.substring(0, missing.length() - 2);
        }
        return "";
    }
}
