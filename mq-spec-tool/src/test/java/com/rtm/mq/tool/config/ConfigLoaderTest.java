package com.rtm.mq.tool.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigLoader Tests")
class ConfigLoaderTest {

    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new ConfigLoader();
    }

    @Nested
    @DisplayName("Load Valid Config File Tests")
    class LoadValidConfigFileTests {

        @Test
        @DisplayName("should load valid config file with all fields")
        void loadValidConfigFile() {
            // Given
            Path configPath = Path.of("src/test/resources/valid-config.yaml");

            // When
            Config config = configLoader.load(configPath, null);

            // Then
            assertEquals("./test-output", config.getOutput().getRootDir());
            assertEquals("com.example.inbound", config.getXml().getNamespace().getInbound());
            assertEquals("com.example.outbound", config.getXml().getNamespace().getOutbound());
            assertEquals("com.example", config.getXml().getProject().getGroupId());
            assertEquals("test-project", config.getXml().getProject().getArtifactId());
            assertTrue(config.getJava().isUseLombok());
            assertEquals("com.example.model", config.getJava().getPackageName());
            assertEquals(100, config.getParser().getMaxNestingDepth());
            assertEquals("3.1.0", config.getOpenapi().getVersion());
            assertFalse(config.getOpenapi().isSplitSchemas());
            assertTrue(config.getAudit().isHashOutputs());
            assertTrue(config.getAudit().isRedactFilePaths());
            assertFalse(config.getValidation().isRedactPayload());
            assertEquals("DEBUG", config.getLoggingLevel());
        }

        @Test
        @DisplayName("should load partial config file and use defaults for missing fields")
        void loadPartialConfigFile() {
            // Given
            Path configPath = Path.of("src/test/resources/partial-config.yaml");

            // When
            Config config = configLoader.load(configPath, null);

            // Then
            // Required fields from file
            assertEquals("com.partial.inbound", config.getXml().getNamespace().getInbound());
            assertEquals("com.partial.outbound", config.getXml().getNamespace().getOutbound());
            assertEquals("com.partial", config.getXml().getProject().getGroupId());

            // Default values
            assertEquals("./output", config.getOutput().getRootDir());
            assertFalse(config.getJava().isUseLombok());
            assertEquals(50, config.getParser().getMaxNestingDepth());
            assertEquals("3.0.3", config.getOpenapi().getVersion());
            assertTrue(config.getOpenapi().isSplitSchemas());
            assertFalse(config.getAudit().isHashOutputs());
            assertTrue(config.getValidation().isRedactPayload());
            assertEquals("INFO", config.getLoggingLevel());
        }
    }

    @Nested
    @DisplayName("Load Invalid Config File Tests")
    class LoadInvalidConfigFileTests {

        @Test
        @DisplayName("should throw ConfigException for missing required fields")
        void loadConfigFileMissingRequiredFields() {
            // Given
            Path configPath = Path.of("src/test/resources/invalid-config.yaml");

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.load(configPath, null));

            assertTrue(exception.getMessage().contains("xml.namespace.inbound is required"));
            assertTrue(exception.getMessage().contains("xml.namespace.outbound is required"));
            assertTrue(exception.getMessage().contains("xml.project.groupId is required"));
        }

        @Test
        @DisplayName("should throw ConfigException for malformed YAML")
        void loadMalformedConfigFile() {
            // Given
            Path configPath = Path.of("src/test/resources/malformed-config.yaml");

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.load(configPath, null));

            assertTrue(exception.getMessage().contains("Failed to parse config file"));
        }

        @Test
        @DisplayName("should throw ConfigException when no config file and no required CLI args")
        void loadWithoutConfigFileOrRequiredCliArgs() {
            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.load(null, null));

            assertTrue(exception.getMessage().contains("Configuration validation failed"));
        }
    }

    @Nested
    @DisplayName("CLI Override Tests")
    class CliOverrideTests {

        @Test
        @DisplayName("CLI arguments should override config file values")
        void cliOverridesConfigFile() {
            // Given
            Path configPath = Path.of("src/test/resources/valid-config.yaml");
            Map<String, String> cliOverrides = new HashMap<>();
            cliOverrides.put("output-dir", "./cli-output");
            cliOverrides.put("max-nesting-depth", "200");
            cliOverrides.put("logging-level", "WARN");

            // When
            Config config = configLoader.load(configPath, cliOverrides);

            // Then - CLI values should override file values
            assertEquals("./cli-output", config.getOutput().getRootDir());
            assertEquals(200, config.getParser().getMaxNestingDepth());
            assertEquals("WARN", config.getLoggingLevel());

            // Non-overridden values should remain from file
            assertEquals("com.example.inbound", config.getXml().getNamespace().getInbound());
        }

        @Test
        @DisplayName("CLI arguments should override defaults when no config file")
        void cliOverridesDefaultsWithRequiredFields() {
            // Given
            Map<String, String> cliOverrides = new HashMap<>();
            cliOverrides.put("xml-namespace-inbound", "cli.inbound");
            cliOverrides.put("xml-namespace-outbound", "cli.outbound");
            cliOverrides.put("xml-project-groupId", "cli.group");
            cliOverrides.put("output-dir", "./cli-output");
            cliOverrides.put("max-nesting-depth", "75");

            // When
            Config config = configLoader.load(null, cliOverrides);

            // Then
            assertEquals("cli.inbound", config.getXml().getNamespace().getInbound());
            assertEquals("cli.outbound", config.getXml().getNamespace().getOutbound());
            assertEquals("cli.group", config.getXml().getProject().getGroupId());
            assertEquals("./cli-output", config.getOutput().getRootDir());
            assertEquals(75, config.getParser().getMaxNestingDepth());
        }

        @Test
        @DisplayName("should throw ConfigException for invalid numeric CLI value")
        void cliInvalidNumericValue() {
            // Given
            Path configPath = Path.of("src/test/resources/valid-config.yaml");
            Map<String, String> cliOverrides = new HashMap<>();
            cliOverrides.put("max-nesting-depth", "not-a-number");

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.load(configPath, cliOverrides));

            assertTrue(exception.getMessage().contains("Invalid value for max-nesting-depth"));
        }

        @Test
        @DisplayName("should handle all CLI override options")
        void allCliOverrideOptions() {
            // Given
            Map<String, String> cliOverrides = new HashMap<>();
            cliOverrides.put("xml-namespace-inbound", "cli.inbound");
            cliOverrides.put("xml-namespace-outbound", "cli.outbound");
            cliOverrides.put("xml-project-groupId", "cli.group");
            cliOverrides.put("xml-project-artifactId", "cli-artifact");
            cliOverrides.put("java-package", "cli.java.pkg");
            cliOverrides.put("use-lombok", "true");
            cliOverrides.put("openapi-version", "3.1.0");
            cliOverrides.put("split-schemas", "false");
            cliOverrides.put("hash-outputs", "true");
            cliOverrides.put("redact-file-paths", "true");
            cliOverrides.put("redact-payload", "false");

            // When
            Config config = configLoader.load(null, cliOverrides);

            // Then
            assertEquals("cli.inbound", config.getXml().getNamespace().getInbound());
            assertEquals("cli.outbound", config.getXml().getNamespace().getOutbound());
            assertEquals("cli.group", config.getXml().getProject().getGroupId());
            assertEquals("cli-artifact", config.getXml().getProject().getArtifactId());
            assertEquals("cli.java.pkg", config.getJava().getPackageName());
            assertTrue(config.getJava().isUseLombok());
            assertEquals("3.1.0", config.getOpenapi().getVersion());
            assertFalse(config.getOpenapi().isSplitSchemas());
            assertTrue(config.getAudit().isHashOutputs());
            assertTrue(config.getAudit().isRedactFilePaths());
            assertFalse(config.getValidation().isRedactPayload());
        }
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("should set correct default values")
        void defaultValuesAreCorrect() {
            // Given
            Config config = new Config();

            // When
            config.setDefaults();

            // Then
            assertNotNull(config.getOutput());
            assertEquals("./output", config.getOutput().getRootDir());

            assertNotNull(config.getXml());
            assertNotNull(config.getXml().getNamespace());
            assertNotNull(config.getXml().getProject());

            assertNotNull(config.getJava());
            assertFalse(config.getJava().isUseLombok());

            assertNotNull(config.getParser());
            assertEquals(50, config.getParser().getMaxNestingDepth());

            assertNotNull(config.getOpenapi());
            assertEquals("3.0.3", config.getOpenapi().getVersion());
            assertTrue(config.getOpenapi().isSplitSchemas());

            assertNotNull(config.getAudit());
            assertFalse(config.getAudit().isHashOutputs());
            assertFalse(config.getAudit().isRedactFilePaths());

            assertNotNull(config.getValidation());
            assertTrue(config.getValidation().isRedactPayload());

            assertEquals("INFO", config.getLoggingLevel());
        }

        @Test
        @DisplayName("should fix invalid maxNestingDepth on setDefaults")
        void fixInvalidMaxNestingDepth() {
            // Given
            ParserConfig parserConfig = new ParserConfig();
            parserConfig.setMaxNestingDepth(-1);

            // When
            parserConfig.setDefaults();

            // Then
            assertEquals(50, parserConfig.getMaxNestingDepth());
        }
    }

    @Nested
    @DisplayName("Config File Not Found Tests")
    class ConfigFileNotFoundTests {

        @Test
        @DisplayName("should use defaults when config file does not exist but CLI provides required fields")
        void nonExistentConfigFileWithCliArgs() {
            // Given
            Path configPath = Path.of("non-existent-config.yaml");
            Map<String, String> cliOverrides = new HashMap<>();
            cliOverrides.put("xml-namespace-inbound", "cli.inbound");
            cliOverrides.put("xml-namespace-outbound", "cli.outbound");
            cliOverrides.put("xml-project-groupId", "cli.group");

            // When
            Config config = configLoader.load(configPath, cliOverrides);

            // Then - should use defaults + CLI overrides
            assertEquals("./output", config.getOutput().getRootDir());
            assertEquals("cli.inbound", config.getXml().getNamespace().getInbound());
        }
    }

    @Nested
    @DisplayName("Config Merge Tests")
    class ConfigMergeTests {

        @Test
        @DisplayName("should correctly merge two configs")
        void mergeConfigs() {
            // Given
            Config base = new Config();
            base.setDefaults();

            Config override = new Config();
            override.setOutput(new OutputConfig());
            override.getOutput().setRootDir("./merged-output");

            // When
            base.merge(override);

            // Then
            assertEquals("./merged-output", base.getOutput().getRootDir());
            // Other fields should remain at defaults
            assertEquals(50, base.getParser().getMaxNestingDepth());
        }

        @Test
        @DisplayName("should not override with null values")
        void mergeDoesNotOverrideWithNull() {
            // Given
            OutputConfig base = new OutputConfig();
            base.setRootDir("./original");

            OutputConfig other = new OutputConfig();
            // Set to null explicitly to test that null doesn't override
            other.setRootDir(null);

            // When
            base.merge(other);

            // Then
            assertEquals("./original", base.getRootDir());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should pass validation with all required fields")
        void validationPassesWithAllRequiredFields() {
            // Given
            Config config = new Config();
            config.setDefaults();
            config.getXml().getNamespace().setInbound("test.inbound");
            config.getXml().getNamespace().setOutbound("test.outbound");
            config.getXml().getProject().setGroupId("test.group");

            // When/Then - no exception
            assertDoesNotThrow(() -> configLoader.validate(config));
        }

        @Test
        @DisplayName("should fail validation when xml.namespace is null")
        void validationFailsWhenNamespaceNull() {
            // Given
            Config config = new Config();
            config.setDefaults();
            config.setXml(new XmlConfig());
            // namespace is null

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.validate(config));

            assertTrue(exception.getMessage().contains("xml.namespace is required"));
        }

        @Test
        @DisplayName("should fail validation when xml.project is null")
        void validationFailsWhenProjectNull() {
            // Given
            Config config = new Config();
            config.setDefaults();
            config.getXml().getNamespace().setInbound("test.inbound");
            config.getXml().getNamespace().setOutbound("test.outbound");
            config.getXml().setProject(null);

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.validate(config));

            assertTrue(exception.getMessage().contains("xml.project is required"));
        }

        @Test
        @DisplayName("should fail validation with empty required string")
        void validationFailsWithEmptyString() {
            // Given
            Config config = new Config();
            config.setDefaults();
            config.getXml().getNamespace().setInbound("");
            config.getXml().getNamespace().setOutbound("test.outbound");
            config.getXml().getProject().setGroupId("test.group");

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.validate(config));

            assertTrue(exception.getMessage().contains("xml.namespace.inbound is required"));
        }
    }

    @Nested
    @DisplayName("IO Error Handling Tests")
    class IOErrorHandlingTests {

        @Test
        @DisplayName("should throw ConfigException with cause for IO errors")
        void ioErrorHandling(@TempDir Path tempDir) throws IOException {
            // Given - create a directory with the config name (can't read as file)
            Path configPath = tempDir.resolve("config.yaml");
            Files.createDirectory(configPath);

            // When/Then
            ConfigException exception = assertThrows(ConfigException.class,
                    () -> configLoader.load(configPath, null));

            assertNotNull(exception.getCause());
        }
    }
}
