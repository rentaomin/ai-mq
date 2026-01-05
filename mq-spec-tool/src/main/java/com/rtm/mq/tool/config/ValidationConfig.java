package com.rtm.mq.tool.config;

/**
 * Configuration for validation settings.
 */
public class ValidationConfig {
    private boolean redactPayload = true;

    public boolean isRedactPayload() {
        return redactPayload;
    }

    public void setRedactPayload(boolean redactPayload) {
        this.redactPayload = redactPayload;
    }

    public void setDefaults() {
        // redactPayload defaults to true
    }

    public void merge(ValidationConfig other) {
        if (other == null) {
            return;
        }
        // Take the value from other, as it's explicitly set in config file
        this.redactPayload = other.redactPayload;
    }
}
