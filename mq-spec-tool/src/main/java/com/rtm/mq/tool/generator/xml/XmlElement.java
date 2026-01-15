package com.rtm.mq.tool.generator.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an XML element for template-based XML generation.
 *
 * <p>This class provides a fluent builder-style API for constructing XML elements
 * with attributes and child elements. It uses LinkedHashMap for attributes to
 * preserve insertion order, ensuring deterministic XML output.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Preserves attribute order using LinkedHashMap</li>
 *   <li>Preserves child element order using ArrayList</li>
 *   <li>Supports text content for leaf elements</li>
 *   <li>Fluent API for easy element construction</li>
 * </ul>
 *
 * @see XmlFormatter
 * @see XmlTemplateEngine
 */
public class XmlElement {

    /** The XML tag name. */
    private final String tagName;

    /** The attributes map, ordered by insertion. */
    private final Map<String, String> attributes = new LinkedHashMap<>();

    /** The child elements list, preserving order. */
    private final List<XmlElement> children = new ArrayList<>();

    /** The text content for leaf elements. */
    private String textContent;

    /** Flag to add blank line after this element. */
    private boolean blankLineAfter = false;

    /**
     * Constructs an XML element with the specified tag name.
     *
     * @param tagName the XML tag name (must not be null or empty)
     * @throws IllegalArgumentException if tagName is null or empty
     */
    public XmlElement(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("Tag name must not be null or empty");
        }
        this.tagName = tagName;
    }

    /**
     * Adds an attribute to this element.
     *
     * <p>Null values are ignored to allow conditional attribute addition.</p>
     *
     * @param name the attribute name
     * @param value the attribute value, or null to skip
     * @return this element for method chaining
     */
    public XmlElement attribute(String name, String value) {
        if (value != null) {
            attributes.put(name, value);
        }
        return this;
    }

    /**
     * Adds a child element to this element.
     *
     * @param child the child element to add
     * @return this element for method chaining
     */
    public XmlElement addChild(XmlElement child) {
        if (child != null) {
            children.add(child);
        }
        return this;
    }

    /**
     * Sets the text content for this element.
     *
     * <p>Text content is mutually exclusive with child elements in standard
     * XML usage. If both are set, the formatter will handle appropriately.</p>
     *
     * @param content the text content
     * @return this element for method chaining
     */
    public XmlElement text(String content) {
        this.textContent = content;
        return this;
    }

    /**
     * Gets the XML tag name.
     *
     * @return the tag name
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Gets the attributes map.
     *
     * <p>The returned map preserves insertion order.</p>
     *
     * @return the attributes map (never null)
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Gets the list of child elements.
     *
     * <p>The returned list preserves insertion order.</p>
     *
     * @return the children list (never null, may be empty)
     */
    public List<XmlElement> getChildren() {
        return children;
    }

    /**
     * Gets the text content.
     *
     * @return the text content, or null if not set
     */
    public String getTextContent() {
        return textContent;
    }

    /**
     * Checks if this element has any children.
     *
     * @return true if there are child elements
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Checks if this element has text content.
     *
     * @return true if text content is set
     */
    public boolean hasTextContent() {
        return textContent != null;
    }

    /**
     * Checks if this element is empty (no children and no text content).
     *
     * @return true if the element is empty
     */
    public boolean isEmpty() {
        return children.isEmpty() && textContent == null;
    }

    /**
     * Marks that a blank line should be added after this element.
     *
     * @param mark true to add blank line after
     * @return this element for method chaining
     */
    public XmlElement markForBlankLine(boolean mark) {
        this.blankLineAfter = mark;
        return this;
    }

    /**
     * Checks if a blank line should be added after this element.
     *
     * @return true if blank line should follow
     */
    public boolean isBlankLineAfter() {
        return blankLineAfter;
    }
}
