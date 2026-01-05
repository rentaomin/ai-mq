package com.rtm.mq.tool.config;

/**
 * Configuration for OpenAPI YAML generation.
 */
public class OpenApiConfig {
    private String version = "3.0.3";
    private boolean splitSchemas = true;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isSplitSchemas() {
        return splitSchemas;
    }

    public void setSplitSchemas(boolean splitSchemas) {
        this.splitSchemas = splitSchemas;
    }

    public void setDefaults() {
        if (version == null || version.isEmpty()) {
            version = "3.0.3";
        }
        // splitSchemas defaults to true
    }

    public void merge(OpenApiConfig other) {
        if (other == null) {
            return;
        }
        if (other.version != null && !other.version.isEmpty()) {
            this.version = other.version;
        }
        // For boolean, take explicit setting from file
        this.splitSchemas = other.splitSchemas;
    }
}
