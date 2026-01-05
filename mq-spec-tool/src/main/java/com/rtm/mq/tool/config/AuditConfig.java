package com.rtm.mq.tool.config;

/**
 * Configuration for audit logging settings.
 */
public class AuditConfig {
    private boolean hashOutputs = false;
    private boolean redactFilePaths = false;

    public boolean isHashOutputs() {
        return hashOutputs;
    }

    public void setHashOutputs(boolean hashOutputs) {
        this.hashOutputs = hashOutputs;
    }

    public boolean isRedactFilePaths() {
        return redactFilePaths;
    }

    public void setRedactFilePaths(boolean redactFilePaths) {
        this.redactFilePaths = redactFilePaths;
    }

    public void setDefaults() {
        // Both default to false, which is already set
    }

    public void merge(AuditConfig other) {
        if (other == null) {
            return;
        }
        if (other.hashOutputs) {
            this.hashOutputs = true;
        }
        if (other.redactFilePaths) {
            this.redactFilePaths = true;
        }
    }
}
