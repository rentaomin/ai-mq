package com.rtm.mq.tool.parser;

/**
 * Column name constants for Excel sheet parsing.
 *
 * <p>These constants represent the normalized column names expected in
 * the MQ message specification Excel sheets.</p>
 */
public final class ColumnNames {

    private ColumnNames() {
        // Utility class - prevent instantiation
    }

    // Required columns
    /** Segment level column - defines hierarchy depth */
    public static final String SEG_LVL = "Seg lvl";

    /** Field name column - the identifier for the field */
    public static final String FIELD_NAME = "FieldName";

    /** Description column - human-readable description */
    public static final String DESCRIPTION = "Description";

    /** Length column - field length specification (e.g., "1-20", "N/A") */
    public static final String LENGTH = "Length";

    /** Messaging datatype column - data type of the field */
    public static final String MESSAGING_DATATYPE = "Messaging Datatype";

    // Optional columns
    /** Optionality column - O for optional, M for mandatory */
    public static final String OPTIONALITY = "Opt (O/M)";

    /** Null allowed column - Y/N indicator */
    public static final String NULL_YN = "Null (Y/N)";

    /** NLS (National Language Support) column - Y/N indicator */
    public static final String NLS_YN = "NLS (Y/N)";

    /** Sample values column - example values for the field */
    public static final String SAMPLE_VALUES = "Sample Value(s)";

    /** Remarks column - additional notes */
    public static final String REMARKS = "Remarks";

    /** GMR Physical Name column - physical database name */
    public static final String GMR_PHYSICAL_NAME = "GMR Physical Name";

    /** Test value column - value used for testing */
    public static final String TEST_VALUE = "Test Value";
    
    public static final String HARD_CODE_VALUE_FOR_MNL = "Hard code Value for MNL";
}
