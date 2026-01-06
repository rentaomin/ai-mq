package com.rtm.mq.tool.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single cross-artifact consistency issue.
 *
 * <p>Each issue contains:</p>
 * <ul>
 *   <li>Category: MISSING_FIELD, TYPE_MISMATCH, STRUCTURE_MISMATCH, etc.</li>
 *   <li>Severity: ERROR or WARNING</li>
 *   <li>Field path identifying the problematic field</li>
 *   <li>Artifact-specific details (type, shape, required flag per artifact)</li>
 * </ul>
 */
public class ConsistencyIssue {
    private String category;
    private String severity;
    private String fieldPath;
    private Map<String, ArtifactFieldInfo> artifacts = new HashMap<>();
    private String message;

    public ConsistencyIssue() {
    }

    public ConsistencyIssue(String category, String severity, String fieldPath, String message) {
        this.category = category;
        this.severity = severity;
        this.fieldPath = fieldPath;
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public Map<String, ArtifactFieldInfo> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Map<String, ArtifactFieldInfo> artifacts) {
        this.artifacts = artifacts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Adds artifact field information to this issue.
     *
     * @param artifactName the artifact name (xml, java, openapi)
     * @param info the field information for this artifact
     */
    public void addArtifactInfo(String artifactName, ArtifactFieldInfo info) {
        this.artifacts.put(artifactName, info);
    }

    /**
     * Information about a field in a specific artifact.
     */
    public static class ArtifactFieldInfo {
        private Boolean present;
        private String type;
        private String canonicalType;
        private String shape;
        private Boolean required;

        public ArtifactFieldInfo() {
        }

        public ArtifactFieldInfo(boolean present) {
            this.present = present;
        }

        public Boolean getPresent() {
            return present;
        }

        public void setPresent(Boolean present) {
            this.present = present;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCanonicalType() {
            return canonicalType;
        }

        public void setCanonicalType(String canonicalType) {
            this.canonicalType = canonicalType;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }
    }
}
