package com.rtm.mq.tool.generator.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XmlFormatter.
 *
 * <p>Tests cover acceptance criteria:</p>
 * <ul>
 *   <li>AC1: XML declaration is correct</li>
 *   <li>AC5: Attribute values are properly escaped</li>
 *   <li>AC6: Output uses 2-space indentation</li>
 *   <li>AC7: Empty elements use self-closing tags</li>
 * </ul>
 */
@DisplayName("XmlFormatter Tests")
class XmlFormatterTest {

    private XmlFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new XmlFormatter();
    }

    @Nested
    @DisplayName("XML Declaration Tests (AC1)")
    class XmlDeclarationTests {

        @Test
        @DisplayName("Should include correct XML declaration")
        void shouldIncludeCorrectXmlDeclaration() {
            XmlElement root = new XmlElement("root");

            String result = formatter.format(root);

            assertTrue(result.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        }

        @Test
        @DisplayName("Should return declaration only for null root")
        void shouldReturnDeclarationOnlyForNullRoot() {
            String result = formatter.format(null);

            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", result);
        }

        @Test
        @DisplayName("Should format without declaration when requested")
        void shouldFormatWithoutDeclarationWhenRequested() {
            XmlElement root = new XmlElement("root");

            String result = formatter.formatWithoutDeclaration(root);

            assertFalse(result.contains("<?xml"));
            assertTrue(result.contains("<root"));
        }
    }

    @Nested
    @DisplayName("Attribute Escaping Tests (AC5)")
    class AttributeEscapingTests {

        @Test
        @DisplayName("Should escape ampersand in attribute value")
        void shouldEscapeAmpersandInAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "A&B");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"A&amp;B\""));
        }

        @Test
        @DisplayName("Should escape less-than in attribute value")
        void shouldEscapeLessThanInAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "A<B");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"A&lt;B\""));
        }

        @Test
        @DisplayName("Should escape greater-than in attribute value")
        void shouldEscapeGreaterThanInAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "A>B");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"A&gt;B\""));
        }

        @Test
        @DisplayName("Should escape double quote in attribute value")
        void shouldEscapeDoubleQuoteInAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "A\"B");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"A&quot;B\""));
        }

        @Test
        @DisplayName("Should escape apostrophe in attribute value")
        void shouldEscapeApostropheInAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "A'B");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"A&apos;B\""));
        }

        @Test
        @DisplayName("Should escape multiple special characters")
        void shouldEscapeMultipleSpecialCharacters() {
            XmlElement element = new XmlElement("field")
                .attribute("value", "<tag attr=\"val\"> & 'test'");

            String result = formatter.format(element);

            assertTrue(result.contains("value=\"&lt;tag attr=&quot;val&quot;&gt; &amp; &apos;test&apos;\""));
        }
    }

    @Nested
    @DisplayName("Text Content Escaping Tests")
    class TextContentEscapingTests {

        @Test
        @DisplayName("Should escape special characters in text content")
        void shouldEscapeSpecialCharactersInTextContent() {
            XmlElement element = new XmlElement("value")
                .text("<script>alert('test')</script>");

            String result = formatter.format(element);

            assertTrue(result.contains("&lt;script&gt;alert(&apos;test&apos;)&lt;/script&gt;"));
        }
    }

    @Nested
    @DisplayName("Indentation Tests (AC6)")
    class IndentationTests {

        @Test
        @DisplayName("Should use 2-space indentation")
        void shouldUseTwoSpaceIndentation() {
            XmlElement root = new XmlElement("root");
            XmlElement child = new XmlElement("child");
            root.addChild(child);

            String result = formatter.format(root);

            // Child should be indented with 2 spaces
            assertTrue(result.contains("\n  <child"));
        }

        @Test
        @DisplayName("Should indent nested elements correctly")
        void shouldIndentNestedElementsCorrectly() {
            XmlElement root = new XmlElement("level0");
            XmlElement level1 = new XmlElement("level1");
            XmlElement level2 = new XmlElement("level2");
            XmlElement level3 = new XmlElement("level3");

            level2.addChild(level3);
            level1.addChild(level2);
            root.addChild(level1);

            String result = formatter.format(root);

            // Verify indentation pattern
            assertTrue(result.contains("<level0"));          // No indent for root
            assertTrue(result.contains("\n  <level1"));      // 2 spaces
            assertTrue(result.contains("\n    <level2"));    // 4 spaces
            assertTrue(result.contains("\n      <level3"));  // 6 spaces
        }

        @Test
        @DisplayName("Should indent closing tags correctly")
        void shouldIndentClosingTagsCorrectly() {
            XmlElement root = new XmlElement("root");
            XmlElement child = new XmlElement("child");
            XmlElement grandchild = new XmlElement("grandchild");
            child.addChild(grandchild);
            root.addChild(child);

            String result = formatter.format(root);

            // Closing tags should match opening tag indentation
            assertTrue(result.contains("\n  </child>"));
            assertTrue(result.contains("\n</root>"));
        }
    }

    @Nested
    @DisplayName("Self-Closing Tag Tests (AC7)")
    class SelfClosingTagTests {

        @Test
        @DisplayName("Should use self-closing tag for empty element")
        void shouldUseSelfClosingTagForEmptyElement() {
            XmlElement element = new XmlElement("field")
                .attribute("name", "test");

            String result = formatter.format(element);

            assertTrue(result.contains("<field name=\"test\" />"));
            assertFalse(result.contains("</field>"));
        }

        @Test
        @DisplayName("Should use self-closing tag for element with only attributes")
        void shouldUseSelfClosingTagForElementWithOnlyAttributes() {
            XmlElement element = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "accountNumber")
                .attribute("length", "16");

            String result = formatter.format(element);

            assertTrue(result.contains(" />"));
            assertFalse(result.contains("</field>"));
        }

        @Test
        @DisplayName("Should not use self-closing tag for element with children")
        void shouldNotUseSelfClosingTagForElementWithChildren() {
            XmlElement parent = new XmlElement("message");
            parent.addChild(new XmlElement("field"));

            String result = formatter.format(parent);

            assertTrue(result.contains("</message>"));
            assertFalse(result.contains("<message />") && !result.contains("<message>"));
        }

        @Test
        @DisplayName("Should not use self-closing tag for element with text")
        void shouldNotUseSelfClosingTagForElementWithText() {
            XmlElement element = new XmlElement("value")
                .text("content");

            String result = formatter.format(element);

            assertTrue(result.contains("<value>content</value>"));
            assertFalse(result.contains("<value />"));
        }
    }

    @Nested
    @DisplayName("Complex Structure Tests")
    class ComplexStructureTests {

        @Test
        @DisplayName("Should format complete XML bean structure")
        void shouldFormatCompleteXmlBeanStructure() {
            XmlElement beans = new XmlElement("beans:beans")
                .attribute("xmlns", "http://example.com/schema");

            XmlElement converter = new XmlElement("fix-length-outbound-converter")
                .attribute("id", "req_converter")
                .attribute("codeGen", "true");

            XmlElement message = new XmlElement("message")
                .attribute("forType", "com.example.TestRequest");

            XmlElement field1 = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "accountNumber")
                .attribute("length", "16");

            XmlElement field2 = new XmlElement("field")
                .attribute("type", "CompositeField")
                .attribute("name", "address")
                .attribute("forType", "com.example.Address");

            XmlElement nestedField = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "street")
                .attribute("length", "50");

            field2.addChild(nestedField);
            message.addChild(field1);
            message.addChild(field2);
            converter.addChild(message);
            beans.addChild(converter);

            String result = formatter.format(beans);

            // Verify structure
            assertTrue(result.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
            assertTrue(result.contains("<beans:beans"));
            assertTrue(result.contains("<fix-length-outbound-converter"));
            assertTrue(result.contains("<message"));
            assertTrue(result.contains("name=\"accountNumber\""));
            assertTrue(result.contains("name=\"street\""));
            assertTrue(result.contains("</field2>") || result.contains("</field>"));
        }

        @Test
        @DisplayName("Should preserve attribute order in output")
        void shouldPreserveAttributeOrderInOutput() {
            XmlElement element = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "test")
                .attribute("length", "10");

            String result = formatter.format(element);

            // Attributes should appear in insertion order
            int typePos = result.indexOf("type=");
            int namePos = result.indexOf("name=");
            int lengthPos = result.indexOf("length=");

            assertTrue(typePos < namePos);
            assertTrue(namePos < lengthPos);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty string text content")
        void shouldHandleEmptyStringTextContent() {
            XmlElement element = new XmlElement("value")
                .text("");

            String result = formatter.format(element);

            // Empty text is still text content, not empty element
            assertTrue(result.contains("<value></value>"));
        }

        @Test
        @DisplayName("Should handle element with namespace prefix")
        void shouldHandleElementWithNamespacePrefix() {
            XmlElement element = new XmlElement("beans:beans")
                .attribute("xmlns:beans", "http://example.com");

            String result = formatter.format(element);

            assertTrue(result.contains("<beans:beans"));
            assertTrue(result.contains("xmlns:beans=\"http://example.com\""));
        }
    }
}
