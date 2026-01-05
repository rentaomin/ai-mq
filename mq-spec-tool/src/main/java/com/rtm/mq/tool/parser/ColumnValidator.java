package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Column validator for Excel sheets.
 *
 * <p>Validates that required columns exist in the header row and builds
 * a column name to index mapping for field extraction.</p>
 *
 * <p>Required columns:</p>
 * <ul>
 *   <li>Seg lvl - Segment level for hierarchy</li>
 *   <li>Field Name - The field identifier</li>
 *   <li>Description - Field description</li>
 *   <li>Length - Field length specification</li>
 *   <li>Messaging Datatype - Data type of the field</li>
 * </ul>
 */
public class ColumnValidator {

    /** Required column names (normalized) */
    private static final List<String> REQUIRED_COLUMNS = Arrays.asList(
        "Seg lvl",
        "Field Name",
        "Description",
        "Length",
        "Messaging Datatype"
    );

    /** Optional column names */
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
     * Validates and maps columns from the header row.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Iterates through all cells in the header row</li>
     *   <li>Normalizes each column name using {@link ColumnNormalizer}</li>
     *   <li>Builds a LinkedHashMap preserving column order</li>
     *   <li>Validates that all required columns are present</li>
     * </ol>
     *
     * @param headerRow the header row (typically row 8, 0-indexed as 7)
     * @param sheetName the sheet name for error messages
     * @return a LinkedHashMap mapping normalized column names to their indices
     * @throws ParseException if the header row is null or required columns are missing
     */
    public Map<String, Integer> validateAndMapColumns(Row headerRow, String sheetName) {
        if (headerRow == null) {
            throw new ParseException("Header row is null")
                .withContext(sheetName, 8);
        }

        Map<String, Integer> columnMap = new LinkedHashMap<>();

        // Iterate through all cells and build the mapping
        for (Cell cell : headerRow) {
            String rawName = getCellValue(cell);
            if (rawName != null && !rawName.isEmpty()) {
                String normalized = ColumnNormalizer.normalize(rawName);
                if (normalized != null && !normalized.isEmpty()) {
                    // Note: If duplicate column names exist, later ones will overwrite earlier
                    columnMap.put(normalized, cell.getColumnIndex());
                }
            }
        }

        // Validate required columns
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
     * Gets the string value from a cell.
     *
     * @param cell the cell to read
     * @return the cell value as a string, or null if the cell is empty or unsupported type
     */
    private String getCellValue(Cell cell) {
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
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    /**
     * Gets the list of required column names.
     *
     * @return an unmodifiable list of required column names
     */
    public static List<String> getRequiredColumns() {
        return Collections.unmodifiableList(REQUIRED_COLUMNS);
    }

    /**
     * Gets the list of optional column names.
     *
     * @return an unmodifiable list of optional column names
     */
    public static List<String> getOptionalColumns() {
        return Collections.unmodifiableList(OPTIONAL_COLUMNS);
    }
}
