package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.exception.OutputException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enum helper generator for creating Java enum classes from field constraints.
 *
 * <p>This generator extracts enum type fields from the intermediate JSON tree
 * (represented as FieldGroup/FieldNode) and generates independent Java enum classes
 * with helper methods for code conversion and validation.</p>
 *
 * <p>Generated enum classes include:</p>
 * <ul>
 *   <li>Enum values with code and description</li>
 *   <li>{@code fromCode(String)} - Convert code to enum value</li>
 *   <li>{@code isValid(String)} - Validate if a code is recognized</li>
 *   <li>{@code getCode()} - Get the code value</li>
 *   <li>{@code getDescription()} - Get the description</li>
 * </ul>
 *
 * <p>Enum value naming rules:</p>
 * <ul>
 *   <li>Pure digits (e.g., "01") - VALUE_01</li>
 *   <li>Pure uppercase letters (e.g., "Y") - Y</li>
 *   <li>Other values - Converted to uppercase with special chars replaced by underscore</li>
 * </ul>
 *
 * @see JavaTypeMapper#isEnumType(FieldNode)
 * @see JavaBeanGenerator
 */
public class EnumHelperGenerator {

    private final Config config;
    private final JavaTypeMapper typeMapper;

    /**
     * Map of generated enum class names to their Java source content.
     * Uses LinkedHashMap to preserve generation order for deterministic output.
     */
    private final Map<String, String> generatedContents = new LinkedHashMap<>();

    /**
     * Set of processed enum class names to prevent duplicate generation.
     */
    private final Set<String> processedEnumNames = new HashSet<>();

    /**
     * Constructs an EnumHelperGenerator with the given configuration.
     *
     * @param config the configuration containing output and Java settings
     */
    public EnumHelperGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    /**
     * Generates all enum classes from the given field group.
     *
     * <p>This method recursively traverses all fields in the group, identifies
     * enum type fields using {@link JavaTypeMapper#isEnumType(FieldNode)},
     * and generates corresponding Java enum classes.</p>
     *
     * @param fieldGroup the field group to process
     * @return a list of generated enum class names
     * @throws GenerationException if enum generation fails
     */
    public List<String> generateEnumClasses(FieldGroup fieldGroup) throws GenerationException {
        List<String> generatedEnumNames = new ArrayList<>();

        if (fieldGroup == null || fieldGroup.getFields() == null || fieldGroup.getFields().isEmpty()) {
            return generatedEnumNames;
        }

        // Recursively traverse all fields to collect enum types
        for (FieldNode field : fieldGroup.getFields()) {
            collectEnumClasses(field, generatedEnumNames);
        }

        return generatedEnumNames;
    }

    /**
     * Recursively collects and generates enum classes from a field and its children.
     *
     * @param field the field node to process
     * @param generatedEnumNames the list to add generated enum names to
     * @throws GenerationException if enum generation fails
     */
    private void collectEnumClasses(FieldNode field, List<String> generatedEnumNames)
            throws GenerationException {

        // Skip transitory fields
        if (field.isTransitory()) {
            return;
        }

        // Check if this field is an enum type
        if (typeMapper.isEnumType(field)) {
            String enumClassName = deriveEnumClassName(field);

            // Avoid duplicate generation
            if (!processedEnumNames.contains(enumClassName)) {
                List<EnumValue> enumValues = extractEnumValues(field);
                generateEnumClass(enumClassName, enumValues, field.getOriginalName());
                generatedEnumNames.add(enumClassName);
                processedEnumNames.add(enumClassName);
            }
        }

        // Recursively process child fields
        if (field.hasChildren()) {
            for (FieldNode child : field.getChildren()) {
                collectEnumClasses(child, generatedEnumNames);
            }
        }
    }

    /**
     * Generates a single Java enum class.
     *
     * @param enumClassName the name of the enum class
     * @param enumValues the list of enum values
     * @param fieldOriginalName the original field name for documentation
     * @throws GenerationException if generation fails
     */
    private void generateEnumClass(String enumClassName,
                                   List<EnumValue> enumValues,
                                   String fieldOriginalName) throws GenerationException {
        StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package ").append(typeMapper.getModelPackage()).append(";\n\n");

        // Class Javadoc
        sb.append("/**\n");
        sb.append(" * Enum for ").append(fieldOriginalName).append("\n");
        sb.append(" *\n");
        sb.append(" * @generated by MQ Tool\n");
        sb.append(" */\n");

        // Enum declaration
        sb.append("public enum ").append(enumClassName).append(" {\n\n");

        // Enum values
        for (int i = 0; i < enumValues.size(); i++) {
            EnumValue enumValue = enumValues.get(i);
            sb.append("    /** ").append(escapeJavadoc(enumValue.getDescription())).append(" */\n");
            sb.append("    ").append(enumValue.getName())
              .append("(\"").append(escapeString(enumValue.getCode())).append("\", \"")
              .append(escapeString(enumValue.getDescription())).append("\")");

            if (i < enumValues.size() - 1) {
                sb.append(",\n\n");
            } else {
                sb.append(";\n\n");
            }
        }

        // Fields
        sb.append("    private final String code;\n");
        sb.append("    private final String description;\n\n");

        // Constructor
        sb.append("    ").append(enumClassName)
          .append("(String code, String description) {\n");
        sb.append("        this.code = code;\n");
        sb.append("        this.description = description;\n");
        sb.append("    }\n\n");

        // getCode method
        sb.append("    public String getCode() {\n");
        sb.append("        return code;\n");
        sb.append("    }\n\n");

        // getDescription method
        sb.append("    public String getDescription() {\n");
        sb.append("        return description;\n");
        sb.append("    }\n\n");

        // fromCode static method
        sb.append("    /**\n");
        sb.append("     * Convert code to enum\n");
        sb.append("     * @param code enum code\n");
        sb.append("     * @return enum value, or null if not found\n");
        sb.append("     */\n");
        sb.append("    public static ").append(enumClassName)
          .append(" fromCode(String code) {\n");
        sb.append("        if (code == null) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        for (").append(enumClassName).append(" value : values()) {\n");
        sb.append("            if (value.code.equals(code)) {\n");
        sb.append("                return value;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");

        // isValid static method
        sb.append("    /**\n");
        sb.append("     * Check if code is valid\n");
        sb.append("     * @param code enum code\n");
        sb.append("     * @return true if valid\n");
        sb.append("     */\n");
        sb.append("    public static boolean isValid(String code) {\n");
        sb.append("        return fromCode(code) != null;\n");
        sb.append("    }\n");

        sb.append("}\n");

        String content = sb.toString();
        generatedContents.put(enumClassName, content);
        writeToFile(enumClassName, content);
    }

    /**
     * Derives the enum class name from a field node.
     *
     * <p>Converts camelCaseName to PascalCase.</p>
     *
     * @param field the field node
     * @return the derived enum class name
     */
    private String deriveEnumClassName(FieldNode field) {
        String camelName = field.getCamelCaseName();
        if (camelName == null || camelName.isEmpty()) {
            // Fallback to original name converted to PascalCase
            return toPascalCase(field.getOriginalName());
        }
        return capitalizeFirst(camelName);
    }

    /**
     * Extracts enum values from the field's enum constraint.
     *
     * @param field the field node with enum constraint
     * @return the list of extracted enum values
     * @throws GenerationException if the enum constraint is invalid or empty
     */
    private List<EnumValue> extractEnumValues(FieldNode field) throws GenerationException {
        String enumConstraint = field.getEnumConstraint();
        if (enumConstraint == null || enumConstraint.isBlank()) {
            throw new GenerationException(
                "Enum constraint is required for enum field: " + field.getOriginalName()
            ).withGenerator("EnumHelperGenerator")
             .withArtifact(field.getOriginalName());
        }

        // Parse enum constraint, format: "01|02|03" or "Y|N"
        String[] codes = enumConstraint.split("\\|");
        List<EnumValue> enumValues = new ArrayList<>();

        for (String code : codes) {
            code = code.trim();
            if (code.isEmpty()) {
                continue;
            }

            String name = deriveEnumValueName(code);
            String description = deriveEnumValueDescription(code, field);

            enumValues.add(new EnumValue(name, code, description));
        }

        if (enumValues.isEmpty()) {
            throw new GenerationException(
                "No valid enum values found for field: " + field.getOriginalName()
            ).withGenerator("EnumHelperGenerator")
             .withArtifact(field.getOriginalName());
        }

        return enumValues;
    }

    /**
     * Derives an enum value name from the code.
     *
     * <p>Naming rules:</p>
     * <ul>
     *   <li>Pure digits: VALUE_ prefix (e.g., "01" becomes VALUE_01)</li>
     *   <li>Pure uppercase letters: use as-is (e.g., "Y" becomes Y)</li>
     *   <li>Other: convert to uppercase, replace special chars with underscore</li>
     * </ul>
     *
     * @param code the enum code value
     * @return a valid Java enum constant name
     */
    private String deriveEnumValueName(String code) {
        if (code.matches("\\d+")) {
            // Pure digits - add VALUE_ prefix
            return "VALUE_" + code;
        } else if (code.matches("[A-Z]+")) {
            // Pure uppercase letters - use directly
            return code;
        } else {
            // Other cases - convert to uppercase and replace special chars
            return code.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        }
    }

    /**
     * Derives an enum value description from the code.
     *
     * <p>Currently returns the code itself as description. Can be extended
     * to read from field metadata or Excel spec remarks column.</p>
     *
     * @param code the enum code value
     * @param field the field node (for accessing metadata)
     * @return the description for the enum value
     */
    private String deriveEnumValueDescription(String code, FieldNode field) {
        // Simple implementation: use code as description
        // Can be extended to read from field.getSource() metadata
        return code;
    }

    /**
     * Writes the generated enum class to a file.
     *
     * @param enumClassName the enum class name
     * @param content the Java source content
     * @throws OutputException if file writing fails
     */
    private void writeToFile(String enumClassName, String content) throws OutputException {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        String outputRoot = config.getOutput() != null ? config.getOutput().getRootDir() : "./output";
        Path outputDir = Path.of(outputRoot, "java", packagePath);
        Path outputFile = outputDir.resolve(enumClassName + ".java");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new OutputException(
                "Failed to write enum class file: " + enumClassName + " - " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Capitalizes the first character of a string.
     *
     * @param str the input string
     * @return the string with first character capitalized
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Converts a string to PascalCase.
     *
     * @param str the input string (may contain underscores or other separators)
     * @return the PascalCase string
     */
    private String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in a string for use in Java string literals.
     *
     * @param str the input string
     * @return the escaped string
     */
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Escapes special characters in text for use in Javadoc comments.
     *
     * @param str the input string
     * @return the escaped string safe for Javadoc
     */
    private String escapeJavadoc(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("@", "&#64;")
                  .replace("*/", "* /");
    }

    /**
     * Gets the generated content for a specific enum class.
     *
     * @param enumClassName the enum class name
     * @return the Java source content, or null if not generated
     */
    public String getGeneratedContent(String enumClassName) {
        return generatedContents.get(enumClassName);
    }

    /**
     * Gets all generated enum class names.
     *
     * @return a list of all generated enum class names in generation order
     */
    public List<String> getAllGeneratedEnumNames() {
        return new ArrayList<>(generatedContents.keySet());
    }

    /**
     * Represents a single enum value with name, code, and description.
     */
    private static class EnumValue {
        private final String name;
        private final String code;
        private final String description;

        /**
         * Constructs an EnumValue.
         *
         * @param name the Java enum constant name
         * @param code the code value
         * @param description the description
         */
        EnumValue(String name, String code, String description) {
            this.name = name;
            this.code = code;
            this.description = description;
        }

        String getName() {
            return name;
        }

        String getCode() {
            return code;
        }

        String getDescription() {
            return description;
        }
    }
}
