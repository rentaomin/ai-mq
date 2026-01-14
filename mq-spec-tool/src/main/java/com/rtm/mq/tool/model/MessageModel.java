package com.rtm.mq.tool.model;

/**
 * Represents the complete parsed message model from an Excel specification.
 *
 * <p>A message model contains:</p>
 * <ul>
 *   <li>Request structure (outbound message)</li>
 *   <li>Response structure (inbound message)</li>
 *   <li>Shared header definitions</li>
 *   <li>Message metadata (API name, version, etc.)</li>
 * </ul>
 *
 * @see FieldNode
 * @see FieldGroup
 * @see Metadata
 */
public class MessageModel {

    /** Metadata about the parsed specification. */
    private Metadata metadata;

    /** Shared header field group. */
    private FieldGroup sharedHeader;

    /** Request message field group. */
    private FieldGroup request;

    /** Response message field group. */
    private FieldGroup response;

    /** Standalone MQ message field definitions (optional). */
    private MqMessageModel mqMessage;

    /**
     * Gets the metadata about the parsed specification.
     *
     * @return the metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata about the parsed specification.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the shared header field group.
     *
     * @return the shared header
     */
    public FieldGroup getSharedHeader() {
        return sharedHeader;
    }

    /**
     * Sets the shared header field group.
     *
     * @param sharedHeader the shared header to set
     */
    public void setSharedHeader(FieldGroup sharedHeader) {
        this.sharedHeader = sharedHeader;
    }

    /**
     * Gets the request message field group.
     *
     * @return the request field group
     */
    public FieldGroup getRequest() {
        return request;
    }

    /**
     * Sets the request message field group.
     *
     * @param request the request to set
     */
    public void setRequest(FieldGroup request) {
        this.request = request;
    }

    /**
     * Gets the response message field group.
     *
     * @return the response field group
     */
    public FieldGroup getResponse() {
        return response;
    }

    /**
     * Sets the response message field group.
     *
     * @param response the response to set
     */
    public void setResponse(FieldGroup response) {
        this.response = response;
    }

    /**
     * Gets the standalone MQ message model.
     *
     * @return the MQ message model, or null if not provided
     */
    public MqMessageModel getMqMessage() {
        return mqMessage;
    }

    /**
     * Sets the standalone MQ message model.
     *
     * @param mqMessage the MQ message model to set
     */
    public void setMqMessage(MqMessageModel mqMessage) {
        this.mqMessage = mqMessage;
    }
}
