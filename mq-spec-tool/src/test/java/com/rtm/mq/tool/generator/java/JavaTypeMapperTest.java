package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaTypeMapper.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Basic type mappings (String, Number, Amount, etc.)</li>
 *   <li>Array type mapping (List&lt;T&gt;)</li>
 *   <li>Object type mapping</li>
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
    @DisplayName("Amount dataType maps to BigDecimal")
    void amountTypeMappsToBigDecimal() {
        FieldNode field = FieldNode.builder()
                .dataType("Amount")
                .camelCaseName("transactionAmount")
                .build();

        assertEquals("java.math.BigDecimal", mapper.mapType(field));
    }

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
}
