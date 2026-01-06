package com.rtm.mq.tool.config;

/**
 * Configuration for OpenAPI YAML generation.
 */
public class OpenApiConfig {
    private String version = "3.0.3";
    private boolean splitSchemas = true;
    private SplitStrategy splitStrategy = SplitStrategy.BY_OBJECT;
    private String title;
    private String description;
    private String serverUrl;
    private String apiVersion = "1.0.0";

    /**
     * Schema split strategy
     */
    public enum SplitStrategy {
        /** All schemas remain in the main file */
        NONE,
        /** Split by message (Request/Response separate files) */
        BY_MESSAGE,
        /** Split by object (each schema in separate file) */
        BY_OBJECT
    }

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

    public SplitStrategy getSplitStrategy() {
        return splitStrategy;
    }

    public void setSplitStrategy(SplitStrategy splitStrategy) {
        this.splitStrategy = splitStrategy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setDefaults() {
        if (version == null || version.isEmpty()) {
            version = "3.0.3";
        }
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = "1.0.0";
        }
        if (splitStrategy == null) {
            splitStrategy = SplitStrategy.BY_OBJECT;
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
        if (other.title != null && !other.title.isEmpty()) {
            this.title = other.title;
        }
        if (other.description != null && !other.description.isEmpty()) {
            this.description = other.description;
        }
        if (other.serverUrl != null && !other.serverUrl.isEmpty()) {
            this.serverUrl = other.serverUrl;
        }
        if (other.apiVersion != null && !other.apiVersion.isEmpty()) {
            this.apiVersion = other.apiVersion;
        }
        if (other.splitStrategy != null) {
            this.splitStrategy = other.splitStrategy;
        }
        // For boolean, take explicit setting from file
        this.splitSchemas = other.splitSchemas;
    }
}
