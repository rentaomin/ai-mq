package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OpenApiTypeMapper}.
 */
class OpenApiTypeMapperTest {

    private OpenApiTypeMapper mapper;
    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
        mapper = new OpenApiTypeMapper(config);
    }

    @Nested
    @DisplayName("Primitive Type Mapping")
    class PrimitiveTypeMappingTests {

        @Test
        @DisplayName("String type maps to type: string")
        void stringTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("String")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertNull(schema.get("format"));
        }

        @Test
        @DisplayName("AN type maps to type: string")
        void anTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("AN")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertNull(schema.get("format"));
        }

        @Test
        @DisplayName("A type maps to type: string")
        void aTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("A")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }

        @Test
        @DisplayName("A/N type maps to type: string")
        void aSlashNTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("A/N")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }

        @Test
        @DisplayName("Number type maps to type: string")
        void numberTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("Number")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertNull(schema.get("format"));
        }

        @Test
        @DisplayName("N type maps to type: string")
        void nTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("N")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }

        @Test
        @DisplayName("Unsigned Integer type maps to type: string")
        void unsignedIntegerMapsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("fieldName")
                    .dataType("Unsigned Integer")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }

        @Test
        @DisplayName("Amount type maps to type: string, format: decimal")
        void amountTypeMapsToDecimal() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("amount")
                    .dataType("Amount")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertEquals("decimal", schema.get("format"));
        }

        @Test
        @DisplayName("Currency type maps to type: string, format: decimal")
        void currencyTypeMapsToDecimal() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("currency")
                    .dataType("Currency")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertEquals("decimal", schema.get("format"));
        }

        @Test
        @DisplayName("Date type maps to type: string, format: date")
        void dateTypeMapsToDate() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("birthDate")
                    .dataType("Date")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertEquals("date", schema.get("format"));
        }

        @Test
        @DisplayName("Unknown type defaults to type: string")
        void unknownTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("unknownField")
                    .dataType("SomeUnknownType")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
            assertNull(schema.get("format"));
        }

        @Test
        @DisplayName("Null dataType defaults to type: string")
        void nullDataTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("nullTypeField")
                    .dataType(null)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }

        @Test
        @DisplayName("Empty dataType defaults to type: string")
        void emptyDataTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("emptyTypeField")
                    .dataType("")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }
    }

    @Nested
    @DisplayName("Array Type Mapping")
    class ArrayTypeMappingTests {

        @Test
        @DisplayName("Array field generates type: array with items.$ref")
        void arrayFieldGeneratesArraySchema() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("cardArray")
                    .className("CardArray")
                    .isArray(true)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("array", schema.get("type"));
            assertNotNull(schema.get("items"));
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) schema.get("items");
            assertEquals("./CardArray.yaml", items.get("$ref"));
        }

        @Test
        @DisplayName("Array with occurrenceCount 0..9 generates maxItems: 9")
        void arrayWithBoundedOccurrenceGeneratesMaxItems() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("cbaCardArr")
                    .className("CBACardArray")
                    .isArray(true)
                    .occurrenceCount("0..9")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("array", schema.get("type"));
            assertEquals(9, schema.get("maxItems"));
        }

        @Test
        @DisplayName("Array with occurrenceCount 1..5 generates maxItems: 5")
        void arrayWithOneToFiveGeneratesMaxItems() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("items")
                    .className("Item")
                    .isArray(true)
                    .occurrenceCount("1..5")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals(5, schema.get("maxItems"));
        }

        @Test
        @DisplayName("Array with occurrenceCount 0..N does not generate maxItems")
        void arrayWithUnboundedOccurrenceNoMaxItems() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("unboundedArray")
                    .className("UnboundedItem")
                    .isArray(true)
                    .occurrenceCount("0..N")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("array", schema.get("type"));
            assertNull(schema.get("maxItems"));
        }

        @Test
        @DisplayName("Array with occurrenceCount 1..N does not generate maxItems")
        void arrayWithOneToNNoMaxItems() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("items")
                    .className("Item")
                    .isArray(true)
                    .occurrenceCount("1..N")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("maxItems"));
        }

        @Test
        @DisplayName("Array with occurrenceCount 0..* does not generate maxItems")
        void arrayWithAsteriskNoMaxItems() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("items")
                    .className("Item")
                    .isArray(true)
                    .occurrenceCount("0..*")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("maxItems"));
        }
    }

    @Nested
    @DisplayName("Object Type Mapping")
    class ObjectTypeMappingTests {

        @Test
        @DisplayName("Object field generates $ref")
        void objectFieldGeneratesRef() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("createApp")
                    .className("CreateApplication")
                    .isObject(true)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("./CreateApplication.yaml", schema.get("$ref"));
            assertNull(schema.get("type"));
        }
    }

    @Nested
    @DisplayName("maxLength Constraint")
    class MaxLengthTests {

        @Test
        @DisplayName("Field with length generates maxLength")
        void fieldWithLengthGeneratesMaxLength() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("accountNumber")
                    .dataType("String")
                    .length(20)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals(20, schema.get("maxLength"));
        }

        @Test
        @DisplayName("Field with null length does not generate maxLength")
        void fieldWithNullLengthNoMaxLength() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("description")
                    .dataType("String")
                    .length(null)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("maxLength"));
        }

        @Test
        @DisplayName("Field with zero length does not generate maxLength")
        void fieldWithZeroLengthNoMaxLength() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("field")
                    .dataType("String")
                    .length(0)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("maxLength"));
        }
    }

    @Nested
    @DisplayName("Default Value")
    class DefaultValueTests {

        @Test
        @DisplayName("Field with defaultValue generates default")
        void fieldWithDefaultValueGeneratesDefault() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("status")
                    .dataType("String")
                    .defaultValue("ACTIVE")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertEquals("ACTIVE", schema.get("default"));
        }

        @Test
        @DisplayName("Field with null defaultValue does not generate default")
        void fieldWithNullDefaultNoDefault() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("field")
                    .dataType("String")
                    .defaultValue(null)
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("default"));
        }

        @Test
        @DisplayName("Field with empty defaultValue does not generate default")
        void fieldWithEmptyDefaultNoDefault() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("field")
                    .dataType("String")
                    .defaultValue("")
                    .build();

            Map<String, Object> schema = mapper.mapToSchema(field);

            assertNull(schema.get("default"));
        }
    }

    @Nested
    @DisplayName("Transitory Field Filtering")
    class TransitoryFieldTests {

        @Test
        @DisplayName("Transitory fields are filtered out")
        void transitoryFieldsAreFiltered() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().camelCaseName("normalField").isTransitory(false).build(),
                    FieldNode.builder().camelCaseName("groupId").isTransitory(true).build(),
                    FieldNode.builder().camelCaseName("occurrenceCount").isTransitory(true).build(),
                    FieldNode.builder().camelCaseName("anotherField").isTransitory(false).build()
            );

            List<FieldNode> filtered = mapper.filterTransitoryFields(fields);

            assertEquals(2, filtered.size());
            assertEquals("normalField", filtered.get(0).getCamelCaseName());
            assertEquals("anotherField", filtered.get(1).getCamelCaseName());
        }

        @Test
        @DisplayName("Empty list returns empty list")
        void emptyListReturnsEmptyList() {
            List<FieldNode> filtered = mapper.filterTransitoryFields(new ArrayList<>());

            assertTrue(filtered.isEmpty());
        }

        @Test
        @DisplayName("shouldIncludeInSchema returns false for transitory")
        void shouldIncludeReturnsFalseForTransitory() {
            FieldNode transitory = FieldNode.builder()
                    .camelCaseName("groupId")
                    .isTransitory(true)
                    .build();

            assertFalse(mapper.shouldIncludeInSchema(transitory));
        }

        @Test
        @DisplayName("shouldIncludeInSchema returns true for non-transitory")
        void shouldIncludeReturnsTrueForNonTransitory() {
            FieldNode normal = FieldNode.builder()
                    .camelCaseName("normalField")
                    .isTransitory(false)
                    .build();

            assertTrue(mapper.shouldIncludeInSchema(normal));
        }
    }

    @Nested
    @DisplayName("Required Fields Collection")
    class RequiredFieldsTests {

        @Test
        @DisplayName("Mandatory fields are collected in required list")
        void mandatoryFieldsCollectedInRequired() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().camelCaseName("mandatoryField").optionality("M").build(),
                    FieldNode.builder().camelCaseName("optionalField").optionality("O").build(),
                    FieldNode.builder().camelCaseName("anotherMandatory").optionality("M").build()
            );

            List<String> required = mapper.collectRequiredFields(fields);

            assertEquals(2, required.size());
            assertEquals("mandatoryField", required.get(0));
            assertEquals("anotherMandatory", required.get(1));
        }

        @Test
        @DisplayName("Transitory mandatory fields are excluded from required")
        void transitoryMandatoryExcludedFromRequired() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().camelCaseName("mandatoryField").optionality("M").isTransitory(false).build(),
                    FieldNode.builder().camelCaseName("transitoryMandatory").optionality("M").isTransitory(true).build()
            );

            List<String> required = mapper.collectRequiredFields(fields);

            assertEquals(1, required.size());
            assertEquals("mandatoryField", required.get(0));
        }

        @Test
        @DisplayName("Empty list returns empty required list")
        void emptyListReturnsEmptyRequired() {
            List<String> required = mapper.collectRequiredFields(new ArrayList<>());

            assertTrue(required.isEmpty());
        }

        @Test
        @DisplayName("All optional fields returns empty required list")
        void allOptionalReturnsEmptyRequired() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().camelCaseName("optionalA").optionality("O").build(),
                    FieldNode.builder().camelCaseName("optionalB").optionality("O").build()
            );

            List<String> required = mapper.collectRequiredFields(fields);

            assertTrue(required.isEmpty());
        }
    }

    @Nested
    @DisplayName("Object Schema Generation")
    class ObjectSchemaGenerationTests {

        @Test
        @DisplayName("generateObjectSchema produces correct structure")
        void generateObjectSchemaProducesCorrectStructure() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder()
                            .camelCaseName("createApp")
                            .className("CreateApplication")
                            .isObject(true)
                            .optionality("M")
                            .build(),
                    FieldNode.builder()
                            .camelCaseName("productDel")
                            .className("ProductDetails")
                            .isObject(true)
                            .optionality("O")
                            .build()
            );

            Map<String, Object> schema = mapper.generateObjectSchema(fields);

            assertEquals("object", schema.get("type"));

            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) schema.get("required");
            assertEquals(1, required.size());
            assertEquals("createApp", required.get(0));

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertEquals(2, properties.size());
            assertTrue(properties.containsKey("createApp"));
            assertTrue(properties.containsKey("productDel"));
        }

        @Test
        @DisplayName("generateObjectSchema excludes transitory fields from properties")
        void generateObjectSchemaExcludesTransitoryFromProperties() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder()
                            .camelCaseName("normalField")
                            .dataType("String")
                            .isTransitory(false)
                            .build(),
                    FieldNode.builder()
                            .camelCaseName("groupId")
                            .dataType("String")
                            .isTransitory(true)
                            .build()
            );

            Map<String, Object> schema = mapper.generateObjectSchema(fields);

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertEquals(1, properties.size());
            assertTrue(properties.containsKey("normalField"));
            assertFalse(properties.containsKey("groupId"));
        }

        @Test
        @DisplayName("generateObjectSchema with no required fields omits required key")
        void generateObjectSchemaNoRequiredOmitsKey() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder()
                            .camelCaseName("optionalField")
                            .dataType("String")
                            .optionality("O")
                            .build()
            );

            Map<String, Object> schema = mapper.generateObjectSchema(fields);

            assertNull(schema.get("required"));
        }

        @Test
        @DisplayName("Field order is preserved in properties")
        void fieldOrderPreservedInProperties() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().camelCaseName("first").dataType("String").build(),
                    FieldNode.builder().camelCaseName("second").dataType("String").build(),
                    FieldNode.builder().camelCaseName("third").dataType("String").build()
            );

            Map<String, Object> schema = mapper.generateObjectSchema(fields);

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            List<String> keys = new ArrayList<>(properties.keySet());
            assertEquals(Arrays.asList("first", "second", "third"), keys);
        }
    }

    @Nested
    @DisplayName("Ref Path Generation")
    class RefPathGenerationTests {

        @Test
        @DisplayName("generateRefPath produces correct path")
        void generateRefPathProducesCorrectPath() {
            String refPath = mapper.generateRefPath("CreateApplication", "request");

            assertEquals("./CreateApplication.yaml", refPath);
        }
    }

    @Nested
    @DisplayName("Null Config Handling")
    class NullConfigTests {

        @Test
        @DisplayName("Mapper works with null config")
        void mapperWorksWithNullConfig() {
            OpenApiTypeMapper nullConfigMapper = new OpenApiTypeMapper(null);
            FieldNode field = FieldNode.builder()
                    .camelCaseName("field")
                    .dataType("String")
                    .build();

            Map<String, Object> schema = nullConfigMapper.mapToSchema(field);

            assertEquals("string", schema.get("type"));
        }
    }
}
