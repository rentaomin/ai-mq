package com.rtm.mq.tool.config;

/**
 * Configuration for validation settings.
 */
public class ValidationConfig {
    private boolean redactPayload = true;
    private ConsistencyConfig consistency;

    public boolean isRedactPayload() {
        return redactPayload;
    }

    public void setRedactPayload(boolean redactPayload) {
        this.redactPayload = redactPayload;
    }

    public ConsistencyConfig getConsistency() {
        return consistency;
    }

    public void setConsistency(ConsistencyConfig consistency) {
        this.consistency = consistency;
    }

    public void setDefaults() {
        // redactPayload defaults to true
        if (consistency == null) {
            consistency = new ConsistencyConfig();
        }
        consistency.setDefaults();
    }

    public void merge(ValidationConfig other) {
        if (other == null) {
            return;
        }
        // Take the value from other, as it's explicitly set in config file
        this.redactPayload = other.redactPayload;
        if (other.consistency != null) {
            if (this.consistency == null) {
                this.consistency = new ConsistencyConfig();
            }
            this.consistency.merge(other.consistency);
        }
    }
}
