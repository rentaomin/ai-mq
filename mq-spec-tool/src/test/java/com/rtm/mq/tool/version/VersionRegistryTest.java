package com.rtm.mq.tool.version;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VersionRegistry Tests")
class VersionRegistryTest {

    @BeforeEach
    void setUp() {
        // Reset state before each test to ensure isolation
        VersionRegistry.reset();
    }

    @AfterEach
    void tearDown() {
        // Reset state after each test
        VersionRegistry.reset();
    }

    @Nested
    @DisplayName("Read Existing Version Properties Tests")
    class ReadExistingVersionPropertiesTests {

        @Test
        @DisplayName("should read parser version correctly")
        void readParserVersion() {
            // When
            String version = VersionRegistry.getParserVersion();

            // Then
            assertEquals("1.0.0", version);
        }

        @Test
        @DisplayName("should read XML template version correctly")
        void readXmlTemplateVersion() {
            // When
            String version = VersionRegistry.getXmlTemplateVersion();

            // Then
            assertEquals("1.0.0", version);
        }

        @Test
        @DisplayName("should read Java template version correctly")
        void readJavaTemplateVersion() {
            // When
            String version = VersionRegistry.getJavaTemplateVersion();

            // Then
            assertEquals("1.0.0", version);
        }

        @Test
        @DisplayName("should read YAML template version correctly")
        void readYamlTemplateVersion() {
            // When
            String version = VersionRegistry.getYamlTemplateVersion();

            // Then
            assertEquals("1.0.0", version);
        }

        @Test
        @DisplayName("should read rules version correctly")
        void readRulesVersion() {
            // When
            String version = VersionRegistry.getRulesVersion();

            // Then
            assertEquals("1.0.0", version);
        }

        @Test
        @DisplayName("should read tool version correctly")
        void readToolVersion() {
            // When
            String version = VersionRegistry.getToolVersion();

            // Then
            assertEquals("1.0.0", version);
        }
    }

    @Nested
    @DisplayName("Version Summary Format Tests")
    class VersionSummaryFormatTests {

        @Test
        @DisplayName("should return correctly formatted version summary")
        void versionSummaryFormat() {
            // When
            String summary = VersionRegistry.getVersionSummary();

            // Then
            assertNotNull(summary);
            assertTrue(summary.contains("tool=1.0.0"));
            assertTrue(summary.contains("parser=1.0.0"));
            assertTrue(summary.contains("xml=1.0.0"));
            assertTrue(summary.contains("java=1.0.0"));
            assertTrue(summary.contains("yaml=1.0.0"));
            assertTrue(summary.contains("rules=1.0.0"));
        }

        @Test
        @DisplayName("should have correct format: tool=%s, parser=%s, xml=%s, java=%s, yaml=%s, rules=%s")
        void versionSummaryExactFormat() {
            // When
            String summary = VersionRegistry.getVersionSummary();

            // Then
            String expected = "tool=1.0.0, parser=1.0.0, xml=1.0.0, java=1.0.0, yaml=1.0.0, rules=1.0.0";
            assertEquals(expected, summary);
        }
    }

    @Nested
    @DisplayName("getAllVersions Tests")
    class GetAllVersionsTests {

        @Test
        @DisplayName("should return a copy of version properties")
        void getAllVersionsReturnsCopy() {
            // When
            Properties versions1 = VersionRegistry.getAllVersions();
            Properties versions2 = VersionRegistry.getAllVersions();

            // Then - should be different instances
            assertNotSame(versions1, versions2);
        }

        @Test
        @DisplayName("modifying returned properties should not affect registry")
        void modifyingReturnedPropertiesShouldNotAffectRegistry() {
            // Given
            Properties versions = VersionRegistry.getAllVersions();

            // When
            versions.setProperty("parser.version", "modified");

            // Then - original should be unchanged
            assertEquals("1.0.0", VersionRegistry.getParserVersion());
        }

        @Test
        @DisplayName("should contain all version keys")
        void shouldContainAllVersionKeys() {
            // When
            Properties versions = VersionRegistry.getAllVersions();

            // Then
            assertTrue(versions.containsKey("parser.version"));
            assertTrue(versions.containsKey("xml.template.version"));
            assertTrue(versions.containsKey("java.template.version"));
            assertTrue(versions.containsKey("yaml.template.version"));
            assertTrue(versions.containsKey("rules.version"));
            assertTrue(versions.containsKey("tool.version"));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent access safely")
        void concurrentAccessSafety() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            boolean[] results = new boolean[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        String version = VersionRegistry.getParserVersion();
                        results[index] = "1.0.0".equals(version);
                    } catch (Exception e) {
                        results[index] = false;
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            for (int i = 0; i < threadCount; i++) {
                assertTrue(results[i], "Thread " + i + " failed");
            }
        }

        @Test
        @DisplayName("should load only once even with concurrent access")
        void loadOnlyOnceWithConcurrentAccess() throws InterruptedException {
            // Given
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            String[] versions = new String[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    versions[index] = VersionRegistry.getToolVersion();
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then - all threads should get the same value
            for (int i = 0; i < threadCount; i++) {
                assertEquals("1.0.0", versions[i]);
            }
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("reset should clear loaded state")
        void resetShouldClearLoadedState() {
            // Given
            String versionBefore = VersionRegistry.getParserVersion();
            assertEquals("1.0.0", versionBefore);

            // When
            VersionRegistry.reset();

            // Then - can reload
            String versionAfter = VersionRegistry.getParserVersion();
            assertEquals("1.0.0", versionAfter);
        }
    }
}
