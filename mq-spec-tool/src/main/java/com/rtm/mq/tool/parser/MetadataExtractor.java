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
 *
 * <p>Support for multiple metadata extraction sources:</p>
 * <ul>
 *   <li>Primary: Request Sheet (typical case)</li>
 *   <li>Fallback: Shared Header Sheet (if embedded)</li>
 *   <li>Fallback: Shared Header File (separate file case)</li>
 * </ul>
 */
public class MetadataExtractor {

    // Row indices (0-based)
    private static final int ROW_OPERATION_NAME = 1;    // Row 2
    private static final int ROW_OPERATION_ID = 2;      // Row 3
    private static final int ROW_SERVICE_CATEGORY = 3;      // Row 3
    private static final int ROW_SERVICE_COMPONENT = 4;      // Row 4
    private static final int ROW_DESCRIPTION = 5;      // Row 5

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

        // Extract metadata from the provided sheet
        extractMetadataFields(sheet, meta);

        return meta;
    }

    /**
     * Extracts metadata from an Excel Sheet with null safety.
     *
     * <p>This method handles cases where the sheet may be null,
     * rows may not exist, or cells may be missing.</p>
     *
     * @param sheet           Excel Sheet (may be null)
     * @param sourceFile      path to the source Excel file
     * @param sharedHeaderFile optional path to the shared header file
     * @return a populated Metadata object, or empty if sheet is null
     */
    public Metadata extractSafely(Sheet sheet, Path sourceFile, Path sharedHeaderFile) {
        if (sheet == null) {
            Metadata meta = new Metadata();
            meta.setSourceFile(sourceFile.toAbsolutePath().toString());
            if (sharedHeaderFile != null) {
                meta.setSharedHeaderFile(sharedHeaderFile.toAbsolutePath().toString());
            }
            meta.setParseTimestamp(Instant.now().toString());
            meta.setParserVersion(VersionRegistry.getParserVersion());
            return meta;
        }
        return extract(sheet, sourceFile, sharedHeaderFile);
    }

    /**
     * Internal method to extract metadata fields from a sheet.
     *
     * @param sheet the sheet to extract from (may be null)
     * @param meta  the Metadata object to populate
     */
    private void extractMetadataFields(Sheet sheet, Metadata meta) {
        if (sheet == null) {
            return;
        }

        // Extract Operation Name (Row 2, Column C)
        String operationName = extractCellValue(sheet, ROW_OPERATION_NAME, COL_VALUE);
        meta.setOperationName(trimOrNull(operationName));

        // Extract Operation ID (Row 3, Column C)
        String operationId = extractCellValue(sheet, ROW_OPERATION_ID, COL_VALUE);
        meta.setOperationId(trimOrNull(operationId));

        // Extract Version (Row 3, Column E)
        String version = extractCellValue(sheet, ROW_OPERATION_ID, COL_VERSION_VALUE);
        meta.setVersion(trimOrNull(version));

        // Extract Version (Row 4, Column E)
        String serviceCategory = extractCellValue(sheet, ROW_SERVICE_CATEGORY, COL_VERSION_VALUE);
        meta.setServiceCategory(trimOrNull(serviceCategory));

        // Extract Version (Row 4, Column E)
        String serviceInterface = extractCellValue(sheet, ROW_SERVICE_CATEGORY, COL_VERSION_VALUE);
        meta.setServiceInterface(trimOrNull(serviceInterface));

        // Extract Version (Row 5, Column E)
        String serviceComponent= extractCellValue(sheet, ROW_SERVICE_COMPONENT, COL_VERSION_VALUE);
        meta.setServiceComponent(trimOrNull(serviceComponent));
        
        // Extract Version (Row 5, Column E)
        String serviceID = extractCellValue(sheet, ROW_SERVICE_COMPONENT, COL_VERSION_VALUE);
        meta.setServiceID(trimOrNull(serviceID));

        // Extract Version (Row 6, Column E)
        String description = extractCellValue(sheet, ROW_DESCRIPTION, COL_VERSION_VALUE);
        meta.setDescription(trimOrNull(description));
        
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
