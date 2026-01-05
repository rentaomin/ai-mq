package com.rtm.mq.tool.version;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VersionInfo Tests")
class VersionInfoTest {

    @BeforeEach
    void setUp() {
        VersionRegistry.reset();
    }

    @AfterEach
    void tearDown() {
        VersionRegistry.reset();
    }

    @Nested
    @DisplayName("fromRegistry Tests")
    class FromRegistryTests {

        @Test
        @DisplayName("should create VersionInfo from registry")
        void createFromRegistry() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertNotNull(info);
        }

        @Test
        @DisplayName("should contain parser version from registry")
        void containsParserVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getParser());
        }

        @Test
        @DisplayName("should contain XML template version from registry")
        void containsXmlTemplateVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getXmlTemplate());
        }

        @Test
        @DisplayName("should contain Java template version from registry")
        void containsJavaTemplateVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getJavaTemplate());
        }

        @Test
        @DisplayName("should contain YAML template version from registry")
        void containsYamlTemplateVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getYamlTemplate());
        }

        @Test
        @DisplayName("should contain rules version from registry")
        void containsRulesVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getRules());
        }

        @Test
        @DisplayName("should contain tool version from registry")
        void containsToolVersion() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertEquals("1.0.0", info.getTool());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("all getters should return non-null values")
        void allGettersReturnNonNull() {
            // When
            VersionInfo info = VersionInfo.fromRegistry();

            // Then
            assertNotNull(info.getParser());
            assertNotNull(info.getXmlTemplate());
            assertNotNull(info.getJavaTemplate());
            assertNotNull(info.getYamlTemplate());
            assertNotNull(info.getRules());
            assertNotNull(info.getTool());
        }
    }
}
