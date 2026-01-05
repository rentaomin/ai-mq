package com.rtm.mq.tool.generator.xml;

/**
 * Maps data types from Excel specifications to XML converter bean names.
 *
 * <p>This mapper provides consistent converter selection based on the
 * data type specified in the Excel specification. Converters handle the
 * serialization and deserialization of field values in the messaging system.</p>
 *
 * <p>Standard converters include:</p>
 * <ul>
 *   <li>stringFieldConverter - Default for string/alphanumeric fields</li>
 *   <li>counterFieldConverter - For occurrence count fields</li>
 *   <li>OHcurrencyamountFieldConverter - For currency/amount fields</li>
 *   <li>nlsStringFieldConverter - For NLS (National Language Support) strings</li>
 *   <li>OHunsignedlongFieldConverter - For unsigned long integers</li>
 *   <li>OHunsignedintFieldConverter - For unsigned integers</li>
 * </ul>
 */
public class ConverterMapper {

    public static final String STRING_CONVERTER = "stringFieldConverter";
    public static final String COUNTER_CONVERTER = "counterFieldConverter";
    public static final String CURRENCY_CONVERTER = "OHcurrencyamountFieldConverter";
    public static final String NLS_CONVERTER = "nlsStringFieldConverter";
    public static final String UNSIGNED_LONG_CONVERTER = "OHunsignedlongFieldConverter";
    public static final String UNSIGNED_INT_CONVERTER = "OHunsignedintFieldConverter";

    /**
     * Gets the appropriate converter for the given data type.
     *
     * <p>Data type matching is case-insensitive and supports various
     * abbreviations commonly found in Excel specifications.</p>
     *
     * @param dataType the data type from the Excel specification
     * @return the converter bean name
     */
    public String getConverter(String dataType) {
        if (dataType == null) return STRING_CONVERTER;

        String normalized = dataType.trim().toLowerCase();

        switch (normalized) {
            case "string":
            case "an":
                return STRING_CONVERTER;

            case "number":
            case "n":
            case "unsigned integer":
                return STRING_CONVERTER;  // Keep as string for numeric types

            case "amount":
            case "currency":
                return CURRENCY_CONVERTER;

            default:
                return STRING_CONVERTER;
        }
    }

    /**
     * Gets the forType attribute value for specific data types.
     *
     * <p>Some data types require a Java type mapping. For example,
     * amount/currency fields map to java.math.BigDecimal.</p>
     *
     * @param dataType the data type from the Excel specification
     * @return the fully qualified Java type, or null if not applicable
     */
    public String getForType(String dataType) {
        if (dataType == null) return null;

        String normalized = dataType.trim().toLowerCase();

        if ("amount".equals(normalized) || "currency".equals(normalized)) {
            return "java.math.BigDecimal";
        }

        return null;
    }
}
