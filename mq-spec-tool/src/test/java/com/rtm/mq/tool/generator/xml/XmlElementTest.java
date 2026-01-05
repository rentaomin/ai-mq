package com.rtm.mq.tool.generator.xml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XmlElement.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Element construction and tag name validation</li>
 *   <li>Attribute handling with order preservation</li>
 *   <li>Child element management</li>
 *   <li>Text content handling</li>
 *   <li>Empty element detection</li>
 * </ul>
 */
@DisplayName("XmlElement Tests")
class XmlElementTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create element with valid tag name")
        void shouldCreateElementWithValidTagName() {
            XmlElement element = new XmlElement("field");
            assertEquals("field", element.getTagName());
        }

        @Test
        @DisplayName("Should throw exception for null tag name")
        void shouldThrowExceptionForNullTagName() {
            assertThrows(IllegalArgumentException.class, () -> new XmlElement(null));
        }

        @Test
        @DisplayName("Should throw exception for empty tag name")
        void shouldThrowExceptionForEmptyTagName() {
            assertThrows(IllegalArgumentException.class, () -> new XmlElement(""));
        }
    }

    @Nested
    @DisplayName("Attribute Tests")
    class AttributeTests {

        @Test
        @DisplayName("Should add attribute with non-null value")
        void shouldAddAttributeWithNonNullValue() {
            XmlElement element = new XmlElement("field")
                .attribute("name", "testField");

            assertEquals("testField", element.getAttributes().get("name"));
        }

        @Test
        @DisplayName("Should ignore null attribute value")
        void shouldIgnoreNullAttributeValue() {
            XmlElement element = new XmlElement("field")
                .attribute("name", null);

            assertFalse(element.getAttributes().containsKey("name"));
        }

        @Test
        @DisplayName("Should preserve attribute insertion order")
        void shouldPreserveAttributeInsertionOrder() {
            XmlElement element = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "testField")
                .attribute("length", "10");

            Map<String, String> attrs = element.getAttributes();
            String[] keys = attrs.keySet().toArray(new String[0]);

            assertEquals("type", keys[0]);
            assertEquals("name", keys[1]);
            assertEquals("length", keys[2]);
        }

        @Test
        @DisplayName("Should support fluent API for multiple attributes")
        void shouldSupportFluentApiForMultipleAttributes() {
            XmlElement element = new XmlElement("field")
                .attribute("type", "DataField")
                .attribute("name", "accountNumber")
                .attribute("length", "16");

            assertEquals(3, element.getAttributes().size());
            assertEquals("DataField", element.getAttributes().get("type"));
            assertEquals("accountNumber", element.getAttributes().get("name"));
            assertEquals("16", element.getAttributes().get("length"));
        }
    }

    @Nested
    @DisplayName("Child Element Tests")
    class ChildElementTests {

        @Test
        @DisplayName("Should add child element")
        void shouldAddChildElement() {
            XmlElement parent = new XmlElement("message");
            XmlElement child = new XmlElement("field");

            parent.addChild(child);

            assertEquals(1, parent.getChildren().size());
            assertSame(child, parent.getChildren().get(0));
        }

        @Test
        @DisplayName("Should ignore null child element")
        void shouldIgnoreNullChildElement() {
            XmlElement parent = new XmlElement("message");
            parent.addChild(null);

            assertTrue(parent.getChildren().isEmpty());
        }

        @Test
        @DisplayName("Should preserve child element order")
        void shouldPreserveChildElementOrder() {
            XmlElement parent = new XmlElement("message");
            XmlElement child1 = new XmlElement("field").attribute("name", "first");
            XmlElement child2 = new XmlElement("field").attribute("name", "second");
            XmlElement child3 = new XmlElement("field").attribute("name", "third");

            parent.addChild(child1).addChild(child2).addChild(child3);

            assertEquals(3, parent.getChildren().size());
            assertEquals("first", parent.getChildren().get(0).getAttributes().get("name"));
            assertEquals("second", parent.getChildren().get(1).getAttributes().get("name"));
            assertEquals("third", parent.getChildren().get(2).getAttributes().get("name"));
        }

        @Test
        @DisplayName("Should support nested structures")
        void shouldSupportNestedStructures() {
            XmlElement root = new XmlElement("beans:beans");
            XmlElement converter = new XmlElement("fix-length-outbound-converter");
            XmlElement message = new XmlElement("message");
            XmlElement field = new XmlElement("field");

            message.addChild(field);
            converter.addChild(message);
            root.addChild(converter);

            assertEquals(1, root.getChildren().size());
            assertEquals(1, root.getChildren().get(0).getChildren().size());
            assertEquals(1, root.getChildren().get(0).getChildren().get(0).getChildren().size());
        }
    }

    @Nested
    @DisplayName("Text Content Tests")
    class TextContentTests {

        @Test
        @DisplayName("Should set text content")
        void shouldSetTextContent() {
            XmlElement element = new XmlElement("value")
                .text("Hello World");

            assertEquals("Hello World", element.getTextContent());
        }

        @Test
        @DisplayName("Should allow null text content")
        void shouldAllowNullTextContent() {
            XmlElement element = new XmlElement("value")
                .text(null);

            assertNull(element.getTextContent());
        }
    }

    @Nested
    @DisplayName("State Check Tests")
    class StateCheckTests {

        @Test
        @DisplayName("Should detect element with children")
        void shouldDetectElementWithChildren() {
            XmlElement element = new XmlElement("parent")
                .addChild(new XmlElement("child"));

            assertTrue(element.hasChildren());
        }

        @Test
        @DisplayName("Should detect element without children")
        void shouldDetectElementWithoutChildren() {
            XmlElement element = new XmlElement("leaf");

            assertFalse(element.hasChildren());
        }

        @Test
        @DisplayName("Should detect element with text content")
        void shouldDetectElementWithTextContent() {
            XmlElement element = new XmlElement("value")
                .text("content");

            assertTrue(element.hasTextContent());
        }

        @Test
        @DisplayName("Should detect element without text content")
        void shouldDetectElementWithoutTextContent() {
            XmlElement element = new XmlElement("value");

            assertFalse(element.hasTextContent());
        }

        @Test
        @DisplayName("Should detect empty element")
        void shouldDetectEmptyElement() {
            XmlElement element = new XmlElement("empty");

            assertTrue(element.isEmpty());
        }

        @Test
        @DisplayName("Should detect non-empty element with children")
        void shouldDetectNonEmptyElementWithChildren() {
            XmlElement element = new XmlElement("parent")
                .addChild(new XmlElement("child"));

            assertFalse(element.isEmpty());
        }

        @Test
        @DisplayName("Should detect non-empty element with text")
        void shouldDetectNonEmptyElementWithText() {
            XmlElement element = new XmlElement("value")
                .text("content");

            assertFalse(element.isEmpty());
        }
    }
}
