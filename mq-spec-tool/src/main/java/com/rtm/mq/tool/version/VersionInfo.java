package com.rtm.mq.tool.version;

/**
 * Version information structure class for JSON serialization
 */
public class VersionInfo {
    private String parser;
    private String xmlTemplate;
    private String javaTemplate;
    private String yamlTemplate;
    private String rules;
    private String tool;

    public static VersionInfo fromRegistry() {
        VersionInfo info = new VersionInfo();
        info.parser = VersionRegistry.getParserVersion();
        info.xmlTemplate = VersionRegistry.getXmlTemplateVersion();
        info.javaTemplate = VersionRegistry.getJavaTemplateVersion();
        info.yamlTemplate = VersionRegistry.getYamlTemplateVersion();
        info.rules = VersionRegistry.getRulesVersion();
        info.tool = VersionRegistry.getToolVersion();
        return info;
    }

    // Getters
    public String getParser() { return parser; }
    public String getXmlTemplate() { return xmlTemplate; }
    public String getJavaTemplate() { return javaTemplate; }
    public String getYamlTemplate() { return yamlTemplate; }
    public String getRules() { return rules; }
    public String getTool() { return tool; }
}
