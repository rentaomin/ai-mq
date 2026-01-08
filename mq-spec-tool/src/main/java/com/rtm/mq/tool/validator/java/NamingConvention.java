package com.rtm.mq.tool.validator.java;

import java.util.regex.Pattern;

/**
 * Naming convention rules for Java Bean validation.
 *
 * <p>This class encapsulates naming patterns for fields and classes.
 * Patterns are configurable to allow customization without code changes.</p>
 *
 * <p>Default conventions:</p>
 * <ul>
 *   <li>Field names: lowerCamelCase (e.g., fieldName, customerAccount)</li>
 *   <li>Class names: UpperCamelCase / PascalCase (e.g., ClassName, CustomerAccount)</li>
 * </ul>
 */
public class NamingConvention {

    /**
     * Default pattern for field names (lowerCamelCase).
     * Must start with lowercase letter, followed by letters or digits.
     */
    public static final String DEFAULT_FIELD_PATTERN = "^[a-z][a-zA-Z0-9]*$";

    /**
     * Default pattern for class names (UpperCamelCase / PascalCase).
     * Must start with uppercase letter, followed by letters or digits.
     */
    public static final String DEFAULT_CLASS_PATTERN = "^[A-Z][a-zA-Z0-9]*$";

    private final Pattern fieldPattern;
    private final Pattern classPattern;

    /**
     * Constructs a NamingConvention with default patterns.
     */
    public NamingConvention() {
        this(DEFAULT_FIELD_PATTERN, DEFAULT_CLASS_PATTERN);
    }

    /**
     * Constructs a NamingConvention with custom patterns.
     *
     * @param fieldPatternStr the regex pattern for field names
     * @param classPatternStr the regex pattern for class names
     */
    public NamingConvention(String fieldPatternStr, String classPatternStr) {
        this.fieldPattern = Pattern.compile(fieldPatternStr);
        this.classPattern = Pattern.compile(classPatternStr);
    }

    /**
     * Validates a field name against the naming convention.
     *
     * @param fieldName the field name to validate
     * @return true if the name matches the convention
     */
    public boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        return fieldPattern.matcher(fieldName).matches();
    }

    /**
     * Validates a class name against the naming convention.
     *
     * @param className the class name to validate
     * @return true if the name matches the convention
     */
    public boolean isValidClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        return classPattern.matcher(className).matches();
    }

    /**
     * Gets the field pattern as a string.
     *
     * @return the field pattern regex
     */
    public String getFieldPatternString() {
        return fieldPattern.pattern();
    }

    /**
     * Gets the class pattern as a string.
     *
     * @return the class pattern regex
     */
    public String getClassPatternString() {
        return classPattern.pattern();
    }
}
