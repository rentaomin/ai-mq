package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

/**
 * Detects and processes object/array definitions in Excel specifications.
 *
 * <p>This detector identifies three types of special fields:</p>
 * <ol>
 *   <li><b>Object definitions</b>: Fields in "FieldName:ClassName" format that define
 *       nested structures. Identified by containing a colon and having empty Length
 *       and Datatype columns.</li>
 *   <li><b>groupId fields</b>: Meta-fields that provide group identifiers for related
 *       structures. The actual group ID value is in the Description column.</li>
 *   <li><b>occurrenceCount fields</b>: Meta-fields that specify how many times a
 *       structure can repeat. The count pattern (e.g., "0..9") is in Description.</li>
 * </ol>
 *
 * <p>Both groupId and occurrenceCount fields are marked as transitory, meaning they
 * provide metadata but should not appear in final generated outputs (Java beans,
 * OpenAPI YAML).</p>
 *
 * <p>Note: The Excel specification contains a typo where "occurrenceCount" is spelled
 * as "occurenceCount" (missing one 'r'). This detector handles both spellings.</p>
 */
public class ObjectArrayDetector {

    private final OccurrenceCountParser occurrenceParser = new OccurrenceCountParser();
    private final Map<String, Integer> columnMap;

    /**
     * Creates a new ObjectArrayDetector with the specified column mapping.
     *
     * @param columnMap map of column names to their indices
     */
    public ObjectArrayDetector(Map<String, Integer> columnMap) {
        this.columnMap = columnMap;
    }

    /**
     * Detects and enhances a FieldNode based on its type.
     *
     * <p>This method examines the field and determines if it is:</p>
     * <ul>
     *   <li>A groupId field - enhances with isTransitory=true and groupId value</li>
     *   <li>An occurrenceCount field - enhances with isTransitory=true and occurrenceCount value</li>
     *   <li>An object definition - enhances with isObject=true and className</li>
     *   <li>A normal field - returns unchanged</li>
     * </ul>
     *
     * @param node the base FieldNode to detect/enhance
     * @param row the Excel row containing the field data
     * @return the enhanced FieldNode (may be the same instance or a new one)
     */
    public FieldNode detect(FieldNode node, Row row) {
        String fieldName = node.getOriginalName();
        String description = getCellValue(row, ColumnNames.DESCRIPTION);
        String length = getCellValue(row, ColumnNames.LENGTH);
        String dataType = getCellValue(row, ColumnNames.MESSAGING_DATATYPE);

        // Check for groupId field
        if (isGroupIdField(fieldName)) {
            return enhanceAsGroupId(node, description);
        }

        // Check for occurrenceCount field
        if (isOccurrenceCountField(fieldName)) {
            return enhanceAsOccurrenceCount(node, description);
        }

        // Check for object definition (FieldName:ClassName format)
        if (isObjectDefinition(fieldName, length, dataType)) {
            return enhanceAsObjectOrArray(node, fieldName);
        }

        // Normal field - return unchanged
        return node;
    }

    /**
     * Checks if the field name represents a groupId field.
     *
     * @param fieldName the field name to check
     * @return true if this is a groupId field
     */
    public boolean isGroupIdField(String fieldName) {
        return "groupid".equalsIgnoreCase(fieldName);
    }

    /**
     * Checks if the field name represents an occurrenceCount field.
     *
     * <p>Note: Handles both correct spelling "occurrenceCount" and the
     * Excel typo "occurenceCount" (missing one 'r').</p>
     *
     * @param fieldName the field name to check
     * @return true if this is an occurrenceCount field
     */
    public boolean isOccurrenceCountField(String fieldName) {
        return "occurenceCount".equalsIgnoreCase(fieldName) ||
               "occurrenceCount".equalsIgnoreCase(fieldName);
    }

    /**
     * Checks if the field represents an object definition.
     *
     * <p>Object definitions are identified by:</p>
     * <ul>
     *   <li>Containing a colon in the field name (FieldName:ClassName format)</li>
     *   <li>Having empty or null Length column</li>
     *   <li>Having empty or null Datatype column</li>
     * </ul>
     *
     * @param fieldName the field name to check
     * @param length the Length column value
     * @param dataType the Datatype column value
     * @return true if this is an object definition
     */
    public boolean isObjectDefinition(String fieldName, String length, String dataType) {
        return fieldName != null &&
               fieldName.contains(":") &&
               (length == null || length.trim().isEmpty()) &&
               (dataType == null || dataType.trim().isEmpty());
    }

    /**
     * Parses an object definition from a field name.
     *
     * <p>Extracts the field name and class name from "FieldName:ClassName" format.</p>
     *
     * @param fieldName the field name containing the object definition
     * @return the parsed ObjectDefinition
     * @throws ParseException if the format is invalid
     */
    public ObjectDefinition parseObjectDefinition(String fieldName) {
        if (fieldName == null || !fieldName.contains(":")) {
            throw new ParseException("Invalid object definition format: " + fieldName);
        }

        String[] parts = fieldName.split(":", 2);
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            throw new ParseException("Invalid object definition format: " + fieldName +
                ". Expected format: 'fieldName:ClassName'");
        }

        return new ObjectDefinition(parts[0].trim(), parts[1].trim());
    }

    /**
     * Updates a container node's type based on its occurrenceCount child.
     *
     * <p>After parsing children, this method searches for an occurrenceCount
     * field among the children. If found and its value indicates an array
     * (max > 1), the container is updated to be an array type.</p>
     *
     * <p>This should be called after all children of an object node have
     * been parsed.</p>
     *
     * @param containerNode the container node to update
     * @return a new FieldNode with updated array/object flags, or the same node if no change
     */
    public FieldNode updateContainerType(FieldNode containerNode) {
        if (containerNode.getChildren() == null) {
            return containerNode;
        }

        // Search for occurrenceCount among children
        for (FieldNode child : containerNode.getChildren()) {
            if (isOccurrenceCountField(child.getOriginalName())) {
                String occCount = child.getOccurrenceCount();
                if (occCount != null) {
                    ArrayInfo info = occurrenceParser.parse(occCount);
                    if (info != null && info.isArray()) {
                        // Rebuild as array type
                        return FieldNode.builder()
                            .originalName(containerNode.getOriginalName())
                            .camelCaseName(containerNode.getCamelCaseName())
                            .className(containerNode.getClassName())
                            .segLevel(containerNode.getSegLevel())
                            .length(containerNode.getLength())
                            .dataType(containerNode.getDataType())
                            .optionality(containerNode.getOptionality())
                            .defaultValue(containerNode.getDefaultValue())
                            .hardCodeValue(containerNode.getHardCodeValue())
                            .groupId(containerNode.getGroupId())
                            .occurrenceCount(occCount)
                            .isArray(true)
                            .isObject(false)
                            .isTransitory(containerNode.isTransitory())
                            .children(containerNode.getChildren())
                            .source(containerNode.getSource())
                            .build();
                    }
                }
                break;
            }
        }

        return containerNode;
    }

    /**
     * Enhances a field node as a groupId field.
     *
     * @param node the base node
     * @param description the Description column value containing the group ID
     * @return enhanced FieldNode with groupId set and isTransitory=true
     */
    private FieldNode enhanceAsGroupId(FieldNode node, String description) {
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(node.getOriginalName().toLowerCase())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .optionality(node.getOptionality())
            .defaultValue(node.getDefaultValue())
            .hardCodeValue(node.getHardCodeValue())
            .groupId(description != null ? description.trim() : null)
            .isTransitory(true)
            .source(node.getSource())
            .build();
    }

    /**
     * Enhances a field node as an occurrenceCount field.
     *
     * @param node the base node
     * @param description the Description column value containing the occurrence count
     * @return enhanced FieldNode with occurrenceCount set and isTransitory=true
     */
    private FieldNode enhanceAsOccurrenceCount(FieldNode node, String description) {
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(node.getOriginalName().toLowerCase())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .optionality(node.getOptionality())
            .defaultValue(node.getDefaultValue())
            .hardCodeValue(node.getHardCodeValue())
            .occurrenceCount(description != null ? description.trim() : null)
            .isTransitory(true)
            .source(node.getSource())
            .build();
    }

    /**
     * Enhances a field node as an object or array definition.
     *
     * @param node the base node
     * @param fieldName the field name containing the object definition
     * @return enhanced FieldNode with isObject=true and className set
     */
    private FieldNode enhanceAsObjectOrArray(FieldNode node, String fieldName) {
        ObjectDefinition objDef = parseObjectDefinition(fieldName);

        return FieldNode.builder()
            .originalName(fieldName)
            .camelCaseName(null)  // Will be set by T-106 NamingNormalizer
            .className(objDef.getClassName())
            .segLevel(node.getSegLevel())
            .optionality(node.getOptionality())
            .defaultValue(node.getDefaultValue())
            .hardCodeValue(node.getHardCodeValue())
            .isObject(true)  // Default to object; updateContainerType may change to array
            .isArray(false)
            .children(node.getChildren())
            .source(node.getSource())
            .build();
    }

    /**
     * Gets the cell value from a row by column name.
     *
     * @param row the Excel row
     * @param columnName the column name
     * @return the cell value as a string, or null if not found
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

        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cellType == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }
}
