package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.XmlConfig;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for XmlTypeMapper.
 */
public class XmlTypeMapperTest {

    private XmlTypeMapper mapper;
    private Config config;

    @Before
    public void setUp() {
        config = new Config();
        config.setDefaults();

        // Set up project configuration for forType generation
        XmlConfig.ProjectConfig projectConfig = new XmlConfig.ProjectConfig();
        projectConfig.setGroupId("com.example");
        projectConfig.setArtifactId("mq-beans");
        config.getXml().setProject(projectConfig);

        mapper = new XmlTypeMapper(config);
    }

    @Test
    public void testMapGroupIdField() {
        FieldNode node = FieldNode.builder()
            .isTransitory(true)
            .groupId("GRP001")
            .length(10)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("10", map.get("length"));
        assertEquals("true", map.get("fixedLength"));
        assertEquals("true", map.get("transitory"));
        assertEquals("GRP001", map.get("defaultValue"));
        assertEquals("stringFieldConverter", map.get("converter"));
    }

    @Test
    public void testMapGroupIdFieldWithDefaultLength() {
        FieldNode node = FieldNode.builder()
            .isTransitory(true)
            .groupId("ABC")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals("10", map.get("length"));
        assertEquals("ABC", map.get("defaultValue"));
    }

    @Test
    public void testMapOccurrenceCountField() {
        FieldNode node = FieldNode.builder()
            .isTransitory(true)
            .occurrenceCount("0..9")
            .length(4)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("4", map.get("length"));
        assertEquals("true", map.get("fixedLength"));
        assertEquals("true", map.get("transitory"));
        assertEquals("9", map.get("defaultValue"));
        assertEquals("0", map.get("pad"));
        assertEquals("true", map.get("alignRight"));
        assertEquals("counterFieldConverter", map.get("converter"));
    }

    @Test
    public void testMapOccurrenceCountFieldWithDefaultLength() {
        FieldNode node = FieldNode.builder()
            .isTransitory(true)
            .occurrenceCount("1..99")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals("4", map.get("length"));
        assertEquals("99", map.get("defaultValue"));
    }

    @Test
    public void testMapCompositeField() {
        FieldNode node = FieldNode.builder()
            .isObject(true)
            .camelCaseName("customer")
            .className("Customer")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.COMPOSITE_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("customer", map.get("name"));
        assertEquals("com.example.mq-beans.Customer", map.get("forType"));
    }

    @Test
    public void testMapRepeatingField() {
        FieldNode node = FieldNode.builder()
            .isArray(true)
            .camelCaseName("transactions")
            .className("Transaction")
            .occurrenceCount("0..9")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.REPEATING_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("transactions", map.get("name"));
        assertEquals("9", map.get("fixedCount"));
        assertEquals("com.example.mq-beans.Transaction", map.get("forType"));
    }

    @Test
    public void testMapDataFieldString() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("accountNumber")
            .dataType("String")
            .length(20)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("accountNumber", map.get("name"));
        assertEquals("20", map.get("length"));
        assertEquals(" ", map.get("nullPad"));
        assertEquals("stringFieldConverter", map.get("converter"));
        assertNull(map.get("pad"));
        assertNull(map.get("alignRight"));
    }

    @Test
    public void testMapDataFieldNumeric() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("transactionCount")
            .dataType("Number")
            .length(10)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("transactionCount", map.get("name"));
        assertEquals("10", map.get("length"));
        assertEquals("0", map.get("pad"));
        assertEquals("true", map.get("alignRight"));
        assertEquals("stringFieldConverter", map.get("converter"));
    }

    @Test
    public void testMapDataFieldUnsignedInteger() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("count")
            .dataType("Unsigned Integer")
            .length(8)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals("0", map.get("pad"));
        assertEquals("true", map.get("alignRight"));
        assertEquals("stringFieldConverter", map.get("converter"));
    }

    @Test
    public void testMapDataFieldAmount() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("totalAmount")
            .dataType("Amount")
            .length(15)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("totalAmount", map.get("name"));
        assertEquals("15", map.get("length"));
        assertEquals("OHcurrencyamountFieldConverter", map.get("converter"));
        assertEquals("java.math.BigDecimal", map.get("forType"));
    }

    @Test
    public void testMapDataFieldWithDefaultValue() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("status")
            .dataType("String")
            .length(5)
            .defaultValue("ACTIV")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals("ACTIV", map.get("defaultValue"));
    }

    @Test
    public void testMapDataFieldWithoutDefaultValue() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("status")
            .dataType("String")
            .length(5)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertNull(map.get("defaultValue"));
    }

    @Test
    public void testMappingPriority_GroupIdOverOccurrence() {
        // If both groupId and occurrenceCount are set with transitory,
        // groupId takes priority
        FieldNode node = FieldNode.builder()
            .isTransitory(true)
            .groupId("GRP001")
            .occurrenceCount("0..9")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals("GRP001", map.get("defaultValue"));
        assertEquals("stringFieldConverter", map.get("converter"));
        assertNull(map.get("fixedCount"));
    }

    @Test
    public void testMappingPriority_ArrayOverObject() {
        // If both isArray and isObject are set, array takes priority
        FieldNode node = FieldNode.builder()
            .isArray(true)
            .isObject(true)
            .camelCaseName("items")
            .className("Item")
            .occurrenceCount("0..9")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        assertEquals(XmlFieldType.REPEATING_FIELD, attrs.getType());
        Map<String, String> map = attrs.getAttributes();
        assertEquals("9", map.get("fixedCount"));
    }

    @Test
    public void testDataTypeNormalization() {
        // Test case-insensitive matching for "N"
        FieldNode node1 = FieldNode.builder()
            .camelCaseName("field1")
            .dataType("n")
            .length(5)
            .build();

        XmlFieldAttributes attrs1 = mapper.map(node1);
        assertEquals("0", attrs1.getAttributes().get("pad"));
        assertEquals("true", attrs1.getAttributes().get("alignRight"));

        // Test lowercase "amount"
        FieldNode node2 = FieldNode.builder()
            .camelCaseName("field2")
            .dataType("amount")
            .length(10)
            .build();

        XmlFieldAttributes attrs2 = mapper.map(node2);
        assertEquals("OHcurrencyamountFieldConverter", attrs2.getAttributes().get("converter"));
        assertEquals("java.math.BigDecimal", attrs2.getAttributes().get("forType"));
    }

    @Test
    public void testNullDataType() {
        FieldNode node = FieldNode.builder()
            .camelCaseName("field")
            .length(10)
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        Map<String, String> map = attrs.getAttributes();
        assertEquals(" ", map.get("nullPad"));
        assertEquals("stringFieldConverter", map.get("converter"));
        assertNull(map.get("pad"));
        assertNull(map.get("alignRight"));
    }

    @Test
    public void testForTypeBuildFormat() {
        FieldNode node = FieldNode.builder()
            .isObject(true)
            .camelCaseName("myObject")
            .className("MyClassName")
            .build();

        XmlFieldAttributes attrs = mapper.map(node);

        // Verify format: groupId.artifactId.className
        assertEquals("com.example.mq-beans.MyClassName", attrs.getAttributes().get("forType"));
    }
}
