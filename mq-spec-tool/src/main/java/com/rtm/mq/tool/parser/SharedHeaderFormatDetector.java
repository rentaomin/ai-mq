package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.HashSet;
import java.util.Set;

/**
 * Detector for shared header file formats.
 *
 * <p>Identifies whether a shared header file is in standard hierarchical format
 * or ISM v2.0 FIX fixed-format specification.</p>
 *
 * <p>Format characteristics:</p>
 * <ul>
 *   <li>STANDARD: Has metadata rows (1-7), hierarchy levels, "Seg lvl" column</li>
 *   <li>ISM_V2_FIX: No metadata, fixed-format fields, "Start Position" column</li>
 * </ul>
 *
 * @see SharedHeaderLoader
 * @see IsmV2FixParser
 */
public class SharedHeaderFormatDetector {

    /**
     * Enumeration of detected file formats.
     */
    public enum FileFormat {
        /** Standard message specification format (hierarchical, with metadata) */
        STANDARD,
        /** ISM v2.0 FIX format (fixed-format, no metadata) */
        ISM_V2_FIX,
        /** Format could not be determined */
        UNKNOWN
    }

    /**
     * Detects the format of a shared header file.
     *
     * <p>Detection strategy (in order of priority):</p>
     * <ol>
     *   <li>Check sheet name for "ISM V2.0 FIX" pattern → ISM_V2_FIX</li>
     *   <li>Check row 1-2 for standard metadata presence</li>
     *   <li>Check column headers for format-specific columns</li>
     *   <li>Default → UNKNOWN</li>
     * </ol>
     *
     * @param workbook the Excel workbook
     * @param sheet the sheet to analyze
     * @return the detected file format
     * @throws ParseException if format cannot be determined
     */
    public FileFormat detectFormat(Workbook workbook, Sheet sheet) {
        if (sheet == null) {
            throw new ParseException("Sheet is null");
        }

        // Priority 1: Check sheet name for ISM pattern
        String sheetName = sheet.getSheetName();
        if (sheetName != null && sheetName.toLowerCase().contains("ism") &&
            sheetName.toLowerCase().contains("v2") &&
            sheetName.toLowerCase().contains("fix")) {
            return FileFormat.ISM_V2_FIX;
        }

        // Priority 2: Check for metadata rows (standard format only)
        // Row 2 (0-indexed 1) should have "Operation Name" in standard format
        Row metadataRow = sheet.getRow(1);
        if (metadataRow != null) {
            String metadataLabel = getCellStringValue(metadataRow.getCell(1)); // Column B
            if (metadataLabel != null && metadataLabel.toLowerCase().contains("operation name")) {
                return FileFormat.STANDARD;
            }
        }

        // Priority 3: Check for format-specific column headers
        Set<String> headerColumns = extractHeaderColumns(sheet);

        // Check for ISM columns
        if (hasAnyOf(headerColumns, "start position", "start pos", "byte offset") &&
            hasAnyOf(headerColumns, "status", "opt", "m/o")) {
            return FileFormat.ISM_V2_FIX;
        }

        // Check for standard columns
        if (headerColumns.contains("seg lvl") || headerColumns.contains("seglvl")) {
            return FileFormat.STANDARD;
        }

        // Unable to determine format
        throw new ParseException(
            "Unable to determine shared header file format. " +
            "File must be either standard format (with 'Seg lvl' column) or " +
            "ISM v2.0 FIX format (with 'Start Position' column). " +
            "Found columns: " + headerColumns
        );
    }

    /**
     * Extracts all non-empty column headers from the first few rows.
     *
     * <p>This searches rows 0-8 for column headers, as different formats
     * may have headers at different row positions.</p>
     *
     * @param sheet the sheet to analyze
     * @return a set of normalized column names found
     */
    private Set<String> extractHeaderColumns(Sheet sheet) {
        Set<String> columns = new HashSet<>();

        // Search first 9 rows for column headers
        for (int rowIndex = 0; rowIndex <= 8 && rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            // Check each cell in the row
            for (Cell cell : row) {
                String cellValue = getCellStringValue(cell);
                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    String normalized = normalizeName(cellValue);
                    columns.add(normalized);
                }
            }
        }

        return columns;
    }

    /**
     * Normalizes a column name for comparison.
     *
     * <p>Converts to lowercase and removes extra spaces.</p>
     *
     * @param name the original name
     * @return the normalized name
     */
    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Checks if any of the target names appear in the columns set.
     *
     * <p>Uses substring matching for flexibility with variations.</p>
     *
     * @param columns the set of column names to search
     * @param targets the target names to find (case-insensitive)
     * @return true if any target is found in columns
     */
    private boolean hasAnyOf(Set<String> columns, String... targets) {
        for (String target : targets) {
            String normalized = normalizeName(target);
            for (String column : columns) {
                if (column.contains(normalized) || normalized.contains(column)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extracts the string value from a cell.
     *
     * <p>Handles different cell types (STRING, NUMERIC, FORMULA, etc.).</p>
     *
     * @param cell the cell to read (may be null)
     * @return the cell value as a string, or null if empty/unsupported
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
}
