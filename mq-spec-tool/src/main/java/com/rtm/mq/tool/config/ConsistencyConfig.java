package com.rtm.mq.tool.config;

import java.util.*;

/**
 * Configuration for cross-artifact consistency validation.
 *
 * <p>Controls how consistency checks are performed across XML, Java, and OpenAPI artifacts.</p>
 */
public class ConsistencyConfig {
    private boolean strictMode = false;
    private List<String> ignoreFields = new ArrayList<>();
    private Map<String, String> typeMappingRules = new HashMap<>();

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public List<String> getIgnoreFields() {
        return ignoreFields;
    }

    public void setIgnoreFields(List<String> ignoreFields) {
        this.ignoreFields = ignoreFields != null ? ignoreFields : new ArrayList<>();
    }

    public Map<String, String> getTypeMappingRules() {
        return typeMappingRules;
    }

    public void setTypeMappingRules(Map<String, String> typeMappingRules) {
        this.typeMappingRules = typeMappingRules != null ? typeMappingRules : new HashMap<>();
    }

    public void setDefaults() {
        if (ignoreFields == null) {
            ignoreFields = new ArrayList<>();
        }
        if (typeMappingRules == null) {
            typeMappingRules = new HashMap<>();
        }

        // Set default type mapping rules if empty
        if (typeMappingRules.isEmpty()) {
            setDefaultTypeMappings();
        }
    }

    /**
     * Sets default type mapping rules for common types.
     */
    private void setDefaultTypeMappings() {
        // XML types
        typeMappingRules.put("xs:string", "string");
        typeMappingRules.put("xs:int", "integer");
        typeMappingRules.put("xs:integer", "integer");
        typeMappingRules.put("xs:long", "long");
        typeMappingRules.put("xs:decimal", "decimal");
        typeMappingRules.put("xs:boolean", "boolean");
        typeMappingRules.put("xs:date", "date");
        typeMappingRules.put("xs:dateTime", "datetime");

        // Java types
        typeMappingRules.put("String", "string");
        typeMappingRules.put("Integer", "integer");
        typeMappingRules.put("int", "integer");
        typeMappingRules.put("Long", "long");
        typeMappingRules.put("long", "long");
        typeMappingRules.put("BigDecimal", "decimal");
        typeMappingRules.put("Boolean", "boolean");
        typeMappingRules.put("boolean", "boolean");
        typeMappingRules.put("LocalDate", "date");
        typeMappingRules.put("LocalDateTime", "datetime");

        // OpenAPI types
        typeMappingRules.put("string", "string");
        typeMappingRules.put("integer", "integer");
        typeMappingRules.put("number", "decimal");
        typeMappingRules.put("boolean", "boolean");
    }

    public void merge(ConsistencyConfig other) {
        if (other == null) {
            return;
        }
        this.strictMode = other.strictMode;
        if (other.ignoreFields != null && !other.ignoreFields.isEmpty()) {
            this.ignoreFields = new ArrayList<>(other.ignoreFields);
        }
        if (other.typeMappingRules != null && !other.typeMappingRules.isEmpty()) {
            this.typeMappingRules = new HashMap<>(other.typeMappingRules);
        }
    }
}
