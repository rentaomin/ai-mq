package com.rtm.mq.tool.model;

/**
 * Source metadata for audit traceability.
 *
 * <p>This class captures the original location of a field in the Excel specification,
 * allowing for traceability between the parsed model and source documents.</p>
 *
 * <p>The metadata includes:</p>
 * <ul>
 *   <li>Sheet name - identifies which worksheet contained the field</li>
 *   <li>Row index - the 1-based row number in the source sheet</li>
 * </ul>
 *
 * @see FieldNode
 */
public class SourceMetadata {

    /**
     * The name of the Excel worksheet containing this field.
     */
    private String sheetName;

    /**
     * The 1-based row index in the source worksheet.
     */
    private int rowIndex;

    /**
     * The byte offset for fixed-format messages (e.g., ISM v2.0 FIX).
     * This is populated when the field is parsed from a fixed-format specification.
     */
    private Integer byteOffset;

    /**
     * Default constructor.
     */
    public SourceMetadata() {
    }

    /**
     * Constructs a SourceMetadata with the specified sheet name and row index.
     *
     * @param sheetName the name of the source worksheet
     * @param rowIndex the 1-based row index in the worksheet
     */
    public SourceMetadata(String sheetName, int rowIndex) {
        this.sheetName = sheetName;
        this.rowIndex = rowIndex;
    }

    /**
     * Gets the name of the Excel worksheet.
     *
     * @return the sheet name
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * Sets the name of the Excel worksheet.
     *
     * @param sheetName the sheet name to set
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    /**
     * Gets the 1-based row index in the source worksheet.
     *
     * @return the row index
     */
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * Sets the 1-based row index in the source worksheet.
     *
     * @param rowIndex the row index to set
     */
    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    /**
     * Gets the byte offset for fixed-format messages.
     *
     * @return the byte offset, or null if not applicable
     */
    public Integer getByteOffset() {
        return byteOffset;
    }

    /**
     * Sets the byte offset for fixed-format messages.
     *
     * @param byteOffset the byte offset to set
     */
    public void setByteOffset(Integer byteOffset) {
        this.byteOffset = byteOffset;
    }
}
