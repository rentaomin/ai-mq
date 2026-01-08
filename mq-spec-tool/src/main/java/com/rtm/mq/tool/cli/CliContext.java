package com.rtm.mq.tool.cli;

import com.rtm.mq.tool.audit.AuditLogger;
import com.rtm.mq.tool.config.Config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Context object passed to command handlers.
 *
 * <p>Contains all information needed for command execution including
 * merged configuration, input paths, output path, and audit logger.</p>
 */
public final class CliContext {

    private final Config config;
    private final String commandName;
    private final List<Path> inputPaths;
    private final Path outputPath;
    private final Path configPath;
    private final AuditLogger auditLogger;
    private final Map<String, String> options;

    private CliContext(Builder builder) {
        this.config = builder.config;
        this.commandName = builder.commandName;
        this.inputPaths = builder.inputPaths != null
                ? Collections.unmodifiableList(builder.inputPaths)
                : Collections.emptyList();
        this.outputPath = builder.outputPath;
        this.configPath = builder.configPath;
        this.auditLogger = builder.auditLogger;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<>(builder.options));
    }

    /**
     * Returns the merged configuration.
     *
     * @return the configuration object
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the resolved command name.
     *
     * @return the command name
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Returns the list of input paths.
     *
     * @return unmodifiable list of input paths
     */
    public List<Path> getInputPaths() {
        return inputPaths;
    }

    /**
     * Returns the output directory path.
     *
     * @return the output path, may be null
     */
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Returns the configuration file path.
     *
     * @return the config file path, may be null
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Returns the audit logger.
     *
     * @return the audit logger
     */
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    /**
     * Returns additional options as key-value pairs.
     *
     * @return unmodifiable map of options
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Gets an option value by key.
     *
     * @param key the option key
     * @return the option value, or null if not present
     */
    public String getOption(String key) {
        return options.get(key);
    }

    /**
     * Checks if an option is present.
     *
     * @param key the option key
     * @return true if the option is present
     */
    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    /**
     * Creates a new builder for CliContext.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CliContext.
     */
    public static final class Builder {
        private Config config;
        private String commandName;
        private List<Path> inputPaths;
        private Path outputPath;
        private Path configPath;
        private AuditLogger auditLogger;
        private final Map<String, String> options = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder config(Config config) {
            this.config = config;
            return this;
        }

        public Builder commandName(String commandName) {
            this.commandName = commandName;
            return this;
        }

        public Builder inputPaths(List<Path> inputPaths) {
            this.inputPaths = inputPaths;
            return this;
        }

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder configPath(Path configPath) {
            this.configPath = configPath;
            return this;
        }

        public Builder auditLogger(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
            return this;
        }

        public Builder option(String key, String value) {
            this.options.put(key, value);
            return this;
        }

        public Builder options(Map<String, String> options) {
            this.options.putAll(options);
            return this;
        }

        public CliContext build() {
            return new CliContext(this);
        }
    }
}
