package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Java type mapper for converting FieldNode dataType to Java types.
 *
 * <p>This mapper is used by the Java Bean Generator (T-204) to determine
 * the appropriate Java type for each field in the intermediate JSON tree.</p>
 *
 * <p>Mapping rules (from architecture document 6.2.4):</p>
 * <ul>
 *   <li>String, AN, A, Date - String</li>
 *   <li>Number, N, Unsigned Integer - String (fixed-length message preservation)</li>
 *   <li>Amount, Currency - java.math.BigDecimal</li>
 *   <li>Object (1..1, 0..1) - Nested class name</li>
 *   <li>Array (0..N, 1..N) - java.util.List&lt;ClassName&gt;</li>
 * </ul>
 *
 * <p>Unknown or null data types default to String.</p>
 */
public class JavaTypeMapper {

    /**
     * Static type mapping from Excel dataType to Java type.
     * Uses LinkedHashMap to preserve insertion order for deterministic iteration.
     */
    private static final Map<String, String> TYPE_MAP = new LinkedHashMap<>();

    static {
        // String types
        TYPE_MAP.put("String", "String");
        TYPE_MAP.put("AN", "String");
        TYPE_MAP.put("A/N", "String");
        TYPE_MAP.put("A", "String");

        // Numeric types (kept as String for fixed-length message preservation)
        TYPE_MAP.put("Number", "String");
        TYPE_MAP.put("N", "String");
        TYPE_MAP.put("Unsigned Integer", "String");

        // Amount types (BigDecimal for precision)
        TYPE_MAP.put("Amount", "java.math.BigDecimal");
        TYPE_MAP.put("Currency", "java.math.BigDecimal");

        // Date type (kept as String for format preservation)
        TYPE_MAP.put("Date", "String");
    }

    private final Config config;

    /**
     * Constructs a JavaTypeMapper with the given configuration.
     *
     * @param config the configuration containing Java generation settings
     */
    public JavaTypeMapper(Config config) {
        this.config = config;
    }

    /**
     * Maps a FieldNode to its corresponding Java type.
     *
     * <p>Mapping priority:</p>
     * <ol>
     *   <li>Array fields - List&lt;ClassName&gt;</li>
     *   <li>Object fields - ClassName</li>
     *   <li>Basic types - Mapped from TYPE_MAP or default to String</li>
     * </ol>
     *
     * @param field the field node from the intermediate JSON tree
     * @return the Java type string (simple or fully qualified)
     */
    public String mapType(FieldNode field) {
        // Array type
        if (field.isArray()) {
            String itemType = resolveObjectType(field);
            return "java.util.List<" + itemType + ">";
        }

        // Object type
        if (field.isObject()) {
            return resolveObjectType(field);
        }

        // Basic type
        String dataType = field.getDataType();
        if (dataType == null || dataType.isEmpty()) {
            return "String"; // Default to String
        }

        return TYPE_MAP.getOrDefault(dataType, "String");
    }

    /**
     * Resolves the class name for object or array item types.
     *
     * <p>Uses FieldNode.className if available, otherwise falls back
     * to capitalizing the camelCaseName.</p>
     *
     * @param field the field node
     * @return the resolved class name
     */
    private String resolveObjectType(FieldNode field) {
        String className = field.getClassName();
        if (className == null || className.isEmpty()) {
            // Fallback to capitalizing camelCaseName
            className = capitalizeFirst(field.getCamelCaseName());
        }
        return className;
    }

    /**
     * Gets the simple type name (without package prefix).
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>java.util.List&lt;Item&gt; - List&lt;Item&gt;</li>
     *   <li>java.math.BigDecimal - BigDecimal</li>
     *   <li>String - String</li>
     * </ul>
     *
     * @param field the field node
     * @return the simple type name for use in field declarations
     */
    public String getSimpleTypeName(FieldNode field) {
        String fullType = mapType(field);

        // Handle generic types
        if (fullType.contains("<")) {
            return fullType.replace("java.util.List", "List")
                          .replace("java.math.BigDecimal", "BigDecimal");
        }

        // Remove package prefix
        int lastDot = fullType.lastIndexOf('.');
        return lastDot >= 0 ? fullType.substring(lastDot + 1) : fullType;
    }

    /**
     * Collects the import statements required for a single field.
     *
     * @param field the field node
     * @return a set of import statements (fully qualified class names)
     */
    public Set<String> collectImports(FieldNode field) {
        Set<String> imports = new LinkedHashSet<>();
        String type = mapType(field);

        if (type.startsWith("java.util.List")) {
            imports.add("java.util.List");
        }
        if (type.contains("BigDecimal")) {
            imports.add("java.math.BigDecimal");
        }

        return imports;
    }

    /**
     * Collects all import statements required for a list of fields.
     *
     * <p>Results are sorted alphabetically for deterministic output.</p>
     *
     * @param fields the list of field nodes
     * @return a sorted set of import statements
     */
    public Set<String> collectAllImports(List<FieldNode> fields) {
        Set<String> imports = new TreeSet<>(); // TreeSet for alphabetical sorting
        for (FieldNode field : fields) {
            imports.addAll(collectImports(field));
        }
        return imports;
    }

    /**
     * Gets the model package name from configuration.
     *
     * <p>Uses JavaConfig.packageName if available, otherwise constructs
     * from xml.project.groupId + xml.project.artifactId + ".model".</p>
     *
     * @return the fully qualified package name for generated model classes
     */
    public String getModelPackage() {
        // Prefer JavaConfig.packageName
        if (config.getJava() != null && config.getJava().getPackageName() != null
                && !config.getJava().getPackageName().isEmpty()) {
            return config.getJava().getPackageName();
        }

        // Fallback to xml.project settings
        if (config.getXml() != null && config.getXml().getProject() != null) {
            String groupId = config.getXml().getProject().getGroupId();
            String artifactId = config.getXml().getProject().getArtifactId();
            if (groupId != null && artifactId != null) {
                return groupId + "." + artifactId + ".model";
            }
        }

        // Default package
        return "com.rtm.mq.model";
    }

    /**
     * Checks if a type requires an import statement.
     *
     * <p>Types starting with "java.util." or "java.math." require imports.
     * Types in the java.lang package (like String) do not require imports.</p>
     *
     * @param type the type string
     * @return true if the type requires an import statement
     */
    public boolean requiresImport(String type) {
        return type.startsWith("java.util.") ||
               type.startsWith("java.math.") ||
               (type.contains(".") && !type.startsWith("java.lang."));
    }

    /**
     * Gets the default initializer expression for a field.
     *
     * <p>Array fields are initialized to new ArrayList&lt;&gt;() to avoid
     * null pointer exceptions. Non-array fields return null (no initializer).</p>
     *
     * @param field the field node
     * @return the initializer expression, or null if no default initialization
     */
    public String getDefaultInitializer(FieldNode field) {
        if (field.isArray()) {
            return "new java.util.ArrayList<>()";
        }
        return null; // Non-array fields do not need default initialization
    }

    /**
     * Gets the simple initializer expression (for use with imports).
     *
     * @param field the field node
     * @return the simple initializer expression, or null if no initialization
     */
    public String getSimpleInitializer(FieldNode field) {
        if (field.isArray()) {
            return "new ArrayList<>()";
        }
        return null;
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
     * Checks if the given data type maps to BigDecimal.
     *
     * @param dataType the data type from the specification
     * @return true if the type maps to BigDecimal
     */
    public boolean isBigDecimalType(String dataType) {
        if (dataType == null) {
            return false;
        }
        String mapped = TYPE_MAP.get(dataType);
        return "java.math.BigDecimal".equals(mapped);
    }

    /**
     * Gets the static type mapping table for testing or introspection.
     *
     * @return an unmodifiable view of the type mapping
     */
    public static Map<String, String> getTypeMap() {
        return Map.copyOf(TYPE_MAP);
    }

    /**
     * Checks if a field represents an enum type.
     *
     * <p>A field is considered an enum type if it has a non-empty enumConstraint
     * that follows the format "value1|value2|..." (e.g., "01|02|03" or "Y|N").</p>
     *
     * @param field the field node to check
     * @return true if the field represents an enum type
     */
    public boolean isEnumType(FieldNode field) {
        if (field == null) {
            return false;
        }
        String constraint = field.getEnumConstraint();
        return constraint != null && !constraint.isBlank() && constraint.contains("|");
    }
}
