package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Java Bean generator for Request and Response classes.
 *
 * <p>This generator creates Java POJO classes from a {@link MessageModel},
 * producing separate Request and Response classes named using the operationId.</p>
 *
 * <p>Generated classes include:</p>
 * <ul>
 *   <li>Package declaration</li>
 *   <li>Import statements (sorted alphabetically)</li>
 *   <li>Class-level Javadoc with @generated marker</li>
 *   <li>Lombok annotations (if enabled): @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor</li>
 *   <li>Private fields with type comments</li>
 *   <li>Getter/Setter methods (if Lombok disabled)</li>
 *   <li>Builder inner class (if Lombok disabled)</li>
 * </ul>
 *
 * <p>Transitory fields (groupId, occurrenceCount) are excluded from generated beans.</p>
 *
 * @see JavaGenerator
 * @see JavaTypeMapper
 */
public class JavaBeanGenerator implements JavaGenerator {

    private static final String GENERATOR_TYPE = "java";
    private static final String NEWLINE = "\n";
    private static final String INDENT = "    ";

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final Map<String, String> generatedContents = new LinkedHashMap<>();
    private final List<String> generatedClassNames = new ArrayList<>();

    /**
     * Constructs a JavaBeanGenerator with the given configuration.
     *
     * @param config the configuration containing Java generation settings
     */
    public JavaBeanGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    @Override
    public Map<String, String> generate(MessageModel model, Path outputDir) {
        // Clear previous state
        generatedContents.clear();
        generatedClassNames.clear();

        String operationId = getOperationId(model);

        // Generate Request Bean
        FieldGroup request = model.getRequest();
        if (request != null && request.getFields() != null && !request.getFields().isEmpty()) {
            String requestClassName = operationId + "Request";
            String requestContent = generateBeanClass(
                requestClassName,
                request.getFields(),
                "Request Bean for " + operationId
            );
            generatedContents.put(requestClassName, requestContent);
            generatedClassNames.add(requestClassName);
            writeToFile(outputDir, requestClassName, requestContent);
        }

        // Generate Response Bean
        FieldGroup response = model.getResponse();
        if (response != null && response.getFields() != null && !response.getFields().isEmpty()) {
            String responseClassName = operationId + "Response";
            String responseContent = generateBeanClass(
                responseClassName,
                response.getFields(),
                "Response Bean for " + operationId
            );
            generatedContents.put(responseClassName, responseContent);
            generatedClassNames.add(responseClassName);
            writeToFile(outputDir, responseClassName, responseContent);
        }

        // Build result map with relative paths
        Map<String, String> result = new LinkedHashMap<>();
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        for (Map.Entry<String, String> entry : generatedContents.entrySet()) {
            String relativePath = "java/" + packagePath + "/" + entry.getKey() + ".java";
            result.put(relativePath, entry.getValue());
        }

        return result;
    }

    @Override
    public String generateClass(String className, List<FieldNode> fields) {
        return generateBeanClass(className, fields, className);
    }

    @Override
    public String getType() {
        return GENERATOR_TYPE;
    }

    @Override
    public boolean isUseLombok() {
        return config.getJava() != null && config.getJava().isUseLombok();
    }

    @Override
    public String getGeneratedContent(String className) {
        return generatedContents.get(className);
    }

    @Override
    public List<String> getGeneratedClassNames() {
        return new ArrayList<>(generatedClassNames);
    }

    /**
     * Gets the output file path for a specific class.
     *
     * @param outputDir the base output directory
     * @param className the class name
     * @return the full path where the class file will be written
     */
    public Path getOutputPath(Path outputDir, String className) {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        return outputDir.resolve("java").resolve(packagePath).resolve(className + ".java");
    }

    /**
     * Extracts and validates the operationId from the model.
     *
     * @param model the message model
     * @return the operationId
     * @throws GenerationException if operationId is missing or blank
     */
    private String getOperationId(MessageModel model) {
        if (model.getMetadata() == null ||
            model.getMetadata().getOperationId() == null ||
            model.getMetadata().getOperationId().isBlank()) {
            throw new GenerationException("Operation ID is required for Java Bean generation")
                .withGenerator("JavaBeanGenerator");
        }
        return model.getMetadata().getOperationId();
    }

    /**
     * Generates a single Java Bean class.
     *
     * @param className the class name to generate
     * @param fields the field nodes to include
     * @param description the class description for Javadoc
     * @return the generated Java source code
     */
    private String generateBeanClass(String className, List<FieldNode> fields, String description) {
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
            sb.append(" = new java.util.ArrayList<>()");
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
                sb.append(" = new java.util.ArrayList<>()");
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
        Path outputPath = getOutputPath(outputDir, className);

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenerationException("Failed to write Java file: " + e.getMessage(), e)
                .withGenerator("JavaBeanGenerator")
                .withArtifact(className + ".java");
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
}
