package com.rtm.mq.tool.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link E2eResultWriter}.
 */
class E2eResultWriterTest {

    private E2eResultWriter writer;

    @BeforeEach
    void setUp() {
        writer = new E2eResultWriter();
    }

    @Test
    void testOutputFilename() {
        assertEquals("e2e-verification-result.json", E2eResultWriter.OUTPUT_FILENAME);
    }

    @Test
    void testWrite_nullResult_throwsException(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(null, tempDir));
    }

    @Test
    void testWrite_nullDirectory() {
        E2eVerificationResult result = new E2eVerificationResult();
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(result, (Path) null));
    }

    @Test
    void testWrite_createsFile(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testWrite_validJson(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        String content = Files.readString(outputPath);

        // Verify it starts and ends correctly as JSON
        assertTrue(content.startsWith("{"));
        assertTrue(content.trim().endsWith("}"));
    }

    @Test
    void testWrite_containsExpectedFields(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        String content = Files.readString(outputPath);

        assertTrue(content.contains("\"passed\""));
        assertTrue(content.contains("\"exitCode\""));
        assertTrue(content.contains("\"completeness\""));
        assertTrue(content.contains("\"determinism\""));
        assertTrue(content.contains("\"atomicity\""));
        assertTrue(content.contains("\"auditIntegrity\""));
        assertTrue(content.contains("\"cliIntegration\""));
    }

    @Test
    void testWrite_deterministicOutput(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result1 = new E2eVerificationResult();
        E2eVerificationResult result2 = new E2eVerificationResult();

        Path dir1 = tempDir.resolve("run1");
        Path dir2 = tempDir.resolve("run2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        writer.write(result1, dir1);
        writer.write(result2, dir2);

        String content1 = Files.readString(dir1.resolve(E2eResultWriter.OUTPUT_FILENAME));
        String content2 = Files.readString(dir2.resolve(E2eResultWriter.OUTPUT_FILENAME));

        assertEquals(content1, content2);
    }

    @Test
    void testWrite_toWriter() throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();
        StringWriter sw = new StringWriter();

        writer.write(result, sw);

        String content = sw.toString();
        assertTrue(content.startsWith("{"));
        assertTrue(content.contains("\"passed\": true"));
    }

    @Test
    void testWrite_toWriter_nullResult() {
        StringWriter sw = new StringWriter();
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(null, sw));
    }

    @Test
    void testWrite_toWriter_nullWriter() {
        E2eVerificationResult result = new E2eVerificationResult();
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(result, (java.io.Writer) null));
    }

    @Test
    void testWrite_jsonEscaping(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();
        // Add a check with special characters that need escaping
        result.getCompleteness().addCheck("test\"check", true);

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        String content = Files.readString(outputPath);

        // The quote should be escaped
        assertTrue(content.contains("\\\""));
    }

    @Test
    void testWrite_failedResult(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getCompleteness().setPassed(false);
        result.computeOverallResult();

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        String content = Files.readString(outputPath);

        assertTrue(content.contains("\"passed\": false"));
        assertTrue(content.contains("\"exitCode\": 81"));
    }

    @Test
    void testWrite_createsDirectoryIfNotExists(@TempDir Path tempDir) throws IOException {
        Path nestedDir = tempDir.resolve("nested").resolve("output");
        E2eVerificationResult result = new E2eVerificationResult();

        writer.write(result, nestedDir);

        assertTrue(Files.exists(nestedDir));
        assertTrue(Files.exists(nestedDir.resolve(E2eResultWriter.OUTPUT_FILENAME)));
    }

    @Test
    void testWrite_checksArray(@TempDir Path tempDir) throws IOException {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getCompleteness().addCheck("check1", true);
        result.getCompleteness().addCheck("check2", false);

        writer.write(result, tempDir);

        Path outputPath = tempDir.resolve(E2eResultWriter.OUTPUT_FILENAME);
        String content = Files.readString(outputPath);

        assertTrue(content.contains("\"check1:true\""));
        assertTrue(content.contains("\"check2:false\""));
    }
}
