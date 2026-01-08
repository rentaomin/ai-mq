package com.rtm.mq.tool.validator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.generator.java.JavaTypeMapper;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.ValidationResult;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.validator.Validator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static structure validator for Java Beans.
 *
 * <p>This validator performs static analysis on Java Bean structures based on
 * the intermediate JSON tree (FieldGroup/FieldNode). It does not perform
 * runtime validation or compilation.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>JB-001: Field naming convention (lowerCamelCase)</li>
 *   <li>JB-002: Class naming convention (UpperCamelCase)</li>
 *   <li>JB-003: Forbidden field: groupId</li>
 *   <li>JB-004: Forbidden field: occurrenceCount</li>
 *   <li>JB-005: Type mapping consistency with JavaTypeMapper</li>
 *   <li>JB-006: Nested class structure alignment</li>
 *   <li>JB-007: Enum helper method presence</li>
 *   <li>JB-008: Missing nested class for object/array fields</li>
 * </ul>
 *
 * @see JavaTypeMapper
 * @see NamingConvention
 * @see JavaBeanValidationResult
 */
public class JavaBeanValidator implements Validator {

    /** Rule ID for field naming convention violations. */
    public static final String RULE_FIELD_NAMING = "JB-001";

    /** Rule ID for class naming convention violations. */
    public static final String RULE_CLASS_NAMING = "JB-002";

    /** Rule ID for forbidden field: groupId. */
    public static final String RULE_FORBIDDEN_GROUP_ID = "JB-003";

    /** Rule ID for forbidden field: occurrenceCount. */
    public static final String RULE_FORBIDDEN_OCCURRENCE_COUNT = "JB-004";

    /** Rule ID for type mapping mismatch. */
    public static final String RULE_TYPE_MISMATCH = "JB-005";

    /** Rule ID for nested structure mismatch. */
    public static final String RULE_NESTED_STRUCTURE = "JB-006";

    /** Rule ID for missing enum helper method. */
    public static final String RULE_MISSING_ENUM_HELPER = "JB-007";

    /** Rule ID for missing nested class. */
    public static final String RULE_MISSING_NESTED_CLASS = "JB-008";

    /** Forbidden field name: groupId. */
    private static final String FORBIDDEN_GROUP_ID = "groupId";

    /** Forbidden field name: occurrenceCount. */
    private static final String FORBIDDEN_OCCURRENCE_COUNT = "occurrenceCount";

    /** Required enum helper methods. */
    private static final List<String> ENUM_HELPER_METHODS = List.of(
            "fromCode",
            "isValid",
            "getCode",
            "getDescription"
    );

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final NamingConvention namingConvention;

    /**
     * Constructs a JavaBeanValidator with the given configuration.
     *
     * @param config the configuration
     */
    public JavaBeanValidator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
        this.namingConvention = new NamingConvention();
    }

    /**
     * Constructs a JavaBeanValidator with custom naming convention.
     *
     * @param config the configuration
     * @param namingConvention the naming convention rules
     */
    public JavaBeanValidator(Config config, NamingConvention namingConvention) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
        this.namingConvention = namingConvention;
    }

    @Override
    public String getType() {
        return "java-bean";
    }

    @Override
    public ValidationResult validate(Path targetPath) {
        // This method is for file-based validation
        // For structure validation, use validateFieldGroup
        ValidationResult result = new ValidationResult();
        result.addWarning(new ValidationError(
                "JB-000",
                "File-based validation not supported",
                "Use validateFieldGroup() for structure validation",
                ValidationError.Severity.WARN
        ));
        return result;
    }

    /**
     * Validates a FieldGroup structure against Java Bean rules.
     *
     * <p>This is the primary validation entry point for structure validation.</p>
     *
     * @param fieldGroup the field group to validate
     * @param className the expected class name for this group
     * @param filePath the logical file path (for issue reporting)
     * @return the validation result
     */
    public JavaBeanValidationResult validateFieldGroup(FieldGroup fieldGroup,
                                                        String className,
                                                        String filePath) {
        JavaBeanValidationResult result = new JavaBeanValidationResult();

        if (fieldGroup == null || fieldGroup.getFields() == null) {
            return result;
        }

        // Validate class naming convention
        validateClassName(className, filePath, result);

        // Track expected nested classes
        Set<String> expectedNestedClasses = new HashSet<>();

        // Validate each field
        for (FieldNode field : fieldGroup.getFields()) {
            validateField(field, filePath, className, result, expectedNestedClasses);
        }

        return result;
    }

    /**
     * Validates a single field node recursively.
     *
     * @param field the field to validate
     * @param filePath the file path for reporting
     * @param parentPath the parent class path
     * @param result the result to add issues to
     * @param expectedNestedClasses set of expected nested class names
     */
    private void validateField(FieldNode field, String filePath, String parentPath,
                                JavaBeanValidationResult result,
                                Set<String> expectedNestedClasses) {
        if (field == null) {
            return;
        }

        // Skip transitory fields
        if (field.isTransitory()) {
            return;
        }

        String fieldName = field.getCamelCaseName();
        String fieldPath = parentPath + "." + fieldName;

        // Rule JB-001: Field naming convention
        validateFieldNaming(fieldName, filePath, fieldPath, result);

        // Rule JB-003: Forbidden field groupId
        validateForbiddenGroupId(fieldName, filePath, fieldPath, result);

        // Rule JB-004: Forbidden field occurrenceCount
        validateForbiddenOccurrenceCount(fieldName, filePath, fieldPath, result);

        // Rule JB-005: Type mapping consistency
        validateTypeMapping(field, filePath, fieldPath, result);

        // For object/array types, validate nested structure
        if (field.isObject() || field.isArray()) {
            String nestedClassName = field.getClassName();
            if (nestedClassName == null || nestedClassName.isEmpty()) {
                nestedClassName = capitalizeFirst(field.getCamelCaseName());
            }

            // Rule JB-002: Nested class naming convention
            validateClassName(nestedClassName, filePath, result);

            // Track expected nested class
            expectedNestedClasses.add(nestedClassName);

            // Rule JB-006: Nested structure alignment
            validateNestedStructure(field, nestedClassName, filePath, fieldPath, result);

            // Recursively validate children
            if (field.hasChildren()) {
                for (FieldNode child : field.getChildren()) {
                    validateField(child, filePath, fieldPath, result, expectedNestedClasses);
                }
            }
        }

        // Rule JB-007: Enum helper method presence
        if (typeMapper.isEnumType(field)) {
            validateEnumHelperMethods(field, filePath, fieldPath, result);
        }
    }

    /**
     * Validates field naming convention (lowerCamelCase).
     */
    private void validateFieldNaming(String fieldName, String filePath, String fieldPath,
                                      JavaBeanValidationResult result) {
        if (fieldName == null || fieldName.isEmpty()) {
            result.addError(filePath, fieldPath, RULE_FIELD_NAMING,
                    "Field name is null or empty");
            return;
        }

        if (!namingConvention.isValidFieldName(fieldName)) {
            result.addError(filePath, fieldPath, RULE_FIELD_NAMING,
                    "Field name '" + fieldName + "' does not follow lowerCamelCase convention");
        }
    }

    /**
     * Validates class naming convention (UpperCamelCase).
     */
    private void validateClassName(String className, String filePath,
                                    JavaBeanValidationResult result) {
        if (className == null || className.isEmpty()) {
            result.addError(filePath, className, RULE_CLASS_NAMING,
                    "Class name is null or empty");
            return;
        }

        if (!namingConvention.isValidClassName(className)) {
            result.addError(filePath, className, RULE_CLASS_NAMING,
                    "Class name '" + className + "' does not follow UpperCamelCase convention");
        }
    }

    /**
     * Validates that groupId field is not present.
     */
    private void validateForbiddenGroupId(String fieldName, String filePath, String fieldPath,
                                           JavaBeanValidationResult result) {
        if (FORBIDDEN_GROUP_ID.equals(fieldName) ||
            FORBIDDEN_GROUP_ID.equalsIgnoreCase(fieldName)) {
            result.addError(filePath, fieldPath, RULE_FORBIDDEN_GROUP_ID,
                    "Forbidden field 'groupId' must not appear in Java Bean");
        }
    }

    /**
     * Validates that occurrenceCount field is not present.
     */
    private void validateForbiddenOccurrenceCount(String fieldName, String filePath, String fieldPath,
                                                   JavaBeanValidationResult result) {
        if (FORBIDDEN_OCCURRENCE_COUNT.equals(fieldName) ||
            FORBIDDEN_OCCURRENCE_COUNT.equalsIgnoreCase(fieldName)) {
            result.addError(filePath, fieldPath, RULE_FORBIDDEN_OCCURRENCE_COUNT,
                    "Forbidden field 'occurrenceCount' must not appear in Java Bean");
        }
    }

    /**
     * Validates type mapping consistency with JavaTypeMapper.
     */
    private void validateTypeMapping(FieldNode field, String filePath, String fieldPath,
                                      JavaBeanValidationResult result) {
        String mappedType = typeMapper.mapType(field);

        // Validate that the type is deterministic and non-null
        if (mappedType == null || mappedType.isEmpty()) {
            result.addError(filePath, fieldPath, RULE_TYPE_MISMATCH,
                    "Type mapping returned null or empty for field");
            return;
        }

        // For array fields, verify List type
        if (field.isArray() && !mappedType.contains("List")) {
            result.addError(filePath, fieldPath, RULE_TYPE_MISMATCH,
                    "Array field should map to List type, got: " + mappedType);
        }

        // For BigDecimal types, verify correct mapping
        String dataType = field.getDataType();
        if (dataType != null && (dataType.equals("Amount") || dataType.equals("Currency"))) {
            if (!mappedType.contains("BigDecimal")) {
                result.addError(filePath, fieldPath, RULE_TYPE_MISMATCH,
                        "Amount/Currency field should map to BigDecimal, got: " + mappedType);
            }
        }
    }

    /**
     * Validates nested structure alignment with JSON tree hierarchy.
     */
    private void validateNestedStructure(FieldNode field, String nestedClassName,
                                          String filePath, String fieldPath,
                                          JavaBeanValidationResult result) {
        // Rule JB-008: Validate that object/array fields have corresponding structure
        if ((field.isObject() || field.isArray()) && !field.hasChildren()) {
            // Object/array without children may indicate structure issue
            result.addWarning(filePath, fieldPath, RULE_MISSING_NESTED_CLASS,
                    "Object/array field '" + field.getCamelCaseName() +
                    "' has no children defined for nested class: " + nestedClassName);
        }

        // Validate nested class will be generated with proper name
        if (nestedClassName == null || nestedClassName.isEmpty()) {
            result.addError(filePath, fieldPath, RULE_NESTED_STRUCTURE,
                    "Cannot determine nested class name for object/array field");
        }
    }

    /**
     * Validates enum helper method presence (by name only, no body inspection).
     */
    private void validateEnumHelperMethods(FieldNode field, String filePath, String fieldPath,
                                            JavaBeanValidationResult result) {
        // This validation checks that enum fields will have required helper methods
        // The actual method generation is done by EnumHelperGenerator
        // We validate that the field structure supports enum generation

        String enumConstraint = field.getEnumConstraint();
        if (enumConstraint == null || enumConstraint.isBlank()) {
            result.addError(filePath, fieldPath, RULE_MISSING_ENUM_HELPER,
                    "Enum field missing enumConstraint, cannot generate helper methods");
            return;
        }

        // Validate enum constraint format (should contain | separator)
        if (!enumConstraint.contains("|")) {
            result.addWarning(filePath, fieldPath, RULE_MISSING_ENUM_HELPER,
                    "Enum constraint '" + enumConstraint +
                    "' does not contain '|' separator, may not generate proper enum values");
        }

        // Validate enum values are extractable
        String[] values = enumConstraint.split("\\|");
        int validCount = 0;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                validCount++;
            }
        }
        if (validCount == 0) {
            result.addError(filePath, fieldPath, RULE_MISSING_ENUM_HELPER,
                    "Enum constraint contains no valid values");
        }
    }

    /**
     * Validates a list of fields (convenience method).
     *
     * @param fields the list of fields to validate
     * @param className the parent class name
     * @param filePath the file path for reporting
     * @return the validation result
     */
    public JavaBeanValidationResult validateFields(List<FieldNode> fields,
                                                    String className,
                                                    String filePath) {
        FieldGroup group = new FieldGroup();
        group.setFields(fields != null ? fields : new ArrayList<>());
        return validateFieldGroup(group, className, filePath);
    }

    /**
     * Gets the names of required enum helper methods.
     *
     * @return list of required method names
     */
    public static List<String> getRequiredEnumHelperMethods() {
        return ENUM_HELPER_METHODS;
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
