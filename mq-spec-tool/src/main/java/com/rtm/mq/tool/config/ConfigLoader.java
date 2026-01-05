package com.rtm.mq.tool.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and validates configuration from YAML files and CLI arguments.
 *
 * <p>Configuration priority (highest to lowest):
 * <ol>
 *   <li>CLI arguments</li>
 *   <li>Configuration file</li>
 *   <li>Default values</li>
 * </ol>
 */
public class ConfigLoader {
    private final Yaml yaml;

    public ConfigLoader() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new Constructor(Config.class, loaderOptions));
    }

    /**
     * Loads configuration with the specified priority: CLI > Config file > Defaults.
     *
     * @param configFile   path to YAML configuration file, may be null
     * @param cliOverrides map of CLI argument names to values, may be null or empty
     * @return fully initialized and validated configuration
     * @throws ConfigException if configuration is invalid or cannot be loaded
     */
    public Config load(Path configFile, Map<String, String> cliOverrides) {
        Config config = new Config();

        // 1. Set defaults
        config.setDefaults();

        // 2. Load config file
        if (configFile != null && Files.exists(configFile)) {
            Config fileConfig = loadFromFile(configFile);
            if (fileConfig != null) {
                config.merge(fileConfig);
            }
        }

        // 3. Apply CLI overrides
        if (cliOverrides != null && !cliOverrides.isEmpty()) {
            applyCliOverrides(config, cliOverrides);
        }

        // 4. Validate configuration
        validate(config);

        return config;
    }

    /**
     * Loads configuration from a YAML file.
     *
     * @param path the path to the configuration file
     * @return the loaded configuration, or null if file is empty
     * @throws ConfigException if the file cannot be read or parsed
     */
    Config loadFromFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return yaml.loadAs(is, Config.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to load config file: " + path, e);
        } catch (Exception e) {
            throw new ConfigException("Failed to parse config file: " + path + " - " + e.getMessage(), e);
        }
    }

    /**
     * Applies CLI argument overrides to the configuration.
     *
     * @param config    the configuration to modify
     * @param overrides map of CLI argument names to values
     */
    void applyCliOverrides(Config config, Map<String, String> overrides) {
        if (overrides == null) {
            return;
        }

        if (overrides.containsKey("output-dir")) {
            String value = overrides.get("output-dir");
            if (value != null && !value.isEmpty()) {
                config.getOutput().setRootDir(value);
            }
        }

        if (overrides.containsKey("max-nesting-depth")) {
            String value = overrides.get("max-nesting-depth");
            if (value != null && !value.isEmpty()) {
                try {
                    config.getParser().setMaxNestingDepth(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new ConfigException("Invalid value for max-nesting-depth: " + value);
                }
            }
        }

        if (overrides.containsKey("logging-level")) {
            String value = overrides.get("logging-level");
            if (value != null && !value.isEmpty()) {
                config.setLoggingLevel(value);
            }
        }

        if (overrides.containsKey("use-lombok")) {
            String value = overrides.get("use-lombok");
            if (value != null) {
                config.getJava().setUseLombok(Boolean.parseBoolean(value));
            }
        }

        if (overrides.containsKey("openapi-version")) {
            String value = overrides.get("openapi-version");
            if (value != null && !value.isEmpty()) {
                config.getOpenapi().setVersion(value);
            }
        }

        if (overrides.containsKey("split-schemas")) {
            String value = overrides.get("split-schemas");
            if (value != null) {
                config.getOpenapi().setSplitSchemas(Boolean.parseBoolean(value));
            }
        }

        if (overrides.containsKey("hash-outputs")) {
            String value = overrides.get("hash-outputs");
            if (value != null) {
                config.getAudit().setHashOutputs(Boolean.parseBoolean(value));
            }
        }

        if (overrides.containsKey("redact-file-paths")) {
            String value = overrides.get("redact-file-paths");
            if (value != null) {
                config.getAudit().setRedactFilePaths(Boolean.parseBoolean(value));
            }
        }

        if (overrides.containsKey("redact-payload")) {
            String value = overrides.get("redact-payload");
            if (value != null) {
                config.getValidation().setRedactPayload(Boolean.parseBoolean(value));
            }
        }

        // XML namespace overrides
        if (overrides.containsKey("xml-namespace-inbound")) {
            String value = overrides.get("xml-namespace-inbound");
            if (value != null && !value.isEmpty()) {
                config.getXml().getNamespace().setInbound(value);
            }
        }

        if (overrides.containsKey("xml-namespace-outbound")) {
            String value = overrides.get("xml-namespace-outbound");
            if (value != null && !value.isEmpty()) {
                config.getXml().getNamespace().setOutbound(value);
            }
        }

        // Project overrides
        if (overrides.containsKey("xml-project-groupId")) {
            String value = overrides.get("xml-project-groupId");
            if (value != null && !value.isEmpty()) {
                config.getXml().getProject().setGroupId(value);
            }
        }

        if (overrides.containsKey("xml-project-artifactId")) {
            String value = overrides.get("xml-project-artifactId");
            if (value != null && !value.isEmpty()) {
                config.getXml().getProject().setArtifactId(value);
            }
        }

        if (overrides.containsKey("java-package")) {
            String value = overrides.get("java-package");
            if (value != null && !value.isEmpty()) {
                config.getJava().setPackageName(value);
            }
        }
    }

    /**
     * Validates that required configuration fields are present.
     *
     * @param config the configuration to validate
     * @throws ConfigException if required fields are missing
     */
    public void validate(Config config) {
        List<String> errors = new ArrayList<>();

        if (config.getXml() == null || config.getXml().getNamespace() == null) {
            errors.add("xml.namespace is required");
        } else {
            if (config.getXml().getNamespace().getInbound() == null ||
                    config.getXml().getNamespace().getInbound().isEmpty()) {
                errors.add("xml.namespace.inbound is required");
            }
            if (config.getXml().getNamespace().getOutbound() == null ||
                    config.getXml().getNamespace().getOutbound().isEmpty()) {
                errors.add("xml.namespace.outbound is required");
            }
        }

        if (config.getXml() == null || config.getXml().getProject() == null) {
            errors.add("xml.project is required");
        } else {
            if (config.getXml().getProject().getGroupId() == null ||
                    config.getXml().getProject().getGroupId().isEmpty()) {
                errors.add("xml.project.groupId is required");
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigException(
                    "Configuration validation failed:\n" + String.join("\n", errors));
        }
    }
}
