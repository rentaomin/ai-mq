package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.parser.OccurrenceCountParser;

/**
 * Maps FieldNode objects to XML field attributes for Spring XML bean generation.
 *
 * <p>This mapper applies the business rules defined in the architecture document
 * section 6.1.4 to determine the appropriate XML field type and attributes based on
 * the field characteristics from the intermediate JSON tree.</p>
 *
 * <p>Mapping rules:</p>
 * <ul>
 *   <li>Transitory groupId fields - DataField with defaultValue</li>
 *   <li>Transitory occurrenceCount fields - DataField with counterFieldConverter</li>
 *   <li>Array fields - RepeatingField with fixedCount</li>
 *   <li>Object fields - CompositeField with forType</li>
 *   <li>Simple fields - DataField with type-specific converter</li>
 * </ul>
 */
public class XmlTypeMapper {

    private final Config config;
    private final ConverterMapper converterMapper;
    private final OccurrenceCountParser occurrenceParser;

    /**
     * Constructs an XmlTypeMapper with the given configuration.
     *
     * @param config the configuration containing XML generation settings
     */
    public XmlTypeMapper(Config config) {
        this.config = config;
        this.converterMapper = new ConverterMapper();
        this.occurrenceParser = new OccurrenceCountParser();
    }

    /**
     * Maps a FieldNode to its XML field attributes.
     *
     * <p>This method applies the mapping rules in priority order:
     * transitory fields first, then arrays, objects, and finally simple data fields.</p>
     *
     * @param node the field node from the intermediate JSON tree
     * @return the XML field attributes for this node
     */
    public XmlFieldAttributes map(FieldNode node) {
        // 1. transitory groupId field
        if (node.isTransitory() && node.getGroupId() != null) {
            return mapGroupIdField(node);
        }

        // 2. transitory occurrenceCount field
        if (node.isTransitory() && node.getOccurrenceCount() != null) {
            return mapOccurrenceCountField(node);
        }

        // 3. array field
        if (node.isArray()) {
            return mapRepeatingField(node);
        }

        // 4. object field
        if (node.isObject()) {
            return mapCompositeField(node);
        }

        // 5. simple data field
        return mapDataField(node);
    }

    /**
     * Maps a transitory groupId field.
     *
     * <p>GroupId fields are transitory markers that contain the group identifier
     * and should be included in the XML output with a fixed default value.</p>
     *
     * @param node the field node
     * @return DataField attributes with transitory=true and defaultValue=groupId
     */
    private XmlFieldAttributes mapGroupIdField(FieldNode node) {
        return new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name(node.getCamelCaseName())
            .length(node.getLength() != null ? node.getLength() : 10)
            .fixedLength(true)
            .transitory(true)
            .defaultValue(node.getGroupId())
            .converter(ConverterMapper.STRING_CONVERTER)
            .fieldType(XmlFieldType.DATA_FIELD.getValue());
    }

    /**
     * Maps a transitory occurrenceCount field.
     *
     * <p>OccurrenceCount fields indicate how many times a repeating structure
     * appears and use the counterFieldConverter with numeric formatting.</p>
     *
     * @param node the field node
     * @return DataField attributes with transitory=true and counter converter
     */
    private XmlFieldAttributes mapOccurrenceCountField(FieldNode node) {
        int count = occurrenceParser.calculateFixedCount(node.getOccurrenceCount());
        return new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name(node.getCamelCaseName())
            .length(node.getLength() != null ? node.getLength() : 4)
            .fixedLength(true)
            .transitory(true)
            .defaultValue(String.valueOf(count))
            .pad("0")
            .alignRight(true)
            .converter(ConverterMapper.COUNTER_CONVERTER)
            .fieldType(XmlFieldType.DATA_FIELD.getValue());
    }

    /**
     * Maps an object field (non-array composite structure).
     *
     * @param node the field node
     * @return CompositeField attributes with forType pointing to the Java class
     */
    private XmlFieldAttributes mapCompositeField(FieldNode node) {
        return new XmlFieldAttributes(XmlFieldType.COMPOSITE_FIELD)
            .name(node.getCamelCaseName())
            .forType(buildForType(node.getClassName()))
            .fieldType(XmlFieldType.COMPOSITE_FIELD.getValue());
    }

    /**
     * Maps an array field (repeating structure).
     *
     * @param node the field node
     * @return RepeatingField attributes with fixedCount and forType
     */
    private XmlFieldAttributes mapRepeatingField(FieldNode node) {
        int fixedCount = occurrenceParser.calculateFixedCount(node.getOccurrenceCount());
        return new XmlFieldAttributes(XmlFieldType.REPEATING_FIELD)
            .name(node.getCamelCaseName())
            .fixedCount(fixedCount)
            .forType(buildForType(node.getClassName()))
            .fieldType(XmlFieldType.REPEATING_FIELD.getValue());
    }

    /**
     * Maps a simple data field.
     *
     * <p>Data fields receive type-specific formatting based on their data type:
     * numeric fields get right-aligned zero-padding, string fields get space padding.</p>
     *
     * @param node the field node
     * @return DataField attributes with appropriate converter and formatting
     */
    private XmlFieldAttributes mapDataField(FieldNode node) {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name(node.getCamelCaseName())
            .length(node.getLength());

        String dataType = node.getDataType();

        if (node.getDefaultValue() != null) {
            attrs.defaultValue(node.getDefaultValue());
        }

        if (isNumericType(dataType)) {
            attrs.pad("0").alignRight(true);
        } else {
            attrs.nullPad(" ");
        }

        attrs.converter(converterMapper.getConverter(dataType));

        String forType = converterMapper.getForType(dataType);
        if (forType != null) {
            attrs.forType(forType);
        } else {
            attrs.forType(getDefaultJavaType(dataType));
        }

        return attrs.fieldType(XmlFieldType.DATA_FIELD.getValue());
    }

    private String getDefaultJavaType(String dataType) {
        if (dataType == null) return "java.lang.String";
        String normalized = dataType.trim().toLowerCase();
        switch (normalized) {
            case "string":
            case "an":
            case "a/n":
            case "a":
            case "date":
            case "number":
            case "n":
            case "unsigned integer":
                return "java.lang.String";
            default:
                return "java.lang.String";
        }
    }

    /**
     * Builds the fully qualified forType attribute.
     *
     * <p>Format: {groupId}.{artifactId}.{className}</p>
     *
     * @param className the class name from the field node
     * @return the fully qualified class name
     */
    private String buildForType(String className) {
        String groupId = config.getXml().getProject().getGroupId();
        String artifactId = config.getXml().getProject().getArtifactId();
        return groupId + "." + artifactId + "." + className;
    }

    /**
     * Checks if the data type is numeric.
     *
     * @param dataType the data type from the specification
     * @return true if the type is numeric (requires zero-padding and right alignment)
     */
    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String normalized = dataType.trim().toLowerCase();
        return "number".equals(normalized) ||
               "n".equals(normalized) ||
               "unsigned integer".equals(normalized);
    }
}
