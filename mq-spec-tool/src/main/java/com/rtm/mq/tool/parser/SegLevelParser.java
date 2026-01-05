package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.*;

/**
 * Segment level nesting parser.
 *
 * <p>Implements a stack-based algorithm to parse Excel field specifications
 * into a hierarchical tree structure based on the "Seg lvl" column.</p>
 *
 * <p>The parser maintains a stack of parent nodes and processes rows
 * sequentially starting from row 9 (0-indexed as row 8). For each row:</p>
 * <ul>
 *   <li>If Seg lvl == current stack depth + 1: adds to top of stack as child</li>
 *   <li>If Seg lvl <= current stack depth: pops stack to find correct parent</li>
 *   <li>Container nodes (objects/arrays) are pushed onto the stack</li>
 * </ul>
 *
 * <p>This implementation preserves field order exactly as defined in the
 * Excel specification, which is critical for message serialization.</p>
 *
 * @see NestingDepthValidator
 * @see FieldNode
 */
public class SegLevelParser {

    private static final int DATA_START_ROW = 8;  // Row 9 (0-indexed = 8)

    private final NestingDepthValidator depthValidator;
    private final Map<String, Integer> columnMap;
    private final String sheetName;

    /**
     * Creates a parser with default nesting depth validation.
     *
     * @param columnMap the column name to index mapping
     * @param sheetName the name of the source sheet (for error reporting)
     */
    public SegLevelParser(Map<String, Integer> columnMap, String sheetName) {
        this(columnMap, sheetName, new NestingDepthValidator());
    }

    /**
     * Creates a parser with custom nesting depth validation.
     *
     * @param columnMap the column name to index mapping
     * @param sheetName the name of the source sheet (for error reporting)
     * @param depthValidator the depth validator to use
     */
    public SegLevelParser(Map<String, Integer> columnMap, String sheetName,
                          NestingDepthValidator depthValidator) {
        this.columnMap = columnMap;
        this.sheetName = sheetName;
        this.depthValidator = depthValidator;
    }

    /**
     * Parses field definitions from an Excel sheet into a hierarchical tree.
     *
     * <p>Processing begins at row 9 (0-indexed as 8) and continues to the
     * last row in the sheet. Empty rows are skipped. The returned list
     * contains only top-level fields (Seg lvl = 1), with nested fields
     * accessible through the children property.</p>
     *
     * @param sheet the Excel sheet containing field definitions
     * @return list of root-level fields in specification order
     * @throws ParseException if Seg lvl is invalid or has illegal jumps
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

            // Create basic FieldNode from Excel data
            FieldNode node = createBasicFieldNode(row, i + 1);  // 1-based row index
            if (node == null) {
                continue;
            }

            int segLevel = node.getSegLevel();

            // Validate Seg lvl for invalid values and illegal jumps
            validateSegLevel(segLevel, previousLevel, i + 1, node.getOriginalName());

            // Validate nesting depth (logs warning if exceeded)
            depthValidator.validateDepth(segLevel, i + 1, node.getOriginalName());

            // Pop stack until we find the correct parent level
            while (!stack.isEmpty() && stack.peek().getSegLevel() >= segLevel) {
                stack.pop();
            }

            // Add node to correct location in tree
            if (stack.isEmpty()) {
                // Top-level field (Seg lvl = 1)
                rootFields.add(node);
            } else {
                // Nested field - add to parent's children
                stack.peek().getChildren().add(node);
            }

            // If this is a potential container (object/array), push onto stack
            // The actual determination of isObject/isArray happens in T-105
            if (isContainerCandidate(node)) {
                stack.push(node);
            }

            previousLevel = segLevel;
        }

        return rootFields;
    }

    /**
     * Determines if a field node might be a container (object or array).
     *
     * <p>Container candidates are identified by a field name containing a colon (:).
     * This is a preliminary check - the actual type determination (object vs array)
     * is performed by T-105 based on groupId and occurrenceCount analysis.</p>
     *
     * @param node the field node to check
     * @return true if this node might be a container
     */
    private boolean isContainerCandidate(FieldNode node) {
        String fieldName = node.getOriginalName();
        return fieldName != null && fieldName.contains(":");
    }

    /**
     * Validates Seg lvl for correctness.
     *
     * <p>Validation rules:</p>
     * <ul>
     *   <li>Seg lvl must be a positive integer</li>
     *   <li>Cannot jump levels (e.g., 1 -> 3 without a 2 in between)</li>
     *   <li>Can decrease by any amount (e.g., 5 -> 1 is valid)</li>
     * </ul>
     *
     * @param segLevel the current segment level
     * @param previousLevel the previous segment level
     * @param rowIndex the 1-based row index
     * @param fieldName the field name for error reporting
     * @throws ParseException if validation fails
     */
    private void validateSegLevel(int segLevel, int previousLevel, int rowIndex, String fieldName) {
        if (segLevel <= 0) {
            throw new ParseException("Invalid Seg lvl '" + segLevel + "'. Must be a positive integer.")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }

        // Allow returning to any lower level, but prevent skipping levels upward
        if (segLevel > previousLevel + 1 && previousLevel > 0) {
            throw new ParseException(
                "Seg lvl jump from " + previousLevel + " to " + segLevel + ". Missing intermediate level.")
                .withContext(sheetName, rowIndex)
                .withField(fieldName);
        }
    }

    /**
     * Creates a basic FieldNode from an Excel row.
     *
     * <p>This method extracts only the raw field metadata directly from Excel
     * columns. Additional processing (camelCase naming, object/array detection,
     * groupId/occurrenceCount extraction) is handled by subsequent tasks.</p>
     *
     * @param row the Excel row to parse
     * @param rowIndex the 1-based row index for error reporting
     * @return a FieldNode with basic metadata, or null if row should be skipped
     * @throws ParseException if required fields are missing or malformed
     */
    private FieldNode createBasicFieldNode(Row row, int rowIndex) {
        String fieldName = getCellValue(row, ColumnNames.FIELD_NAME);
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return null;  // Skip empty rows
        }

        int segLevel = parseSegLevel(getCellValue(row, ColumnNames.SEG_LVL), rowIndex, fieldName);
        String description = getCellValue(row, ColumnNames.DESCRIPTION);
        String length = getCellValue(row, ColumnNames.LENGTH);
        String dataType = getCellValue(row, ColumnNames.MESSAGING_DATATYPE);
        String optionality = getCellValue(row, ColumnNames.OPTIONALITY);

        // Create source metadata for traceability
        SourceMetadata source = new SourceMetadata();
        source.setSheetName(sheetName);
        source.setRowIndex(rowIndex);

        return FieldNode.builder()
            .originalName(fieldName.trim())
            .segLevel(segLevel)
            .length(parseLength(length))
            .dataType(dataType != null ? dataType.trim() : null)
            .optionality(optionality != null ? optionality.trim() : null)
            // Note: description is preserved in source for T-105 to extract groupId/occurrenceCount
            .source(source)
            .build();
    }

    /**
     * Parses the Seg lvl value from a cell.
     *
     * @param value the cell value as a string
     * @param rowIndex the 1-based row index for error reporting
     * @param fieldName the field name for error reporting
     * @return the parsed segment level
     * @throws ParseException if value is empty or not a valid integer
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
     * Parses the Length value from a cell.
     *
     * <p>Returns null if the value is empty or cannot be parsed as an integer.
     * This is expected for object/array definitions where length is not applicable.</p>
     *
     * @param value the cell value as a string
     * @return the parsed length, or null if not a valid integer
     */
    private Integer parseLength(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;  // Object definitions have empty Length
        }
    }

    /**
     * Extracts the string value from an Excel cell.
     *
     * <p>Handles both STRING and NUMERIC cell types. Numeric values are
     * converted to strings, with integers formatted without decimal points.</p>
     *
     * @param row the row containing the cell
     * @param columnName the name of the column to read
     * @return the cell value as a string, or null if cell is empty or missing
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
     * Checks if a row is empty (should be skipped).
     *
     * <p>A row is considered empty if the Field Name column is null or blank.</p>
     *
     * @param row the row to check
     * @return true if the row should be skipped
     */
    private boolean isEmptyRow(Row row) {
        String fieldName = getCellValue(row, ColumnNames.FIELD_NAME);
        return fieldName == null || fieldName.trim().isEmpty();
    }
}
