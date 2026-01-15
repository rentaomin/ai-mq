package com.rtm.mq.tool.generator.xml;

import java.util.Map;

/**
 * Formats XmlElement trees into well-formed XML strings.
 *
 * <p>This formatter produces XML output with the following characteristics:</p>
 * <ul>
 *   <li>XML 1.0 declaration with UTF-8 encoding</li>
 *   <li>2-space indentation for nested elements</li>
 *   <li>Self-closing tags for empty elements</li>
 *   <li>Proper XML escaping for attribute values and text content</li>
 *   <li>Deterministic output - same input always produces same output</li>
 * </ul>
 *
 * <p>XML escaping rules applied:</p>
 * <ul>
 *   <li>&amp; becomes &amp;amp;</li>
 *   <li>&lt; becomes &amp;lt;</li>
 *   <li>&gt; becomes &amp;gt;</li>
 *   <li>&quot; becomes &amp;quot;</li>
 *   <li>&apos; becomes &amp;apos;</li>
 * </ul>
 *
 * @see XmlElement
 * @see XmlTemplateEngine
 */
public class XmlFormatter {

    /** Standard 2-space indentation per level. */
    private static final String INDENT = "  ";

    /** XML 1.0 declaration with UTF-8 encoding. */
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Formats an XML element tree into a complete XML document string.
     *
     * <p>The output includes the XML declaration followed by the formatted
     * element tree with proper indentation.</p>
     *
     * @param root the root element of the XML tree
     * @return the formatted XML string
     */
    public String format(XmlElement root) {
        if (root == null) {
            return XML_DECLARATION + "\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(XML_DECLARATION).append("\n");
        formatElement(sb, root, 0);
        return sb.toString();
    }

    /**
     * Formats an XML element tree without the XML declaration.
     *
     * <p>This method is useful for generating XML fragments or when
     * the declaration is handled separately.</p>
     *
     * @param root the root element of the XML tree
     * @return the formatted XML string without declaration
     */
    public String formatWithoutDeclaration(XmlElement root) {
        if (root == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        formatElement(sb, root, 0);
        return sb.toString();
    }

    /**
     * Recursively formats an element and its children.
     *
     * @param sb the StringBuilder to append to
     * @param element the element to format
     * @param level the current indentation level
     */
    private void formatElement(StringBuilder sb, XmlElement element, int level) {
        String indent = getIndent(level);

        // Start tag
        sb.append(indent).append("<").append(element.getTagName());

        // Attributes (order preserved by LinkedHashMap)
        for (Map.Entry<String, String> attr : element.getAttributes().entrySet()) {
            sb.append(" ").append(attr.getKey()).append("=\"")
              .append(escapeXml(attr.getValue())).append("\"");
        }

        // Handle empty, text-only, or children cases
        if (element.isEmpty()) {
            // Self-closing tag for empty elements (AC7)
            sb.append(" />\n");
        } else if (element.hasTextContent() && !element.hasChildren()) {
            // Text content on same line
            sb.append(">").append(escapeXml(element.getTextContent()));
            sb.append("</").append(element.getTagName()).append(">\n");
        } else {
            // Has children (and possibly text)
            sb.append(">\n");

            // Text content before children if both exist
            if (element.hasTextContent()) {
                sb.append(getIndent(level + 1))
                  .append(escapeXml(element.getTextContent())).append("\n");
            }

            for (XmlElement child : element.getChildren()) {
                formatElement(sb, child, level + 1);
                if (child.isBlankLineAfter()) {
                    sb.append("\n");
                }
            }

            sb.append(indent).append("</").append(element.getTagName()).append(">\n");
        }
    }

    /**
     * Gets the indentation string for the specified level.
     *
     * @param level the indentation level (0 = no indent)
     * @return the indentation string (2 spaces per level)
     */
    private String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    /**
     * Escapes special XML characters in a string.
     *
     * <p>The following characters are escaped:</p>
     * <ul>
     *   <li>&amp; to &amp;amp; (must be first)</li>
     *   <li>&lt; to &amp;lt;</li>
     *   <li>&gt; to &amp;gt;</li>
     *   <li>&quot; to &amp;quot;</li>
     *   <li>&apos; to &amp;apos;</li>
     * </ul>
     *
     * @param value the string to escape
     * @return the escaped string, or empty string if null
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        // Order matters: & must be replaced first
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
