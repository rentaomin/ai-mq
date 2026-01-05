package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;

import java.util.List;
import java.util.Map;

/**
 * XML Template Engine for generating Spring XML bean definitions.
 *
 * <p>This engine renders FieldGroup structures into XML bean format for
 * Spring-based message converters. It supports both outbound (request) and
 * inbound (response) message generation.</p>
 *
 * <p>Generated XML structure:</p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <beans:beans xmlns:xsi="..." xmlns="..." xmlns:beans="...">
 *   <fix-length-outbound-converter id="..." codeGen="true">
 *     <message forType="...">
 *       <field type="DataField" name="..." length="..." />
 *       <field type="CompositeField" name="..." forType="...">
 *         ...nested fields...
 *       </field>
 *     </message>
 *   </fix-length-outbound-converter>
 * </beans:beans>
 * }</pre>
 *
 * @see XmlElement
 * @see XmlFormatter
 * @see XmlTypeMapper
 */
public class XmlTemplateEngine {

    private final Config config;
    private final XmlTypeMapper typeMapper;
    private final XmlFormatter formatter;

    /**
     * Constructs an XmlTemplateEngine with the given configuration.
     *
     * @param config the configuration containing XML generation settings
     */
    public XmlTemplateEngine(Config config) {
        this.config = config;
        this.typeMapper = new XmlTypeMapper(config);
        this.formatter = new XmlFormatter();
    }

    /**
     * Generates an Outbound XML document (for request messages).
     *
     * @param request the request field group from the intermediate JSON tree
     * @param operationId the operation identifier (used for message type naming)
     * @return the formatted XML string
     */
    public String generateOutbound(FieldGroup request, String operationId) {
        XmlElement root = createRootElement("fix-length-outbound-converter", "req_converter");
        XmlElement message = createMessageElement(operationId + "Request");

        // Add fields preserving order (AC3)
        addFields(message, request.getFields());

        root.addChild(message);

        return formatter.format(wrapWithBeans(root, config.getXml().getNamespace().getOutbound()));
    }

    /**
     * Generates an Inbound XML document (for response messages).
     *
     * @param response the response field group from the intermediate JSON tree
     * @param operationId the operation identifier (used for message type naming)
     * @return the formatted XML string
     */
    public String generateInbound(FieldGroup response, String operationId) {
        XmlElement root = createRootElement("fix-length-inbound-converter", "resp_converter");
        XmlElement message = createMessageElement(operationId + "Response");

        // Add fields preserving order (AC3)
        addFields(message, response.getFields());

        root.addChild(message);

        return formatter.format(wrapWithBeans(root, config.getXml().getNamespace().getInbound()));
    }

    /**
     * Creates the root converter element.
     *
     * @param tagName the converter tag name (fix-length-outbound-converter or fix-length-inbound-converter)
     * @param id the converter bean id
     * @return the root converter element
     */
    private XmlElement createRootElement(String tagName, String id) {
        return new XmlElement(tagName)
            .attribute("id", id)
            .attribute("codeGen", "true");
    }

    /**
     * Creates the message element with forType attribute.
     *
     * @param className the message class name
     * @return the message element
     */
    private XmlElement createMessageElement(String className) {
        String forType = buildForType(className);
        return new XmlElement("message")
            .attribute("forType", forType);
    }

    /**
     * Recursively adds fields to a parent element.
     *
     * <p>Field order is preserved as specified in the intermediate JSON tree (AC3).
     * Nested structures (objects and arrays) are handled recursively (AC4).</p>
     *
     * @param parent the parent element to add fields to
     * @param fields the list of field nodes to add
     */
    private void addFields(XmlElement parent, List<FieldNode> fields) {
        for (FieldNode field : fields) {
            XmlElement fieldElement = createFieldElement(field);
            parent.addChild(fieldElement);

            // Recursively add child fields for objects and arrays (AC4)
            if (!field.getChildren().isEmpty() &&
                (field.isObject() || field.isArray())) {
                addFields(fieldElement, field.getChildren());
            }
        }
    }

    /**
     * Creates a field element from a FieldNode.
     *
     * <p>Uses XmlTypeMapper to determine the appropriate XML field type
     * and attributes based on the field characteristics.</p>
     *
     * @param node the field node from the intermediate JSON tree
     * @return the field XML element
     */
    private XmlElement createFieldElement(FieldNode node) {
        XmlFieldAttributes attrs = typeMapper.map(node);

        XmlElement element = new XmlElement("field");
        for (Map.Entry<String, String> entry : attrs.getAttributes().entrySet()) {
            element.attribute(entry.getKey(), entry.getValue());
        }

        return element;
    }

    /**
     * Wraps the converter element with the beans root element.
     *
     * <p>Adds required namespace declarations for Spring XML configuration (AC2).</p>
     *
     * @param content the converter element to wrap
     * @param namespace the primary namespace URI
     * @return the beans root element
     */
    private XmlElement wrapWithBeans(XmlElement content, String namespace) {
        XmlElement beans = new XmlElement("beans:beans")
            .attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            .attribute("xmlns", namespace)
            .attribute("xmlns:beans", "http://www.springframework.org/schema/beans");

        beans.addChild(content);
        return beans;
    }

    /**
     * Builds the fully qualified forType attribute value.
     *
     * <p>Format: {groupId}.{artifactId}.{className}</p>
     *
     * @param className the class name
     * @return the fully qualified class name
     */
    private String buildForType(String className) {
        String groupId = config.getXml().getProject().getGroupId();
        String artifactId = config.getXml().getProject().getArtifactId();
        return groupId + "." + artifactId + "." + className;
    }

    /**
     * Gets the XmlFormatter used by this engine.
     *
     * <p>Exposed for testing purposes.</p>
     *
     * @return the XML formatter
     */
    public XmlFormatter getFormatter() {
        return formatter;
    }

    /**
     * Gets the XmlTypeMapper used by this engine.
     *
     * <p>Exposed for testing purposes.</p>
     *
     * @return the type mapper
     */
    public XmlTypeMapper getTypeMapper() {
        return typeMapper;
    }
}
