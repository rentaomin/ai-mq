package com.rtm.mq.tool.validator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.JavaConfig;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaBeanValidator.
 */
class JavaBeanValidatorTest {

    private JavaBeanValidator validator;
    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
        JavaConfig javaConfig = new JavaConfig();
        javaConfig.setPackageName("com.test.model");
        config.setJava(javaConfig);
        validator = new JavaBeanValidator(config);
    }

    @Test
    void testGetType() {
        assertEquals("java-bean", validator.getType());
    }

    @Test
    void testValidFieldGroup() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("customerName", "String", false, false));
        group.addField(createField("accountNumber", "AN", false, false));

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "CustomerInfo", "CustomerInfo.java");

        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testFieldNamingViolation() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("CustomerName", "String", false, false)); // Wrong: uppercase start

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "CustomerInfo", "CustomerInfo.java");

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount());
        assertEquals(JavaBeanValidator.RULE_FIELD_NAMING, result.getErrors().get(0).getRuleId());
    }

    @Test
    void testClassNamingViolation() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("field", "String", false, false));

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "customerInfo", "customerInfo.java"); // Wrong: lowercase start

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRuleId().equals(JavaBeanValidator.RULE_CLASS_NAMING)));
    }

    @Test
    void testForbiddenFieldGroupId() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("groupId", "String", false, false)); // Forbidden

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRuleId().equals(JavaBeanValidator.RULE_FORBIDDEN_GROUP_ID)));
    }

    @Test
    void testForbiddenFieldOccurrenceCount() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("occurrenceCount", "String", false, false)); // Forbidden

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRuleId().equals(JavaBeanValidator.RULE_FORBIDDEN_OCCURRENCE_COUNT)));
    }

    @Test
    void testArrayFieldTypeMapping() {
        FieldNode arrayField = FieldNode.builder()
                .camelCaseName("items")
                .originalName("items")
                .isArray(true)
                .className("Item")
                .children(List.of(createField("itemName", "String", false, false)))
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(arrayField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Order", "Order.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testObjectFieldNestedStructure() {
        FieldNode childField = createField("street", "String", false, false);
        FieldNode objectField = FieldNode.builder()
                .camelCaseName("address")
                .originalName("address")
                .isObject(true)
                .className("Address")
                .children(List.of(childField))
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(objectField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Customer", "Customer.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testObjectWithoutChildren() {
        FieldNode objectField = FieldNode.builder()
                .camelCaseName("details")
                .originalName("details")
                .isObject(true)
                .className("Details")
                .children(new ArrayList<>()) // Empty children
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(objectField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Order", "Order.java");

        // Should produce warning about empty nested class
        assertEquals(1, result.getWarningCount());
    }

    @Test
    void testEnumFieldWithValidConstraint() {
        FieldNode enumField = FieldNode.builder()
                .camelCaseName("status")
                .originalName("status")
                .dataType("String")
                .enumConstraint("01|02|03")
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(enumField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Order", "Order.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testEnumFieldMissingConstraint() {
        FieldNode enumField = FieldNode.builder()
                .camelCaseName("status")
                .originalName("status")
                .dataType("String")
                .enumConstraint("|") // Will be detected as enum but no valid values
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(enumField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Order", "Order.java");

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRuleId().equals(JavaBeanValidator.RULE_MISSING_ENUM_HELPER)));
    }

    @Test
    void testEnumFieldWithSingleValue() {
        FieldNode enumField = FieldNode.builder()
                .camelCaseName("flag")
                .originalName("flag")
                .dataType("String")
                .enumConstraint("Y") // Single value, no pipe
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(enumField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Order", "Order.java");

        // Should be success but produce warning about missing pipe
        assertTrue(result.isSuccess());
    }

    @Test
    void testTransitoryFieldSkipped() {
        FieldNode transitoryField = FieldNode.builder()
                .camelCaseName("groupId") // Would be forbidden if not transitory
                .originalName("group_id")
                .isTransitory(true)
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(transitoryField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess()); // Transitory fields are skipped
    }

    @Test
    void testNullFieldGroup() {
        JavaBeanValidationResult result = validator.validateFieldGroup(
                null, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess());
        assertEquals(0, result.getIssueCount());
    }

    @Test
    void testEmptyFieldGroup() {
        FieldGroup group = new FieldGroup();

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testValidateFields() {
        List<FieldNode> fields = List.of(
                createField("field1", "String", false, false),
                createField("field2", "AN", false, false)
        );

        JavaBeanValidationResult result = validator.validateFields(
                fields, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testValidateFieldsWithNull() {
        JavaBeanValidationResult result = validator.validateFields(
                null, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testNestedFieldNamingValidation() {
        FieldNode childField = FieldNode.builder()
                .camelCaseName("ChildField") // Wrong: uppercase start
                .originalName("child_field")
                .dataType("String")
                .build();

        FieldNode objectField = FieldNode.builder()
                .camelCaseName("nested")
                .originalName("nested")
                .isObject(true)
                .className("Nested")
                .children(List.of(childField))
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(objectField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Parent", "Parent.java");

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getRuleId().equals(JavaBeanValidator.RULE_FIELD_NAMING)));
    }

    @Test
    void testBigDecimalTypeMapping() {
        FieldNode amountField = FieldNode.builder()
                .camelCaseName("amount")
                .originalName("amount")
                .dataType("Amount")
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(amountField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Payment", "Payment.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testCurrencyTypeMapping() {
        FieldNode currencyField = FieldNode.builder()
                .camelCaseName("price")
                .originalName("price")
                .dataType("Currency")
                .build();

        FieldGroup group = new FieldGroup();
        group.addField(currencyField);

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "Product", "Product.java");

        assertTrue(result.isSuccess());
    }

    @Test
    void testGetRequiredEnumHelperMethods() {
        List<String> methods = JavaBeanValidator.getRequiredEnumHelperMethods();

        assertNotNull(methods);
        assertEquals(4, methods.size());
        assertTrue(methods.contains("fromCode"));
        assertTrue(methods.contains("isValid"));
        assertTrue(methods.contains("getCode"));
        assertTrue(methods.contains("getDescription"));
    }

    @Test
    void testMultipleViolations() {
        FieldGroup group = new FieldGroup();
        group.addField(createField("FieldOne", "String", false, false)); // Naming
        group.addField(createField("groupId", "String", false, false)); // Forbidden
        group.addField(createField("occurrenceCount", "String", false, false)); // Forbidden

        JavaBeanValidationResult result = validator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertFalse(result.isSuccess());
        assertEquals(3, result.getErrorCount());
    }

    @Test
    void testCustomNamingConvention() {
        NamingConvention custom = new NamingConvention(
                "^[a-z_][a-z_0-9]*$", // Allow underscores
                "^[A-Z][A-Za-z0-9]*$"
        );
        JavaBeanValidator customValidator = new JavaBeanValidator(config, custom);

        FieldGroup group = new FieldGroup();
        group.addField(createField("field_name", "String", false, false));

        JavaBeanValidationResult result = customValidator.validateFieldGroup(
                group, "TestClass", "TestClass.java");

        assertTrue(result.isSuccess());
    }

    /**
     * Helper method to create a simple FieldNode.
     */
    private FieldNode createField(String camelName, String dataType,
                                   boolean isObject, boolean isArray) {
        return FieldNode.builder()
                .camelCaseName(camelName)
                .originalName(camelName)
                .dataType(dataType)
                .isObject(isObject)
                .isArray(isArray)
                .build();
    }
}
