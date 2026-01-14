package com.rtm.mq.tool.model;

/**
 * Represents metadata about a parsed message specification.
 *
 * <p>Metadata captures information about the parsing process and
 * the source specification for audit and traceability purposes.</p>
 *
 * @see MessageModel
 */
public class Metadata {

    /** The path to the source Excel specification file. */
    private String sourceFile;

    /** The ISO 8601 timestamp when parsing occurred. */
    private String parseTimestamp;

    /** The version of the parser that processed this specification. */
    private String parserVersion;

    /** The operation name extracted from the specification. */
    private String operationName;

    /** The operation ID (e.g., API endpoint identifier). */
    private String operationId;

    /** The version of the message specification. */
    private String version;

    private String serviceCategory;
    
    private String serviceInterface;
    
    private String serviceComponent;
    
    private String serviceID;
    
    private String description;
    
    
    /**
     * Gets the path to the source Excel specification file.
     *
     * @return the source file path
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the path to the source Excel specification file.
     *
     * @param sourceFile the source file path to set
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * Gets the ISO 8601 timestamp when parsing occurred.
     *
     * @return the parse timestamp
     */
    public String getParseTimestamp() {
        return parseTimestamp;
    }

    /**
     * Sets the ISO 8601 timestamp when parsing occurred.
     *
     * @param parseTimestamp the parse timestamp to set
     */
    public void setParseTimestamp(String parseTimestamp) {
        this.parseTimestamp = parseTimestamp;
    }

    /**
     * Gets the version of the parser that processed this specification.
     *
     * @return the parser version
     */
    public String getParserVersion() {
        return parserVersion;
    }

    /**
     * Sets the version of the parser that processed this specification.
     *
     * @param parserVersion the parser version to set
     */
    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    /**
     * Gets the operation name extracted from the specification.
     *
     * @return the operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the operation name extracted from the specification.
     *
     * @param operationName the operation name to set
     */
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    /**
     * Gets the operation ID (e.g., API endpoint identifier).
     *
     * @return the operation ID
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Sets the operation ID (e.g., API endpoint identifier).
     *
     * @param operationId the operation ID to set
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * Gets the version of the message specification.
     *
     * @return the specification version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the message specification.
     *
     * @param version the specification version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getServiceComponent() {
        return serviceComponent;
    }

    public void setServiceComponent(String serviceComponent) {
        this.serviceComponent = serviceComponent;
    }

    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
