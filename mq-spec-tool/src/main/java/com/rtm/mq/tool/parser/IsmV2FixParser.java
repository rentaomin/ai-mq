package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for ISM v2.0 FIX fixed-format message specifications.
 *
 * <p>ISM v2.0 FIX files define fixed-format message structures with:</p>
 * <ul>
 *   <li>No metadata rows (no Operation ID, Version, etc.)</li>
 *   <li>No hierarchical structure (all fields at level 1)</li>
 *   <li>Fixed-format byte offsets and field lengths</li>
 *   <li>Column structure: Field name, Start Position, Length, Type, Status, Examples</li>
 * </ul>
 *
 * <p>This parser converts ISM format to standard FieldNode structure
 * for compatibility with the rest of the parsing pipeline.</p>
 *
 * @see SharedHeaderFormatDetector
 * @see SharedHeaderLoader
 */
public class IsmV2FixParser {

    // Column names for ISM format (case-insensitive matching)
    private static final String COL_FIELD_NAME = "field_name";
    private static final String COL_START_POS = "start_pos";
    private static final String COL_LENGTH = "length";
    private static final String COL_TYPE = "type";
    private static final String COL_STATUS = "status";

    private final CamelCaseConverter camelCaseConverter = new CamelCaseConverter();

    /**
     * Parses an ISM v2.0 FIX sheet into a FieldGroup.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Detects ISM column positions</li>
     *   <li>Parses each row into a FieldNode</li>
     *   <li>Returns all fields in a FieldGroup</li>
     * </ol>
     *
     * @param sheet the Excel sheet containing ISM v2.0 FIX data
     * @return the parsed FieldGroup containing all fields
     * @throws ParseException if the sheet format is invalid
     */
    public FieldGroup parse(Sheet sheet) {
        if (sheet == null) {
            return new FieldGroup();
        }

        // Detect column positions
        Map<String, Integer> columnMap = detectColumns(sheet);

        // Parse all fields
        List<FieldNode> fields = parseFields(sheet, columnMap);

        FieldGroup group = new FieldGroup();
        group.setFields(fields);
        return group;
    }

    /**
     * Detects the column positions for ISM v2.0 FIX format.
     *
     * <p>Searches the first few rows for column headers with flexible matching:</p>
     * <ul>
     *   <li>Field name: "Element", "Field name", "Field Name"</li>
     *   <li>Start Position: "Start Position", "Start Pos", "Byte Offset"</li>
     *   <li>Length: "Length", "Len"</li>
     *   <li>Type: "Type", "Data Type", "Datatype"</li>
     *   <li>Status: "Status", "Opt", "M/O"</li>
     * </ul>
     *
     * @param sheet the sheet to analyze
     * @return a map of column names to their indices (0-based)
     * @throws ParseException if required columns are not found
     */
    private Map<String, Integer> detectColumns(Sheet sheet) {
        Map<String, Integer> columnMap = new HashMap<>();

        // Search first 3 rows for headers (ISM format has no metadata rows)
        for (int rowIndex = 0; rowIndex <= 2 && rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            // Check each cell
            for (Cell cell : row) {
                String cellValue = getCellStringValue(cell);
                if (cellValue == null || cellValue.trim().isEmpty()) {
                    continue;
                }

                String normalized = cellValue.trim().toLowerCase();
                int colIndex = cell.getColumnIndex();

                // Match field name column
                if (normalized.contains("element") || normalized.equals("field name")) {
                    columnMap.put(COL_FIELD_NAME, colIndex);
                }
                // Match start position column
                else if ((normalized.contains("start") && normalized.contains("pos")) ||
                         normalized.contains("byte offset")) {
                    columnMap.put(COL_START_POS, colIndex);
                }
                // Match length column
                else if (normalized.equals("length") || normalized.equals("len")) {
                    columnMap.put(COL_LENGTH, colIndex);
                }
                // Match type column
                else if (normalized.equals("type") || normalized.contains("data type")) {
                    columnMap.put(COL_TYPE, colIndex);
                }
                // Match status column
                else if (normalized.equals("status") || normalized.equals("opt") || normalized.equals("m/o")) {
                    columnMap.put(COL_STATUS, colIndex);
                }
            }

            // If we found all required columns, stop searching
            if (columnMap.size() >= 5) {
                break;
            }
        }

        // Validate required columns were found
        validateColumns(columnMap);

        return columnMap;
    }

    /**
     * Validates that all required ISM columns were found.
     *
     * @param columnMap the detected column map
     * @throws ParseException if any required columns are missing
     */
    private void validateColumns(Map<String, Integer> columnMap) {
        List<String> missing = new ArrayList<>();

        if (!columnMap.containsKey(COL_FIELD_NAME)) {
            missing.add("Field name");
        }
        if (!columnMap.containsKey(COL_START_POS)) {
            missing.add("Start Position");
        }
        if (!columnMap.containsKey(COL_LENGTH)) {
            missing.add("Length");
        }
        if (!columnMap.containsKey(COL_TYPE)) {
            missing.add("Type");
        }
        if (!columnMap.containsKey(COL_STATUS)) {
            missing.add("Status");
        }

        if (!missing.isEmpty()) {
            throw new ParseException(
                "ISM v2.0 FIX format detected but required columns missing: " + missing +
                ". Expected columns: Field name, Start Position, Length, Type, Status"
            );
        }
    }

    /**
     * Parses all field rows into FieldNode objects.
     *
     * <p>ISM format has no metadata rows, so all rows from row 0 onwards
     * are processed as field definitions.</p>
     *
     * @param sheet the sheet to parse
     * @param columnMap the detected column positions
     * @return a list of parsed FieldNode objects
     */
    private List<FieldNode> parseFields(Sheet sheet, Map<String, Integer> columnMap) {
        List<FieldNode> fields = new ArrayList<>();

        // Skip header row (row 0, column headers)
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            // Skip empty rows
            if (isEmptyRow(row)) {
                continue;
            }

            // Try to parse the field
            FieldNode field = createFieldNode(row, columnMap, rowIndex);
            if (field != null) {
                fields.add(field);
            }
        }

        if (fields.isEmpty()) {
            throw new ParseException("ISM v2.0 FIX file has no data rows");
        }

        return fields;
    }

    /**
     * Creates a FieldNode from an ISM format row.
     *
     * @param row the row to parse
     * @param columnMap the column position map
     * @param rowIndex the 0-based row index
     * @return a FieldNode, or null if the row cannot be parsed
     */
    private FieldNode createFieldNode(Row row, Map<String, Integer> columnMap, int rowIndex) {
        String fieldName = getCellStringValue(row.getCell(columnMap.get(COL_FIELD_NAME)));

        // Skip if no field name
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }

        fieldName = fieldName.trim();
        String startPos = getCellStringValue(row.getCell(columnMap.get(COL_START_POS)));
        String length = getCellStringValue(row.getCell(columnMap.get(COL_LENGTH)));
        String type = getCellStringValue(row.getCell(columnMap.get(COL_TYPE)));
        String status = getCellStringValue(row.getCell(columnMap.get(COL_STATUS)));

        // Build source metadata with row index (1-based for Excel)
        SourceMetadata source = new SourceMetadata();
        source.setSheetName("ISM V2.0 FIX");
        source.setRowIndex(rowIndex + 1);  // Convert 0-based to 1-based

        // Parse byte offset if available
        Integer byteOffset = parseInteger(startPos);

        // Build FieldNode using builder pattern
        FieldNode.Builder builder = FieldNode.builder()
            .originalName(fieldName)
            .camelCaseName(camelCaseConverter.toCamelCase(fieldName))
            .segLevel(1)  // ISM fields are all at level 1 (no hierarchy)
            .length(parseInteger(length))
            .dataType(mapDataType(type))
            .optionality(mapOptStatus(status))
            .isArray(false)
            .isObject(false)
            .source(source);

        // Store byte offset in source remarks if available
        if (byteOffset != null) {
            source.setByteOffset(byteOffset);
        }

        return builder.build();
    }

    /**
     * Maps ISM type names to standard data type codes.
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li>"Character String" → "A/N"</li>
     *   <li>"Numeric" → "N"</li>
     *   <li>"Alphanumeric" → "A/N"</li>
     *   <li>Other → Store as-is</li>
     * </ul>
     *
     * @param ismType the ISM type name
     * @return the standard data type code
     */
    private String mapDataType(String ismType) {
        if (ismType == null) {
            return null;
        }

        String normalized = ismType.trim().toLowerCase();

        if (normalized.contains("character") && normalized.contains("string")) {
            return "A/N";
        }
        if (normalized.equals("numeric") || normalized.equals("n")) {
            return "N";
        }
        if (normalized.contains("alphanumeric")) {
            return "A/N";
        }

        // Store unknown types as-is
        return ismType.trim();
    }

    /**
     * Maps ISM optionality status to standard codes.
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li>"M" or "Mandatory" → "M"</li>
     *   <li>"O" or "Optional" → "O"</li>
     *   <li>Default → "O" (optional)</li>
     * </ul>
     *
     * @param status the ISM status value
     * @return the standard optionality code ("M" or "O")
     */
    private String mapOptStatus(String status) {
        if (status == null) {
            return "O";  // Default to optional
        }

        String normalized = status.trim().toUpperCase();

        if ("M".equals(normalized) || "MANDATORY".equals(normalized)) {
            return "M";
        }
        if ("O".equals(normalized) || "OPTIONAL".equals(normalized)) {
            return "O";
        }

        return "O";  // Default to optional for unknown values
    }

    /**
     * Parses a string to an integer.
     *
     * @param value the string value
     * @return the parsed integer, or null if the value is not a valid integer
     */
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if a row is empty (all cells are null or empty).
     *
     * @param row the row to check (may be null)
     * @return true if the row is empty or null
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            String value = getCellStringValue(cell);
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }

        return true;
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
