package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nested class generator for MQ message specifications.
 *
 * <p>This generator extracts nested object and array element types from a
 * {@link FieldGroup} and generates independent Java class files for each.</p>
 *
 * <p>Generated classes include:</p>
 * <ul>
 *   <li>Package declaration</li>
 *   <li>Import statements (sorted alphabetically)</li>
 *   <li>Class-level Javadoc with @generated marker and spec source traceability</li>
 *   <li>Lombok annotations (if enabled): @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor</li>
 *   <li>Private fields with type comments</li>
 *   <li>Getter/Setter methods (if Lombok disabled)</li>
 *   <li>Builder inner class (if Lombok disabled)</li>
 * </ul>
 *
 * <p>Transitory fields (groupId, occurrenceCount) are excluded from generated beans.</p>
 *
 * <p>Class naming conventions:</p>
 * <ul>
 *   <li>Array element classes: remove Arr/Array/List suffix, add "Array" suffix, PascalCase</li>
 *   <li>Object classes: direct PascalCase conversion from camelCaseName</li>
 * </ul>
 *
 * @see JavaBeanGenerator
 * @see JavaTypeMapper
 * @see FieldGroup
 * @see FieldNode
 */
public class NestedClassGenerator {

    private static final String NEWLINE = "\n";
    private static final String INDENT = "    ";

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final Map<String, String> generatedContents = new LinkedHashMap<>();
    private final Set<String> processedClassNames = new LinkedHashSet<>();

    /**
     * Constructs a NestedClassGenerator with the given configuration.
     *
     * @param config the configuration containing Java generation settings
     */
    public NestedClassGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    /**
     * Generates all nested classes from a FieldGroup.
     *
     * <p>This method traverses the field tree recursively, identifying all
     * fields that represent nested objects or array elements with children,
     * and generates a separate Java class for each.</p>
     *
     * <p>Class name deduplication is performed to ensure each unique nested
     * type is only generated once, even if referenced from multiple locations.</p>
     *
     * @param fieldGroup the field group to process
     * @return a list of generated class names in generation order
     */
    public List<String> generateNestedClasses(FieldGroup fieldGroup) {
        List<String> generatedClassNames = new ArrayList<>();

        if (fieldGroup == null || fieldGroup.getFields() == null || fieldGroup.getFields().isEmpty()) {
            return generatedClassNames;
        }

        // Recursively traverse all fields, collecting nested classes
        for (FieldNode field : fieldGroup.getFields()) {
            collectNestedClasses(field, generatedClassNames);
        }

        return generatedClassNames;
    }

    /**
     * Generates all nested classes from a FieldGroup and writes to disk.
     *
     * <p>This is the main entry point for file-based generation. Generated
     * files are written to the specified output directory under the path:
     * {outputDir}/java/{package-path}/{ClassName}.java</p>
     *
     * @param fieldGroup the field group to process
     * @param outputDir the base output directory
     * @return a list of generated class names in generation order
     */
    public List<String> generateNestedClasses(FieldGroup fieldGroup, Path outputDir) {
        List<String> generatedClassNames = new ArrayList<>();

        if (fieldGroup == null || fieldGroup.getFields() == null || fieldGroup.getFields().isEmpty()) {
            return generatedClassNames;
        }

        // Recursively traverse all fields, collecting nested classes
        for (FieldNode field : fieldGroup.getFields()) {
            collectNestedClassesWithOutput(field, generatedClassNames, outputDir);
        }

        return generatedClassNames;
    }

    /**
     * Recursively collects and generates nested classes from a field.
     *
     * @param field the field to process
     * @param generatedClassNames list to collect generated class names
     */
    private void collectNestedClasses(FieldNode field, List<String> generatedClassNames) {
        // Skip transitory fields
        if (field.isTransitory()) {
            return;
        }

        // Process array types with children
        if (field.isArray() && hasNonEmptyChildren(field)) {
            String arrayClassName = deriveArrayClassName(field);

            if (!processedClassNames.contains(arrayClassName)) {
                String content = generateNestedClass(
                    arrayClassName,
                    field.getChildren(),
                    "Array element class for " + field.getOriginalName()
                );
                generatedContents.put(arrayClassName, content);
                generatedClassNames.add(arrayClassName);
                processedClassNames.add(arrayClassName);

                // Recursively process children
                for (FieldNode child : field.getChildren()) {
                    collectNestedClasses(child, generatedClassNames);
                }
            }
        }
        // Process object types with children
        else if (field.isObject() && hasNonEmptyChildren(field)) {
            String objectClassName = deriveObjectClassName(field);

            if (!processedClassNames.contains(objectClassName)) {
                String content = generateNestedClass(
                    objectClassName,
                    field.getChildren(),
                    "Nested object class for " + field.getOriginalName()
                );
                generatedContents.put(objectClassName, content);
                generatedClassNames.add(objectClassName);
                processedClassNames.add(objectClassName);

                // Recursively process children
                for (FieldNode child : field.getChildren()) {
                    collectNestedClasses(child, generatedClassNames);
                }
            }
        }
        // Handle fields that have children but are not marked as array/object
        // (check children anyway for deep nesting)
        else if (hasNonEmptyChildren(field)) {
            for (FieldNode child : field.getChildren()) {
                collectNestedClasses(child, generatedClassNames);
            }
        }
    }

    /**
     * Recursively collects, generates, and writes nested classes from a field.
     *
     * @param field the field to process
     * @param generatedClassNames list to collect generated class names
     * @param outputDir the base output directory
     */
    private void collectNestedClassesWithOutput(FieldNode field, List<String> generatedClassNames, Path outputDir) {
        // Skip transitory fields
        if (field.isTransitory()) {
            return;
        }

        // Process array types with children
        if (field.isArray() && hasNonEmptyChildren(field)) {
            String arrayClassName = deriveArrayClassName(field);

            if (!processedClassNames.contains(arrayClassName)) {
                String content = generateNestedClass(
                    arrayClassName,
                    field.getChildren(),
                    "Array element class for " + field.getOriginalName()
                );
                generatedContents.put(arrayClassName, content);
                generatedClassNames.add(arrayClassName);
                processedClassNames.add(arrayClassName);
                writeToFile(outputDir, arrayClassName, content);

                // Recursively process children
                for (FieldNode child : field.getChildren()) {
                    collectNestedClassesWithOutput(child, generatedClassNames, outputDir);
                }
            }
        }
        // Process object types with children
        else if (field.isObject() && hasNonEmptyChildren(field)) {
            String objectClassName = deriveObjectClassName(field);

            if (!processedClassNames.contains(objectClassName)) {
                String content = generateNestedClass(
                    objectClassName,
                    field.getChildren(),
                    "Nested object class for " + field.getOriginalName()
                );
                generatedContents.put(objectClassName, content);
                generatedClassNames.add(objectClassName);
                processedClassNames.add(objectClassName);
                writeToFile(outputDir, objectClassName, content);

                // Recursively process children
                for (FieldNode child : field.getChildren()) {
                    collectNestedClassesWithOutput(child, generatedClassNames, outputDir);
                }
            }
        }
        // Handle fields that have children but are not marked as array/object
        else if (hasNonEmptyChildren(field)) {
            for (FieldNode child : field.getChildren()) {
                collectNestedClassesWithOutput(child, generatedClassNames, outputDir);
            }
        }
    }

    /**
     * Checks if a field has non-empty children (after filtering transitory fields).
     *
     * @param field the field to check
     * @return true if the field has at least one non-transitory child
     */
    private boolean hasNonEmptyChildren(FieldNode field) {
        if (field.getChildren() == null || field.getChildren().isEmpty()) {
            return false;
        }
        // Check if any child is non-transitory
        for (FieldNode child : field.getChildren()) {
            if (!child.isTransitory()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a single nested class Java source code.
     *
     * @param className the class name to generate
     * @param fields the fields to include in the class
     * @param description the class description for Javadoc
     * @return the generated Java source code
     */
    private String generateNestedClass(String className, List<FieldNode> fields, String description) {
        StringBuilder sb = new StringBuilder();

        // Filter out transitory fields (groupId, occurrenceCount)
        List<FieldNode> filteredFields = filterTransitoryFields(fields);

        // Package declaration
        sb.append("package ").append(typeMapper.getModelPackage()).append(";").append(NEWLINE).append(NEWLINE);

        // Collect and write imports
        Set<String> imports = collectImports(filteredFields);
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";").append(NEWLINE);
        }
        if (!imports.isEmpty()) {
            sb.append(NEWLINE);
        }

        // Class Javadoc
        sb.append("/**").append(NEWLINE);
        sb.append(" * ").append(description).append(NEWLINE);
        sb.append(" *").append(NEWLINE);
        sb.append(" * @generated by MQ Tool").append(NEWLINE);
        sb.append(" */").append(NEWLINE);

        // Lombok annotations (if enabled)
        if (isUseLombok()) {
            sb.append("@Data").append(NEWLINE);
            sb.append("@Builder").append(NEWLINE);
            sb.append("@NoArgsConstructor").append(NEWLINE);
            sb.append("@AllArgsConstructor").append(NEWLINE);
        }

        // Class declaration
        sb.append("public class ").append(className).append(" {").append(NEWLINE).append(NEWLINE);

        // Field definitions
        for (FieldNode field : filteredFields) {
            generateFieldDefinition(sb, field);
        }

        // If not using Lombok, generate constructors, getters/setters, and Builder
        if (!isUseLombok()) {
            // No-arg constructor
            sb.append(NEWLINE);
            sb.append(INDENT).append("public ").append(className).append("() {").append(NEWLINE);
            sb.append(INDENT).append("}").append(NEWLINE);

            // Getters and Setters
            for (FieldNode field : filteredFields) {
                generateGetterSetter(sb, field);
            }

            // Builder class
            generateBuilderClass(sb, className, filteredFields);
        }

        // Close class
        sb.append("}").append(NEWLINE);

        return sb.toString();
    }

    /**
     * Derives the class name for an array element type.
     *
     * <p>Naming rules:</p>
     * <ul>
     *   <li>Start with the camelCaseName</li>
     *   <li>Remove trailing Arr/Array/List suffix (case-insensitive)</li>
     *   <li>Add "Array" suffix</li>
     *   <li>Convert to PascalCase</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>cbaCardArr -> CbaCardArray</li>
     *   <li>itemList -> ItemArray</li>
     *   <li>paymentDetails -> PaymentDetailsArray</li>
     * </ul>
     *
     * @param field the array field
     * @return the derived class name
     */
    String deriveArrayClassName(FieldNode field) {
        // Prefer className if explicitly set
        if (field.getClassName() != null && !field.getClassName().isEmpty()) {
            return field.getClassName();
        }

        String camelName = field.getCamelCaseName();
        if (camelName == null || camelName.isEmpty()) {
            camelName = field.getOriginalName();
        }

        // Remove trailing Arr/Array/List suffix (case-insensitive)
        String baseName = camelName.replaceAll("(?i)(Arr|Array|List)$", "");

        // Add "Array" suffix and convert to PascalCase
        return capitalizeFirst(baseName) + "Array";
    }

    /**
     * Derives the class name for an object type.
     *
     * <p>Naming rules:</p>
     * <ul>
     *   <li>Start with the camelCaseName</li>
     *   <li>Convert to PascalCase</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>createApplication -> CreateApplication</li>
     *   <li>paymentDetails -> PaymentDetails</li>
     * </ul>
     *
     * @param field the object field
     * @return the derived class name
     */
    String deriveObjectClassName(FieldNode field) {
        // Prefer className if explicitly set
        if (field.getClassName() != null && !field.getClassName().isEmpty()) {
            return field.getClassName();
        }

        String camelName = field.getCamelCaseName();
        if (camelName == null || camelName.isEmpty()) {
            camelName = field.getOriginalName();
        }

        return capitalizeFirst(camelName);
    }

    /**
     * Filters out transitory fields that should not appear in Java beans.
     *
     * <p>Transitory fields include groupId and occurrenceCount markers
     * that are used internally but not exposed in the generated beans.</p>
     *
     * @param fields the list of fields to filter
     * @return a filtered list with transitory fields removed
     */
    private List<FieldNode> filterTransitoryFields(List<FieldNode> fields) {
        List<FieldNode> filtered = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory()) {
                filtered.add(field);
            }
        }
        return filtered;
    }

    /**
     * Collects all import statements required for the given fields.
     *
     * @param fields the list of fields
     * @return a sorted set of fully qualified import names
     */
    private Set<String> collectImports(List<FieldNode> fields) {
        Set<String> imports = new TreeSet<>(); // TreeSet for alphabetical sorting

        // Add type-based imports from JavaTypeMapper
        imports.addAll(typeMapper.collectAllImports(fields));

        // Add Lombok imports if enabled
        if (isUseLombok()) {
            imports.add("lombok.AllArgsConstructor");
            imports.add("lombok.Builder");
            imports.add("lombok.Data");
            imports.add("lombok.NoArgsConstructor");
        }

        // Add ArrayList import if any array field is present
        for (FieldNode field : fields) {
            if (field.isArray()) {
                imports.add("java.util.ArrayList");
                break;
            }
        }

        return imports;
    }

    /**
     * Generates a field definition including Javadoc comment.
     *
     * @param sb the StringBuilder to append to
     * @param field the field to generate
     */
    private void generateFieldDefinition(StringBuilder sb, FieldNode field) {
        String type = typeMapper.getSimpleTypeName(field);
        String name = field.getCamelCaseName();

        // Field Javadoc comment
        String originalName = field.getOriginalName();
        if (originalName != null && !originalName.isEmpty() && !originalName.equals(name)) {
            sb.append(INDENT).append("/**").append(NEWLINE);
            sb.append(INDENT).append(" * ").append(originalName);
            if (field.getLength() != null) {
                sb.append(" (length: ").append(field.getLength()).append(")");
            }
            sb.append(NEWLINE);
            sb.append(INDENT).append(" */").append(NEWLINE);
        }

        // @Builder.Default for Lombok array fields
        if (isUseLombok() && field.isArray()) {
            sb.append(INDENT).append("@Builder.Default").append(NEWLINE);
        }

        // Field declaration
        sb.append(INDENT).append("private ").append(type).append(" ").append(name);

        // Array fields get default initialization
        if (field.isArray()) {
            sb.append(" = new ArrayList<>()");
        }

        sb.append(";").append(NEWLINE).append(NEWLINE);
    }

    /**
     * Generates getter and setter methods for a field.
     *
     * @param sb the StringBuilder to append to
     * @param field the field to generate accessors for
     */
    private void generateGetterSetter(StringBuilder sb, FieldNode field) {
        String type = typeMapper.getSimpleTypeName(field);
        String name = field.getCamelCaseName();
        String capitalizedName = capitalizeFirst(name);

        // Getter
        sb.append(NEWLINE);
        sb.append(INDENT).append("public ").append(type).append(" get").append(capitalizedName).append("() {").append(NEWLINE);
        sb.append(INDENT).append(INDENT).append("return this.").append(name).append(";").append(NEWLINE);
        sb.append(INDENT).append("}").append(NEWLINE);

        // Setter
        sb.append(NEWLINE);
        sb.append(INDENT).append("public void set").append(capitalizedName).append("(").append(type).append(" ").append(name).append(") {").append(NEWLINE);
        sb.append(INDENT).append(INDENT).append("this.").append(name).append(" = ").append(name).append(";").append(NEWLINE);
        sb.append(INDENT).append("}").append(NEWLINE);
    }

    /**
     * Generates the Builder inner class for non-Lombok beans.
     *
     * @param sb the StringBuilder to append to
     * @param className the enclosing class name
     * @param fields the fields to include in the Builder
     */
    private void generateBuilderClass(StringBuilder sb, String className, List<FieldNode> fields) {
        sb.append(NEWLINE);
        sb.append(INDENT).append("/**").append(NEWLINE);
        sb.append(INDENT).append(" * Builder for ").append(className).append(NEWLINE);
        sb.append(INDENT).append(" */").append(NEWLINE);
        sb.append(INDENT).append("public static class Builder {").append(NEWLINE).append(NEWLINE);

        // Builder fields
        for (FieldNode field : fields) {
            String type = typeMapper.getSimpleTypeName(field);
            String name = field.getCamelCaseName();
            sb.append(INDENT).append(INDENT).append("private ").append(type).append(" ").append(name);
            if (field.isArray()) {
                sb.append(" = new ArrayList<>()");
            }
            sb.append(";").append(NEWLINE);
        }

        // Builder setter methods
        for (FieldNode field : fields) {
            String type = typeMapper.getSimpleTypeName(field);
            String name = field.getCamelCaseName();
            sb.append(NEWLINE);
            sb.append(INDENT).append(INDENT).append("public Builder ").append(name).append("(").append(type).append(" ").append(name).append(") {").append(NEWLINE);
            sb.append(INDENT).append(INDENT).append(INDENT).append("this.").append(name).append(" = ").append(name).append(";").append(NEWLINE);
            sb.append(INDENT).append(INDENT).append(INDENT).append("return this;").append(NEWLINE);
            sb.append(INDENT).append(INDENT).append("}").append(NEWLINE);
        }

        // build() method
        sb.append(NEWLINE);
        sb.append(INDENT).append(INDENT).append("public ").append(className).append(" build() {").append(NEWLINE);
        sb.append(INDENT).append(INDENT).append(INDENT).append(className).append(" obj = new ").append(className).append("();").append(NEWLINE);
        for (FieldNode field : fields) {
            String name = field.getCamelCaseName();
            sb.append(INDENT).append(INDENT).append(INDENT).append("obj.").append(name).append(" = this.").append(name).append(";").append(NEWLINE);
        }
        sb.append(INDENT).append(INDENT).append(INDENT).append("return obj;").append(NEWLINE);
        sb.append(INDENT).append(INDENT).append("}").append(NEWLINE);

        // Close Builder class
        sb.append(INDENT).append("}").append(NEWLINE);

        // Static builder() factory method
        sb.append(NEWLINE);
        sb.append(INDENT).append("public static Builder builder() {").append(NEWLINE);
        sb.append(INDENT).append(INDENT).append("return new Builder();").append(NEWLINE);
        sb.append(INDENT).append("}").append(NEWLINE);
    }

    /**
     * Writes the generated content to a Java file.
     *
     * @param outputDir the base output directory
     * @param className the class name
     * @param content the Java source code content
     * @throws GenerationException if file writing fails
     */
    private void writeToFile(Path outputDir, String className, String content) {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        Path outputPath = outputDir.resolve("java").resolve(packagePath).resolve(className + ".java");

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenerationException("Failed to write nested class file: " + e.getMessage(), e)
                .withGenerator("NestedClassGenerator")
                .withArtifact(className + ".java");
        }
    }

    /**
     * Returns whether Lombok annotations should be used.
     *
     * @return true if Lombok annotations should be used
     */
    public boolean isUseLombok() {
        return config.getJava() != null && config.getJava().isUseLombok();
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
     * Gets the generated Java source code for a specific class.
     *
     * @param className the name of the class (without .java extension)
     * @return the Java source code, or null if the class was not generated
     */
    public String getGeneratedContent(String className) {
        return generatedContents.get(className);
    }

    /**
     * Gets the list of all generated class names.
     *
     * @return a list of generated class names in generation order
     */
    public List<String> getAllGeneratedClassNames() {
        return new ArrayList<>(processedClassNames);
    }

    /**
     * Clears internal state to allow reuse.
     */
    public void clear() {
        generatedContents.clear();
        processedClassNames.clear();
    }

    /**
     * Gets the JavaTypeMapper used by this generator.
     *
     * @return the type mapper
     */
    public JavaTypeMapper getTypeMapper() {
        return typeMapper;
    }
}
