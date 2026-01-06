package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.JavaConfig;
import com.rtm.mq.tool.config.XmlConfig;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaTypeMapper.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Basic type mappings (String, Number, Amount, etc.)</li>
 *   <li>Array type mapping (List&lt;T&gt;)</li>
 *   <li>Object type mapping</li>
 *   <li>Import collection</li>
 *   <li>Default type handling</li>
 *   <li>Package name resolution</li>
 * </ul>
 */
class JavaTypeMapperTest {

    private Config config;
    private JavaTypeMapper mapper;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
        mapper = new JavaTypeMapper(config);
    }

    @Nested
    @DisplayName("Basic Type Mapping Tests")
    class BasicTypeMappingTests {

        @Test
        @DisplayName("String dataType maps to String")
        void stringTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("String")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("AN dataType maps to String")
        void anTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("AN")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("A/N dataType maps to String")
        void aSlashNTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("A/N")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("Number dataType maps to String")
        void numberTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("Number")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("N dataType maps to String")
        void nTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("N")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("Unsigned Integer dataType maps to String")
        void unsignedIntegerTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("Unsigned Integer")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("Date dataType maps to String")
        void dateTypeMapsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("Date")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }
    }

    @Nested
    @DisplayName("BigDecimal Type Mapping Tests")
    class BigDecimalTypeMappingTests {

        @Test
        @DisplayName("Amount dataType maps to BigDecimal")
        void amountTypeMappsToBigDecimal() {
            FieldNode field = FieldNode.builder()
                    .dataType("Amount")
                    .camelCaseName("transactionAmount")
                    .build();

            assertEquals("java.math.BigDecimal", mapper.mapType(field));
        }

        @Test
        @DisplayName("Currency dataType maps to BigDecimal")
        void currencyTypeMappsToBigDecimal() {
            FieldNode field = FieldNode.builder()
                    .dataType("Currency")
                    .camelCaseName("exchangeRate")
                    .build();

            assertEquals("java.math.BigDecimal", mapper.mapType(field));
        }

        @Test
        @DisplayName("BigDecimal simple type name is BigDecimal")
        void bigDecimalSimpleTypeName() {
            FieldNode field = FieldNode.builder()
                    .dataType("Amount")
                    .camelCaseName("amount")
                    .build();

            assertEquals("BigDecimal", mapper.getSimpleTypeName(field));
        }

        @Test
        @DisplayName("isBigDecimalType returns true for Amount")
        void isBigDecimalTypeForAmount() {
            assertTrue(mapper.isBigDecimalType("Amount"));
        }

        @Test
        @DisplayName("isBigDecimalType returns true for Currency")
        void isBigDecimalTypeForCurrency() {
            assertTrue(mapper.isBigDecimalType("Currency"));
        }

        @Test
        @DisplayName("isBigDecimalType returns false for String")
        void isBigDecimalTypeForString() {
            assertFalse(mapper.isBigDecimalType("String"));
        }

        @Test
        @DisplayName("isBigDecimalType returns false for null")
        void isBigDecimalTypeForNull() {
            assertFalse(mapper.isBigDecimalType(null));
        }
    }

    @Nested
    @DisplayName("Array Type Mapping Tests")
    class ArrayTypeMappingTests {

        @Test
        @DisplayName("Array field maps to List<ClassName>")
        void arrayFieldMapsToList() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .className("TransactionItem")
                    .camelCaseName("items")
                    .build();

            assertEquals("java.util.List<TransactionItem>", mapper.mapType(field));
        }

        @Test
        @DisplayName("Array field without className uses capitalized camelCaseName")
        void arrayFieldWithoutClassNameUsesCapitalizedName() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .camelCaseName("transactionItems")
                    .build();

            assertEquals("java.util.List<TransactionItems>", mapper.mapType(field));
        }

        @Test
        @DisplayName("Array field simple type is List<ClassName>")
        void arrayFieldSimpleTypeName() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .className("Item")
                    .camelCaseName("items")
                    .build();

            assertEquals("List<Item>", mapper.getSimpleTypeName(field));
        }

        @Test
        @DisplayName("Array field has default initializer")
        void arrayFieldHasDefaultInitializer() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .className("Item")
                    .camelCaseName("items")
                    .build();

            assertEquals("new java.util.ArrayList<>()", mapper.getDefaultInitializer(field));
        }

        @Test
        @DisplayName("Array field has simple initializer")
        void arrayFieldHasSimpleInitializer() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .className("Item")
                    .camelCaseName("items")
                    .build();

            assertEquals("new ArrayList<>()", mapper.getSimpleInitializer(field));
        }
    }

    @Nested
    @DisplayName("Object Type Mapping Tests")
    class ObjectTypeMappingTests {

        @Test
        @DisplayName("Object field maps to ClassName")
        void objectFieldMapsToClassName() {
            FieldNode field = FieldNode.builder()
                    .isObject(true)
                    .className("AccountInfo")
                    .camelCaseName("account")
                    .build();

            assertEquals("AccountInfo", mapper.mapType(field));
        }

        @Test
        @DisplayName("Object field without className uses capitalized camelCaseName")
        void objectFieldWithoutClassNameUsesCapitalizedName() {
            FieldNode field = FieldNode.builder()
                    .isObject(true)
                    .camelCaseName("accountInfo")
                    .build();

            assertEquals("AccountInfo", mapper.mapType(field));
        }

        @Test
        @DisplayName("Object field has no default initializer")
        void objectFieldHasNoDefaultInitializer() {
            FieldNode field = FieldNode.builder()
                    .isObject(true)
                    .className("AccountInfo")
                    .camelCaseName("account")
                    .build();

            assertNull(mapper.getDefaultInitializer(field));
        }
    }

    @Nested
    @DisplayName("Default Type Handling Tests")
    class DefaultTypeHandlingTests {

        @Test
        @DisplayName("Null dataType defaults to String")
        void nullDataTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("Empty dataType defaults to String")
        void emptyDataTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }

        @Test
        @DisplayName("Unknown dataType defaults to String")
        void unknownDataTypeDefaultsToString() {
            FieldNode field = FieldNode.builder()
                    .dataType("UnknownType")
                    .camelCaseName("testField")
                    .build();

            assertEquals("String", mapper.mapType(field));
        }
    }

    @Nested
    @DisplayName("Import Collection Tests")
    class ImportCollectionTests {

        @Test
        @DisplayName("String field requires no imports")
        void stringFieldRequiresNoImports() {
            FieldNode field = FieldNode.builder()
                    .dataType("String")
                    .camelCaseName("name")
                    .build();

            Set<String> imports = mapper.collectImports(field);
            assertTrue(imports.isEmpty());
        }

        @Test
        @DisplayName("BigDecimal field requires BigDecimal import")
        void bigDecimalFieldRequiresBigDecimalImport() {
            FieldNode field = FieldNode.builder()
                    .dataType("Amount")
                    .camelCaseName("amount")
                    .build();

            Set<String> imports = mapper.collectImports(field);
            assertTrue(imports.contains("java.math.BigDecimal"));
            assertEquals(1, imports.size());
        }

        @Test
        @DisplayName("Array field requires List import")
        void arrayFieldRequiresListImport() {
            FieldNode field = FieldNode.builder()
                    .isArray(true)
                    .className("Item")
                    .camelCaseName("items")
                    .build();

            Set<String> imports = mapper.collectImports(field);
            assertTrue(imports.contains("java.util.List"));
            assertEquals(1, imports.size());
        }

        @Test
        @DisplayName("collectAllImports collects from multiple fields without duplicates")
        void collectAllImportsNoDuplicates() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().dataType("Amount").camelCaseName("amount1").build(),
                    FieldNode.builder().dataType("Currency").camelCaseName("amount2").build(),
                    FieldNode.builder().isArray(true).className("Item").camelCaseName("items").build(),
                    FieldNode.builder().dataType("String").camelCaseName("name").build()
            );

            Set<String> imports = mapper.collectAllImports(fields);
            assertEquals(2, imports.size());
            assertTrue(imports.contains("java.math.BigDecimal"));
            assertTrue(imports.contains("java.util.List"));
        }

        @Test
        @DisplayName("collectAllImports returns sorted imports")
        void collectAllImportsAreSorted() {
            List<FieldNode> fields = Arrays.asList(
                    FieldNode.builder().isArray(true).className("Item").camelCaseName("items").build(),
                    FieldNode.builder().dataType("Amount").camelCaseName("amount").build()
            );

            Set<String> imports = mapper.collectAllImports(fields);
            String[] importArray = imports.toArray(new String[0]);

            // TreeSet should sort alphabetically
            assertEquals("java.math.BigDecimal", importArray[0]);
            assertEquals("java.util.List", importArray[1]);
        }
    }

    @Nested
    @DisplayName("Package Name Resolution Tests")
    class PackageNameResolutionTests {

        @Test
        @DisplayName("getModelPackage returns JavaConfig.packageName when set")
        void getModelPackageFromJavaConfig() {
            JavaConfig javaConfig = new JavaConfig();
            javaConfig.setPackageName("com.example.myapp.model");
            config.setJava(javaConfig);
            mapper = new JavaTypeMapper(config);

            assertEquals("com.example.myapp.model", mapper.getModelPackage());
        }

        @Test
        @DisplayName("getModelPackage falls back to xml.project when JavaConfig.packageName is empty")
        void getModelPackageFallbackToXmlProject() {
            JavaConfig javaConfig = new JavaConfig();
            javaConfig.setPackageName("");
            config.setJava(javaConfig);

            XmlConfig xmlConfig = new XmlConfig();
            XmlConfig.ProjectConfig projectConfig = new XmlConfig.ProjectConfig();
            projectConfig.setGroupId("com.rtm");
            projectConfig.setArtifactId("test");
            xmlConfig.setProject(projectConfig);
            config.setXml(xmlConfig);

            mapper = new JavaTypeMapper(config);

            assertEquals("com.rtm.test.model", mapper.getModelPackage());
        }

        @Test
        @DisplayName("getModelPackage returns default when no config is set")
        void getModelPackageDefaultValue() {
            config = new Config();
            mapper = new JavaTypeMapper(config);

            assertEquals("com.rtm.mq.model", mapper.getModelPackage());
        }
    }

    @Nested
    @DisplayName("Requires Import Tests")
    class RequiresImportTests {

        @Test
        @DisplayName("java.util.List requires import")
        void javaUtilListRequiresImport() {
            assertTrue(mapper.requiresImport("java.util.List"));
        }

        @Test
        @DisplayName("java.math.BigDecimal requires import")
        void javaMathBigDecimalRequiresImport() {
            assertTrue(mapper.requiresImport("java.math.BigDecimal"));
        }

        @Test
        @DisplayName("String does not require import")
        void stringDoesNotRequireImport() {
            assertFalse(mapper.requiresImport("String"));
        }

        @Test
        @DisplayName("Simple class name does not require import")
        void simpleClassNameDoesNotRequireImport() {
            assertFalse(mapper.requiresImport("AccountInfo"));
        }
    }

    @Nested
    @DisplayName("Simple Type Name Tests")
    class SimpleTypeNameTests {

        @Test
        @DisplayName("Simple type for String is String")
        void simpleTypeForString() {
            FieldNode field = FieldNode.builder()
                    .dataType("String")
                    .camelCaseName("name")
                    .build();

            assertEquals("String", mapper.getSimpleTypeName(field));
        }

        @Test
        @DisplayName("Simple type for object is ClassName")
        void simpleTypeForObject() {
            FieldNode field = FieldNode.builder()
                    .isObject(true)
                    .className("AccountInfo")
                    .camelCaseName("account")
                    .build();

            assertEquals("AccountInfo", mapper.getSimpleTypeName(field));
        }
    }

    @Test
    @DisplayName("Type map is deterministic and contains expected entries")
    void typeMapIsDeterministic() {
        var typeMap = JavaTypeMapper.getTypeMap();

        assertEquals("String", typeMap.get("String"));
        assertEquals("String", typeMap.get("AN"));
        assertEquals("String", typeMap.get("Number"));
        assertEquals("String", typeMap.get("N"));
        assertEquals("String", typeMap.get("Date"));
        assertEquals("java.math.BigDecimal", typeMap.get("Amount"));
        assertEquals("java.math.BigDecimal", typeMap.get("Currency"));
    }

    @Nested
    @DisplayName("Enum Type Detection Tests")
    class EnumTypeDetectionTests {

        @Test
        @DisplayName("isEnumType returns true for field with pipe-separated enumConstraint")
        void isEnumTypeReturnsTrueForValidConstraint() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("status")
                    .enumConstraint("01|02|03")
                    .build();

            assertTrue(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns true for Y|N constraint")
        void isEnumTypeReturnsTrueForYesNo() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("flag")
                    .enumConstraint("Y|N")
                    .build();

            assertTrue(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns false for null enumConstraint")
        void isEnumTypeReturnsFalseForNullConstraint() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("name")
                    .build();

            assertFalse(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns false for empty enumConstraint")
        void isEnumTypeReturnsFalseForEmptyConstraint() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("name")
                    .enumConstraint("")
                    .build();

            assertFalse(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns false for blank enumConstraint")
        void isEnumTypeReturnsFalseForBlankConstraint() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("name")
                    .enumConstraint("   ")
                    .build();

            assertFalse(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns false for constraint without pipe")
        void isEnumTypeReturnsFalseForConstraintWithoutPipe() {
            FieldNode field = FieldNode.builder()
                    .camelCaseName("name")
                    .enumConstraint("singleValue")
                    .build();

            assertFalse(mapper.isEnumType(field));
        }

        @Test
        @DisplayName("isEnumType returns false for null field")
        void isEnumTypeReturnsFalseForNullField() {
            assertFalse(mapper.isEnumType(null));
        }
    }
}
