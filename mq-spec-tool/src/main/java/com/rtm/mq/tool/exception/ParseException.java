package com.rtm.mq.tool.exception;

/**
 * Exception thrown when Excel parsing fails.
 *
 * <p>This exception provides detailed context information including:</p>
 * <ul>
 *   <li>Sheet name where the error occurred</li>
 *   <li>Row index (1-based for user readability)</li>
 *   <li>Field name involved in the error</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new ParseException("Invalid Seg lvl value '0'")
 *     .withContext("Request", 15);
 * // Output: Invalid Seg lvl value '0' in sheet 'Request' at row 15
 *
 * throw new ParseException("Duplicate field name")
 *     .withContext("Request", 18)
 *     .withField("domicleBranche");
 * // Output: Duplicate field name in sheet 'Request' at row 18 (field: 'domicleBranche')
 * }</pre>
 */
public class ParseException extends MqToolException {

    private String sheetName;
    private Integer rowIndex;
    private String fieldName;

    /**
     * Creates a new ParseException with the specified message.
     *
     * @param message the error message
     */
    public ParseException(String message) {
        super(message, ExitCodes.PARSE_ERROR);
    }

    /**
     * Creates a new ParseException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause, ExitCodes.PARSE_ERROR);
    }

    /**
     * Adds sheet and row context information to this exception.
     *
     * @param sheetName the name of the Excel sheet
     * @param rowIndex the row number (1-based)
     * @return this exception for method chaining
     */
    public ParseException withContext(String sheetName, int rowIndex) {
        this.sheetName = sheetName;
        this.rowIndex = rowIndex;
        return this;
    }

    /**
     * Adds field name context to this exception.
     *
     * @param fieldName the field name involved in the error
     * @return this exception for method chaining
     */
    public ParseException withField(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    /**
     * Returns the full error message including context information.
     *
     * @return the formatted error message
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (sheetName != null) {
            sb.append(" in sheet '").append(sheetName).append("'");
        }
        if (rowIndex != null) {
            sb.append(" at row ").append(rowIndex);
        }
        if (fieldName != null) {
            sb.append(" (field: '").append(fieldName).append("')");
        }
        return sb.toString();
    }

    /**
     * Gets the sheet name where the error occurred.
     *
     * @return the sheet name, or null if not set
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * Gets the row index where the error occurred.
     *
     * @return the row index (1-based), or null if not set
     */
    public Integer getRowIndex() {
        return rowIndex;
    }

    /**
     * Gets the field name involved in the error.
     *
     * @return the field name, or null if not set
     */
    public String getFieldName() {
        return fieldName;
    }
}
