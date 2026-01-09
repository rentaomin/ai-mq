package com.rtm.mq.tool.generator.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for ConverterMapper.
 */
public class ConverterMapperTest {

    private ConverterMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ConverterMapper();
    }

    @Test
    public void testStringDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("String"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("string"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("STRING"));
    }

    @Test
    public void testAlphanumericDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("AN"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("an"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("A/N"));
    }

    @Test
    public void testNumericDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("Number"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("number"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("N"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("n"));
    }

    @Test
    public void testUnsignedIntegerDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("Unsigned Integer"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("unsigned integer"));
    }

    @Test
    public void testAmountDataType() {
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("Amount"));
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("amount"));
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("AMOUNT"));
    }

    @Test
    public void testCurrencyDataType() {
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("Currency"));
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("currency"));
    }

    @Test
    public void testNullDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter(null));
    }

    @Test
    public void testEmptyDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter(""));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("   "));
    }

    @Test
    public void testUnknownDataType() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("UnknownType"));
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("XYZ"));
    }

    @Test
    public void testGetForTypeWithAmount() {
        assertEquals("java.math.BigDecimal", mapper.getForType("Amount"));
        assertEquals("java.math.BigDecimal", mapper.getForType("amount"));
        assertEquals("java.math.BigDecimal", mapper.getForType("AMOUNT"));
    }

    @Test
    public void testGetForTypeWithCurrency() {
        assertEquals("java.math.BigDecimal", mapper.getForType("Currency"));
        assertEquals("java.math.BigDecimal", mapper.getForType("currency"));
    }

    @Test
    public void testGetForTypeWithString() {
        assertNull(mapper.getForType("String"));
        assertNull(mapper.getForType("string"));
    }

    @Test
    public void testGetForTypeWithNumber() {
        assertNull(mapper.getForType("Number"));
        assertNull(mapper.getForType("N"));
    }

    @Test
    public void testGetForTypeWithNull() {
        assertNull(mapper.getForType(null));
    }

    @Test
    public void testGetForTypeWithUnknown() {
        assertNull(mapper.getForType("UnknownType"));
    }

    @Test
    public void testConverterConstants() {
        assertEquals("stringFieldConverter", ConverterMapper.STRING_CONVERTER);
        assertEquals("counterFieldConverter", ConverterMapper.COUNTER_CONVERTER);
        assertEquals("OHcurrencyamountFieldConverter", ConverterMapper.CURRENCY_CONVERTER);
        assertEquals("nlsStringFieldConverter", ConverterMapper.NLS_CONVERTER);
        assertEquals("OHunsignedlongFieldConverter", ConverterMapper.UNSIGNED_LONG_CONVERTER);
        assertEquals("OHunsignedintFieldConverter", ConverterMapper.UNSIGNED_INT_CONVERTER);
    }

    @Test
    public void testDataTypeWithWhitespace() {
        assertEquals(ConverterMapper.STRING_CONVERTER, mapper.getConverter("  String  "));
        assertEquals(ConverterMapper.CURRENCY_CONVERTER, mapper.getConverter("  Amount  "));
    }
}
