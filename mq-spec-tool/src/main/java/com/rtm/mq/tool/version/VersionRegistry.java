package com.rtm.mq.tool.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Version Registry
 * Provides unified access to component version numbers
 */
public final class VersionRegistry {
    private static final String VERSIONS_FILE = "/versions.properties";
    private static final Properties versions = new Properties();
    private static boolean loaded = false;

    private VersionRegistry() {}

    /**
     * Load version configuration
     */
    private static synchronized void ensureLoaded() {
        if (loaded) return;

        try (InputStream is = VersionRegistry.class.getResourceAsStream(VERSIONS_FILE)) {
            if (is == null) {
                throw new RuntimeException("Cannot find " + VERSIONS_FILE);
            }
            versions.load(is);
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load versions", e);
        }
    }

    /**
     * Get parser version
     */
    public static String getParserVersion() {
        ensureLoaded();
        return versions.getProperty("parser.version", "unknown");
    }

    /**
     * Get XML template version
     */
    public static String getXmlTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("xml.template.version", "unknown");
    }

    /**
     * Get Java template version
     */
    public static String getJavaTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("java.template.version", "unknown");
    }

    /**
     * Get YAML template version
     */
    public static String getYamlTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("yaml.template.version", "unknown");
    }

    /**
     * Get rules version
     */
    public static String getRulesVersion() {
        ensureLoaded();
        return versions.getProperty("rules.version", "unknown");
    }

    /**
     * Get tool version
     */
    public static String getToolVersion() {
        ensureLoaded();
        return versions.getProperty("tool.version", "unknown");
    }

    /**
     * Get all version information
     * @return copy of version properties
     */
    public static Properties getAllVersions() {
        ensureLoaded();
        Properties copy = new Properties();
        copy.putAll(versions);
        return copy;
    }

    /**
     * Get version summary string (for audit logs)
     */
    public static String getVersionSummary() {
        ensureLoaded();
        return String.format(
            "tool=%s, parser=%s, xml=%s, java=%s, yaml=%s, rules=%s",
            getToolVersion(),
            getParserVersion(),
            getXmlTemplateVersion(),
            getJavaTemplateVersion(),
            getYamlTemplateVersion(),
            getRulesVersion()
        );
    }

    /**
     * Reset load state (for testing only)
     */
    static void reset() {
        loaded = false;
        versions.clear();
    }
}
