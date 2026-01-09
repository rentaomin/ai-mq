package com.rtm.mq.tool.api.dto;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for spec generation API.
 *
 * <p>This class encapsulates generation configuration passed via multipart form data.</p>
 */
public class GenerationRequest {

    /**
     * XML namespace for inbound messages.
     */
    private String xmlNamespaceInbound;

    /**
     * XML namespace for outbound messages.
     */
    private String xmlNamespaceOutbound;

    /**
     * XML project group ID (Maven coordinates).
     */
    private String xmlProjectGroupId;

    /**
     * XML project artifact ID (Maven coordinates).
     */
    private String xmlProjectArtifactId;

    /**
     * Java package name for generated beans.
     */
    private String javaPackageName;

    /**
     * Whether to use Lombok annotations in Java beans.
     */
    private Boolean useLombok = false;

    /**
     * OpenAPI specification version (default: 3.0.3).
     */
    private String openApiVersion = "3.0.3";

    /**
     * Whether to split OpenAPI schemas into separate files.
     */
    private Boolean splitSchemas = true;

    /**
     * Additional custom configuration overrides.
     */
    private Map<String, Object> overrides = new HashMap<>();

    // Getters and setters

    public String getXmlNamespaceInbound() {
        return xmlNamespaceInbound;
    }

    public void setXmlNamespaceInbound(String xmlNamespaceInbound) {
        this.xmlNamespaceInbound = xmlNamespaceInbound;
    }

    public String getXmlNamespaceOutbound() {
        return xmlNamespaceOutbound;
    }

    public void setXmlNamespaceOutbound(String xmlNamespaceOutbound) {
        this.xmlNamespaceOutbound = xmlNamespaceOutbound;
    }

    public String getXmlProjectGroupId() {
        return xmlProjectGroupId;
    }

    public void setXmlProjectGroupId(String xmlProjectGroupId) {
        this.xmlProjectGroupId = xmlProjectGroupId;
    }

    public String getXmlProjectArtifactId() {
        return xmlProjectArtifactId;
    }

    public void setXmlProjectArtifactId(String xmlProjectArtifactId) {
        this.xmlProjectArtifactId = xmlProjectArtifactId;
    }

    public String getJavaPackageName() {
        return javaPackageName;
    }

    public void setJavaPackageName(String javaPackageName) {
        this.javaPackageName = javaPackageName;
    }

    public Boolean getUseLombok() {
        return useLombok;
    }

    public void setUseLombok(Boolean useLombok) {
        this.useLombok = useLombok;
    }

    public String getOpenApiVersion() {
        return openApiVersion;
    }

    public void setOpenApiVersion(String openApiVersion) {
        this.openApiVersion = openApiVersion;
    }

    public Boolean getSplitSchemas() {
        return splitSchemas;
    }

    public void setSplitSchemas(Boolean splitSchemas) {
        this.splitSchemas = splitSchemas;
    }

    public Map<String, Object> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, Object> overrides) {
        this.overrides = overrides;
    }
}
