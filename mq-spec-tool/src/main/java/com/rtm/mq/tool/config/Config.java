package com.rtm.mq.tool.config;

/**
 * Root configuration class containing all tool settings.
 *
 * <p>Configuration is loaded following the priority order:
 * CLI arguments > Config file > Default values</p>
 */
public class Config {
    private OutputConfig output;
    private XmlConfig xml;
    private JavaConfig java;
    private OpenApiConfig openapi;
    private ParserConfig parser;
    private AuditConfig audit;
    private ValidationConfig validation;
    private String loggingLevel = "INFO";

    public OutputConfig getOutput() {
        return output;
    }

    public void setOutput(OutputConfig output) {
        this.output = output;
    }

    public XmlConfig getXml() {
        return xml;
    }

    public void setXml(XmlConfig xml) {
        this.xml = xml;
    }

    public JavaConfig getJava() {
        return java;
    }

    public void setJava(JavaConfig java) {
        this.java = java;
    }

    public OpenApiConfig getOpenapi() {
        return openapi;
    }

    public void setOpenapi(OpenApiConfig openapi) {
        this.openapi = openapi;
    }

    public ParserConfig getParser() {
        return parser;
    }

    public void setParser(ParserConfig parser) {
        this.parser = parser;
    }

    public AuditConfig getAudit() {
        return audit;
    }

    public void setAudit(AuditConfig audit) {
        this.audit = audit;
    }

    public ValidationConfig getValidation() {
        return validation;
    }

    public void setValidation(ValidationConfig validation) {
        this.validation = validation;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    /**
     * Initializes all configuration sections with default values.
     * Called before loading config file and applying CLI overrides.
     */
    public void setDefaults() {
        if (output == null) {
            output = new OutputConfig();
        }
        if (xml == null) {
            xml = new XmlConfig();
        }
        if (java == null) {
            java = new JavaConfig();
        }
        if (openapi == null) {
            openapi = new OpenApiConfig();
        }
        if (parser == null) {
            parser = new ParserConfig();
        }
        if (audit == null) {
            audit = new AuditConfig();
        }
        if (validation == null) {
            validation = new ValidationConfig();
        }
        if (loggingLevel == null || loggingLevel.isEmpty()) {
            loggingLevel = "INFO";
        }

        output.setDefaults();
        xml.setDefaults();
        java.setDefaults();
        openapi.setDefaults();
        parser.setDefaults();
        audit.setDefaults();
        validation.setDefaults();
    }

    /**
     * Merges another configuration into this one.
     * Non-null values from other override values in this config.
     *
     * @param other the configuration to merge from
     */
    public void merge(Config other) {
        if (other == null) {
            return;
        }
        if (other.output != null) {
            if (this.output == null) {
                this.output = new OutputConfig();
            }
            this.output.merge(other.output);
        }
        if (other.xml != null) {
            if (this.xml == null) {
                this.xml = new XmlConfig();
            }
            this.xml.merge(other.xml);
        }
        if (other.java != null) {
            if (this.java == null) {
                this.java = new JavaConfig();
            }
            this.java.merge(other.java);
        }
        if (other.openapi != null) {
            if (this.openapi == null) {
                this.openapi = new OpenApiConfig();
            }
            this.openapi.merge(other.openapi);
        }
        if (other.parser != null) {
            if (this.parser == null) {
                this.parser = new ParserConfig();
            }
            this.parser.merge(other.parser);
        }
        if (other.audit != null) {
            if (this.audit == null) {
                this.audit = new AuditConfig();
            }
            this.audit.merge(other.audit);
        }
        if (other.validation != null) {
            if (this.validation == null) {
                this.validation = new ValidationConfig();
            }
            this.validation.merge(other.validation);
        }
        if (other.loggingLevel != null && !other.loggingLevel.isEmpty()) {
            this.loggingLevel = other.loggingLevel;
        }
    }
}
