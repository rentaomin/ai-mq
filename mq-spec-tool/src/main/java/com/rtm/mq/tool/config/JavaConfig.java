package com.rtm.mq.tool.config;

/**
 * Configuration for Java bean generation.
 */
public class JavaConfig {
    private boolean useLombok = false;
    private String packageName;

    public boolean isUseLombok() {
        return useLombok;
    }

    public void setUseLombok(boolean useLombok) {
        this.useLombok = useLombok;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setDefaults() {
        // useLombok defaults to false; packageName can be derived from groupId + artifactId
    }

    public void merge(JavaConfig other) {
        if (other == null) {
            return;
        }
        // Boolean field: always take from other if it's true
        if (other.useLombok) {
            this.useLombok = true;
        }
        if (other.packageName != null && !other.packageName.isEmpty()) {
            this.packageName = other.packageName;
        }
    }
}
