package com.rtm.mq.tool.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config POJO Tests")
class ConfigTest {

    @Nested
    @DisplayName("Config Tests")
    class MainConfigTests {

        @Test
        @DisplayName("should initialize all sub-configs on setDefaults")
        void setDefaultsInitializesAllSubConfigs() {
            // Given
            Config config = new Config();

            // When
            config.setDefaults();

            // Then
            assertNotNull(config.getOutput());
            assertNotNull(config.getXml());
            assertNotNull(config.getJava());
            assertNotNull(config.getOpenapi());
            assertNotNull(config.getParser());
            assertNotNull(config.getAudit());
            assertNotNull(config.getValidation());
            assertEquals("INFO", config.getLoggingLevel());
        }

        @Test
        @DisplayName("should not replace existing sub-configs on setDefaults")
        void setDefaultsDoesNotReplaceExisting() {
            // Given
            Config config = new Config();
            OutputConfig existingOutput = new OutputConfig();
            existingOutput.setRootDir("./existing");
            config.setOutput(existingOutput);

            // When
            config.setDefaults();

            // Then
            assertSame(existingOutput, config.getOutput());
            assertEquals("./existing", config.getOutput().getRootDir());
        }

        @Test
        @DisplayName("should handle null merge gracefully")
        void mergeWithNull() {
            // Given
            Config config = new Config();
            config.setDefaults();

            // When/Then - no exception
            assertDoesNotThrow(() -> config.merge(null));
        }
    }

    @Nested
    @DisplayName("OutputConfig Tests")
    class OutputConfigTests {

        @Test
        @DisplayName("should have correct default rootDir")
        void defaultRootDir() {
            // Given
            OutputConfig config = new OutputConfig();

            // When
            config.setDefaults();

            // Then
            assertEquals("./output", config.getRootDir());
        }

        @Test
        @DisplayName("should merge non-empty rootDir")
        void mergeRootDir() {
            // Given
            OutputConfig base = new OutputConfig();
            base.setRootDir("./base");

            OutputConfig other = new OutputConfig();
            other.setRootDir("./other");

            // When
            base.merge(other);

            // Then
            assertEquals("./other", base.getRootDir());
        }

        @Test
        @DisplayName("should not merge empty rootDir")
        void doNotMergeEmptyRootDir() {
            // Given
            OutputConfig base = new OutputConfig();
            base.setRootDir("./base");

            OutputConfig other = new OutputConfig();
            other.setRootDir("");

            // When
            base.merge(other);

            // Then
            assertEquals("./base", base.getRootDir());
        }
    }

    @Nested
    @DisplayName("XmlConfig Tests")
    class XmlConfigTests {

        @Test
        @DisplayName("should initialize nested configs on setDefaults")
        void setDefaultsInitializesNested() {
            // Given
            XmlConfig config = new XmlConfig();

            // When
            config.setDefaults();

            // Then
            assertNotNull(config.getNamespace());
            assertNotNull(config.getProject());
        }

        @Test
        @DisplayName("should merge namespace correctly")
        void mergeNamespace() {
            // Given
            XmlConfig base = new XmlConfig();
            base.setDefaults();

            XmlConfig other = new XmlConfig();
            other.setNamespace(new XmlConfig.NamespaceConfig());
            other.getNamespace().setInbound("other.inbound");

            // When
            base.merge(other);

            // Then
            assertEquals("other.inbound", base.getNamespace().getInbound());
        }

        @Test
        @DisplayName("should merge project correctly")
        void mergeProject() {
            // Given
            XmlConfig base = new XmlConfig();
            base.setDefaults();

            XmlConfig other = new XmlConfig();
            other.setProject(new XmlConfig.ProjectConfig());
            other.getProject().setGroupId("other.group");
            other.getProject().setArtifactId("other-artifact");

            // When
            base.merge(other);

            // Then
            assertEquals("other.group", base.getProject().getGroupId());
            assertEquals("other-artifact", base.getProject().getArtifactId());
        }
    }

    @Nested
    @DisplayName("JavaConfig Tests")
    class JavaConfigTests {

        @Test
        @DisplayName("should default useLombok to false")
        void defaultUseLombok() {
            // Given
            JavaConfig config = new JavaConfig();

            // Then
            assertFalse(config.isUseLombok());
        }

        @Test
        @DisplayName("should merge useLombok when true")
        void mergeUseLombok() {
            // Given
            JavaConfig base = new JavaConfig();
            base.setUseLombok(false);

            JavaConfig other = new JavaConfig();
            other.setUseLombok(true);

            // When
            base.merge(other);

            // Then
            assertTrue(base.isUseLombok());
        }

        @Test
        @DisplayName("should merge packageName")
        void mergePackageName() {
            // Given
            JavaConfig base = new JavaConfig();

            JavaConfig other = new JavaConfig();
            other.setPackageName("com.other.pkg");

            // When
            base.merge(other);

            // Then
            assertEquals("com.other.pkg", base.getPackageName());
        }
    }

    @Nested
    @DisplayName("OpenApiConfig Tests")
    class OpenApiConfigTests {

        @Test
        @DisplayName("should have correct defaults")
        void correctDefaults() {
            // Given
            OpenApiConfig config = new OpenApiConfig();

            // When
            config.setDefaults();

            // Then
            assertEquals("3.0.3", config.getVersion());
            assertTrue(config.isSplitSchemas());
        }

        @Test
        @DisplayName("should merge version")
        void mergeVersion() {
            // Given
            OpenApiConfig base = new OpenApiConfig();
            base.setVersion("3.0.3");

            OpenApiConfig other = new OpenApiConfig();
            other.setVersion("3.1.0");

            // When
            base.merge(other);

            // Then
            assertEquals("3.1.0", base.getVersion());
        }

        @Test
        @DisplayName("should merge splitSchemas")
        void mergeSplitSchemas() {
            // Given
            OpenApiConfig base = new OpenApiConfig();
            base.setSplitSchemas(true);

            OpenApiConfig other = new OpenApiConfig();
            other.setSplitSchemas(false);

            // When
            base.merge(other);

            // Then
            assertFalse(base.isSplitSchemas());
        }
    }

    @Nested
    @DisplayName("ParserConfig Tests")
    class ParserConfigTests {

        @Test
        @DisplayName("should have correct default maxNestingDepth")
        void defaultMaxNestingDepth() {
            // Given
            ParserConfig config = new ParserConfig();

            // Then
            assertEquals(50, config.getMaxNestingDepth());
        }

        @Test
        @DisplayName("should fix zero maxNestingDepth on setDefaults")
        void fixZeroMaxNestingDepth() {
            // Given
            ParserConfig config = new ParserConfig();
            config.setMaxNestingDepth(0);

            // When
            config.setDefaults();

            // Then
            assertEquals(50, config.getMaxNestingDepth());
        }

        @Test
        @DisplayName("should fix negative maxNestingDepth on setDefaults")
        void fixNegativeMaxNestingDepth() {
            // Given
            ParserConfig config = new ParserConfig();
            config.setMaxNestingDepth(-10);

            // When
            config.setDefaults();

            // Then
            assertEquals(50, config.getMaxNestingDepth());
        }

        @Test
        @DisplayName("should merge positive maxNestingDepth")
        void mergeMaxNestingDepth() {
            // Given
            ParserConfig base = new ParserConfig();
            base.setMaxNestingDepth(50);

            ParserConfig other = new ParserConfig();
            other.setMaxNestingDepth(100);

            // When
            base.merge(other);

            // Then
            assertEquals(100, base.getMaxNestingDepth());
        }

        @Test
        @DisplayName("should not merge non-positive maxNestingDepth")
        void doNotMergeNonPositiveMaxNestingDepth() {
            // Given
            ParserConfig base = new ParserConfig();
            base.setMaxNestingDepth(50);

            ParserConfig other = new ParserConfig();
            other.setMaxNestingDepth(0);

            // When
            base.merge(other);

            // Then
            assertEquals(50, base.getMaxNestingDepth());
        }
    }

    @Nested
    @DisplayName("AuditConfig Tests")
    class AuditConfigTests {

        @Test
        @DisplayName("should have correct defaults")
        void correctDefaults() {
            // Given
            AuditConfig config = new AuditConfig();

            // Then
            assertFalse(config.isHashOutputs());
            assertFalse(config.isRedactFilePaths());
        }

        @Test
        @DisplayName("should merge hashOutputs when true")
        void mergeHashOutputs() {
            // Given
            AuditConfig base = new AuditConfig();
            base.setHashOutputs(false);

            AuditConfig other = new AuditConfig();
            other.setHashOutputs(true);

            // When
            base.merge(other);

            // Then
            assertTrue(base.isHashOutputs());
        }

        @Test
        @DisplayName("should merge redactFilePaths when true")
        void mergeRedactFilePaths() {
            // Given
            AuditConfig base = new AuditConfig();
            base.setRedactFilePaths(false);

            AuditConfig other = new AuditConfig();
            other.setRedactFilePaths(true);

            // When
            base.merge(other);

            // Then
            assertTrue(base.isRedactFilePaths());
        }
    }

    @Nested
    @DisplayName("ValidationConfig Tests")
    class ValidationConfigTests {

        @Test
        @DisplayName("should have correct default")
        void correctDefault() {
            // Given
            ValidationConfig config = new ValidationConfig();

            // Then
            assertTrue(config.isRedactPayload());
        }

        @Test
        @DisplayName("should merge redactPayload")
        void mergeRedactPayload() {
            // Given
            ValidationConfig base = new ValidationConfig();
            base.setRedactPayload(true);

            ValidationConfig other = new ValidationConfig();
            other.setRedactPayload(false);

            // When
            base.merge(other);

            // Then
            assertFalse(base.isRedactPayload());
        }
    }
}
