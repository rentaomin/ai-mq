package com.rtm.mq.tool.validator;

import com.rtm.mq.tool.exception.ValidationException;
import com.rtm.mq.tool.generator.xml.XmlFieldType;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates XML bean definition files for structural correctness.
 *
 * <p>This validator performs local XML structural validation only. It does NOT:</p>
 * <ul>
 *   <li>Compare against JSON Tree (handled by T-304)</li>
 *   <li>Validate field order</li>
 *   <li>Validate cross-artifact consistency</li>
 *   <li>Validate runtime message content</li>
 * </ul>
 *
 * <p>Validation checks performed:</p>
 * <ol>
 *   <li>XML well-formed: file exists, non-empty, parsable</li>
 *   <li>Root-level contract: root element exists, required attributes present (messageType, version)</li>
 *   <li>Node-level structure: each field has name (non-empty, camelCase) and type (resolved)</li>
 *   <li>XML type mapping: declared XML type is resolvable</li>
 * </ol>
 *
 * @see Validator
 */
public class XmlBeanValidator implements Validator {

    /** Error code for file not found. */
    public static final String ERR_FILE_NOT_FOUND = "XML-001";

    /** Error code for empty file. */
    public static final String ERR_FILE_EMPTY = "XML-002";

    /** Error code for XML parse error. */
    public static final String ERR_PARSE_FAILED = "XML-003";

    /** Error code for missing root element. */
    public static final String ERR_ROOT_MISSING = "XML-004";

    /** Error code for missing required attribute. */
    public static final String ERR_ATTR_MISSING = "XML-005";

    /** Error code for missing field name. */
    public static final String ERR_NAME_MISSING = "XML-006";

    /** Error code for invalid camelCase name. */
    public static final String ERR_NAME_INVALID = "XML-007";

    /** Error code for missing field type. */
    public static final String ERR_TYPE_MISSING = "XML-008";

    /** Error code for unknown XML type. */
    public static final String ERR_TYPE_UNKNOWN = "XML-009";

    /** Required root attributes. */
    private static final String ATTR_MESSAGE_TYPE = "messageType";
    private static final String ATTR_VERSION = "version";

    /** Field element tag names. */
    private static final String TAG_DATA_FIELD = "DataField";
    private static final String TAG_COMPOSITE_FIELD = "CompositeField";
    private static final String TAG_REPEATING_FIELD = "RepeatingField";

    /** Field attribute names. */
    private static final String ATTR_NAME = "name";

    /** CamelCase pattern: starts with lowercase letter, followed by alphanumeric. */
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    /** Valid XML field types from XmlFieldType enum. */
    private static final Set<String> VALID_FIELD_TYPES = new HashSet<>();

    static {
        for (XmlFieldType type : XmlFieldType.values()) {
            VALID_FIELD_TYPES.add(type.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResult validate(Path targetPath) {
        ValidationResult result = new ValidationResult();

        // 1. File existence check
        if (!Files.exists(targetPath)) {
            result.addError(new ValidationError(
                ERR_FILE_NOT_FOUND,
                "File not found",
                "filePath=" + targetPath.toAbsolutePath()
            ));
            return result;
        }

        // 2. Empty file check
        try {
            if (Files.size(targetPath) == 0) {
                result.addError(new ValidationError(
                    ERR_FILE_EMPTY,
                    "File is empty",
                    "filePath=" + targetPath.toAbsolutePath()
                ));
                return result;
            }
        } catch (IOException e) {
            result.addError(new ValidationError(
                ERR_FILE_NOT_FOUND,
                "Cannot read file",
                "filePath=" + targetPath.toAbsolutePath() + ", error=" + e.getMessage()
            ));
            return result;
        }

        // 3. Parse XML
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(targetPath.toFile());
        } catch (ParserConfigurationException e) {
            throw new ValidationException("XML parser configuration error: " + e.getMessage());
        } catch (SAXException e) {
            result.addError(new ValidationError(
                ERR_PARSE_FAILED,
                "XML parse error",
                "filePath=" + targetPath.toAbsolutePath() + ", error=" + e.getMessage()
            ));
            return result;
        } catch (IOException e) {
            result.addError(new ValidationError(
                ERR_FILE_NOT_FOUND,
                "Cannot read file",
                "filePath=" + targetPath.toAbsolutePath() + ", error=" + e.getMessage()
            ));
            return result;
        }

        // 4. Root element validation
        Element root = document.getDocumentElement();
        if (root == null) {
            result.addError(new ValidationError(
                ERR_ROOT_MISSING,
                "Missing root element",
                "filePath=" + targetPath.toAbsolutePath()
            ));
            return result;
        }

        String filePath = targetPath.toAbsolutePath().toString();
        String rootTagName = root.getTagName();

        // 5. Required root attributes
        validateRequiredAttribute(root, ATTR_MESSAGE_TYPE, filePath, rootTagName, result);
        validateRequiredAttribute(root, ATTR_VERSION, filePath, rootTagName, result);

        // 6. Validate field nodes
        validateFieldNodes(root, filePath, "/" + rootTagName, result);

        return result;
    }

    /**
     * Validates that a required attribute exists and is non-empty.
     *
     * @param element the element to check
     * @param attrName the attribute name
     * @param filePath the file path for error reporting
     * @param elementName the element name for error reporting
     * @param result the validation result to add errors to
     */
    private void validateRequiredAttribute(Element element, String attrName,
            String filePath, String elementName, ValidationResult result) {
        String value = element.getAttribute(attrName);
        if (value == null || value.trim().isEmpty()) {
            result.addError(new ValidationError(
                ERR_ATTR_MISSING,
                "Missing required attribute: " + attrName,
                "filePath=" + filePath + ", element=" + elementName
            ));
        }
    }

    /**
     * Recursively validates field nodes in the XML document.
     *
     * @param parent the parent element
     * @param filePath the file path for error reporting
     * @param parentXpath the parent XPath for error reporting
     * @param result the validation result to add errors to
     */
    private void validateFieldNodes(Element parent, String filePath,
            String parentXpath, ValidationResult result) {
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) node;
            String tagName = element.getTagName();

            // Only validate field elements
            if (isFieldElement(tagName)) {
                String xpath = parentXpath + "/" + tagName;
                validateFieldElement(element, filePath, xpath, result);

                // Recurse into composite/repeating fields
                if (TAG_COMPOSITE_FIELD.equals(tagName) || TAG_REPEATING_FIELD.equals(tagName)) {
                    validateFieldNodes(element, filePath, xpath, result);
                }
            } else {
                // Recurse into other container elements (e.g., beans, bean)
                String xpath = parentXpath + "/" + tagName;
                validateFieldNodes(element, filePath, xpath, result);
            }
        }
    }

    /**
     * Checks if an element is a field element.
     *
     * @param tagName the tag name
     * @return true if this is a field element
     */
    private boolean isFieldElement(String tagName) {
        return TAG_DATA_FIELD.equals(tagName) ||
               TAG_COMPOSITE_FIELD.equals(tagName) ||
               TAG_REPEATING_FIELD.equals(tagName);
    }

    /**
     * Validates a single field element.
     *
     * @param element the field element
     * @param filePath the file path for error reporting
     * @param xpath the XPath for error reporting
     * @param result the validation result to add errors to
     */
    private void validateFieldElement(Element element, String filePath,
            String xpath, ValidationResult result) {
        String tagName = element.getTagName();

        // Validate type is resolvable
        if (!VALID_FIELD_TYPES.contains(tagName)) {
            result.addError(new ValidationError(
                ERR_TYPE_UNKNOWN,
                "Unknown XML field type: " + tagName,
                "filePath=" + filePath + ", xpath=" + xpath
            ));
        }

        // Validate name attribute for non-transitory fields
        // Note: transitory fields may not have name attribute
        String name = element.getAttribute(ATTR_NAME);
        String transitory = element.getAttribute("transitory");
        boolean isTransitory = "true".equalsIgnoreCase(transitory);

        // Only validate name for non-transitory fields
        if (!isTransitory) {
            if (name == null || name.trim().isEmpty()) {
                result.addError(new ValidationError(
                    ERR_NAME_MISSING,
                    "Missing name attribute",
                    "filePath=" + filePath + ", xpath=" + xpath
                ));
            } else if (!isValidCamelCase(name)) {
                result.addError(new ValidationError(
                    ERR_NAME_INVALID,
                    "Invalid camelCase name: " + name,
                    "filePath=" + filePath + ", xpath=" + xpath
                ));
            }
        }
    }

    /**
     * Checks if a name is valid camelCase.
     *
     * <p>A valid camelCase name:</p>
     * <ul>
     *   <li>Starts with a lowercase letter</li>
     *   <li>Contains only alphanumeric characters</li>
     * </ul>
     *
     * @param name the name to check
     * @return true if valid camelCase
     */
    private boolean isValidCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return CAMEL_CASE_PATTERN.matcher(name).matches();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "xml";
    }
}
