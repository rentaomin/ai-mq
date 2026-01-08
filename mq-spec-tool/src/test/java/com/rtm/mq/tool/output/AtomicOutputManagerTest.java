package com.rtm.mq.tool.output;

import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.model.ConsistencyReport;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AtomicOutputManager}.
 */
class AtomicOutputManagerTest {

    @TempDir
    Path tempDir;

    private Path targetDir;
    private AtomicOutputManager manager;

    @BeforeEach
    void setUp() {
        targetDir = tempDir.resolve("output");
        manager = new AtomicOutputManager(targetDir);
    }

    @Test
    void testConstructor_initializesState() {
        assertNotNull(manager.getTransactionId());
        assertEquals(TransactionState.PENDING, manager.getState());
        assertNull(manager.getManifest());
    }

    @Test
    void testConstructor_nullTargetDir_throwsException() {
        assertThrows(NullPointerException.class, () -> new AtomicOutputManager(null));
    }

    @Test
    void testAddOutput_stringContent() {
        manager.addOutput("test.txt", "Hello, World!");
        assertEquals(TransactionState.PENDING, manager.getState());
    }

    @Test
    void testAddOutput_byteContent() {
        manager.addOutput("test.bin", new byte[]{1, 2, 3});
        assertEquals(TransactionState.PENDING, manager.getState());
    }

    @Test
    void testAddOutput_nullPath_throwsException() {
        assertThrows(NullPointerException.class, () -> manager.addOutput(null, "content"));
    }

    @Test
    void testAddOutput_nullContent_throwsException() {
        assertThrows(NullPointerException.class, () -> manager.addOutput("test.txt", (String) null));
    }

    @Test
    void testAddOutputs_fromMap() {
        Map<String, String> outputs = new HashMap<>();
        outputs.put("file1.txt", "content1");
        outputs.put("file2.txt", "content2");
        manager.addOutputs(outputs);
        assertEquals(TransactionState.PENDING, manager.getState());
    }

    @Test
    void testAddOutputs_nullMap_noException() {
        assertDoesNotThrow(() -> manager.addOutputs(null));
    }

    @Test
    void testCheckPreconditions_successfulValidation() {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        assertDoesNotThrow(() -> manager.checkPreconditions(report, null));
    }

    @Test
    void testCheckPreconditions_consistencyFailed_throwsException() {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(false);
        report.setErrorCount(1);

        AtomicOutputException ex = assertThrows(AtomicOutputException.class,
                () -> manager.checkPreconditions(report, null));
        assertEquals(ExitCodes.CONSISTENCY_VALIDATION_FAILED, ex.getExitCode());
    }

    @Test
    void testCheckPreconditions_messageFailed_throwsException() {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);

        ValidationResult messageResult = new ValidationResult();
        messageResult.addError(new ValidationError("Test error", "path", "value"));

        AtomicOutputException ex = assertThrows(AtomicOutputException.class,
                () -> manager.checkPreconditions(report, messageResult));
        assertEquals(ExitCodes.MESSAGE_VALIDATION_FAILED, ex.getExitCode());
    }

    @Test
    void testCheckPreconditions_nullConsistencyReport_throwsException() {
        assertThrows(NullPointerException.class,
                () -> manager.checkPreconditions(null, null));
    }

    @Test
    void testCommit_writesFilesToTarget() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello, World!");
        manager.addOutput("subdir/nested.txt", "Nested content");

        OutputManifest manifest = manager.commit();

        assertEquals(TransactionState.COMMITTED, manager.getState());
        assertNotNull(manifest);
        assertEquals(manager.getTransactionId(), manifest.getTransactionId());
        assertEquals(2, manifest.getFileCount());

        // Verify files exist
        assertTrue(Files.exists(targetDir.resolve("test.txt")));
        assertTrue(Files.exists(targetDir.resolve("subdir/nested.txt")));
        assertTrue(Files.exists(targetDir.resolve("output-manifest.json")));

        // Verify content
        String content = Files.readString(targetDir.resolve("test.txt"));
        assertEquals("Hello, World!", content);
    }

    @Test
    void testCommit_generatesManifestWithCorrectHashes() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello");

        OutputManifest manifest = manager.commit();

        assertEquals(1, manifest.getFiles().size());
        OutputFileEntry entry = manifest.getFiles().get(0);
        assertEquals("test.txt", entry.getRelativePath());
        assertEquals(5, entry.getSizeBytes());
        assertNotNull(entry.getSha256Hash());
        assertEquals(64, entry.getSha256Hash().length()); // SHA-256 hex = 64 chars
    }

    @Test
    void testCommit_afterCommit_throwsException() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello");
        manager.commit();

        assertThrows(IllegalStateException.class, () -> manager.commit());
    }

    @Test
    void testAddOutput_afterCommit_throwsException() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello");
        manager.commit();

        assertThrows(IllegalStateException.class, () -> manager.addOutput("new.txt", "content"));
    }

    @Test
    void testRollback_cleansUpTempFiles() {
        manager.addOutput("test.txt", "Hello");
        manager.rollback();

        assertEquals(TransactionState.ROLLED_BACK, manager.getState());
        assertFalse(Files.exists(targetDir.resolve("test.txt")));
    }

    @Test
    void testRollback_afterCommit_throwsException() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello");
        manager.commit();

        assertThrows(IllegalStateException.class, () -> manager.rollback());
    }

    @Test
    void testManifestFormat_containsRequiredFields() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        manager.addOutput("test.txt", "Hello");
        manager.commit();

        String manifestContent = Files.readString(targetDir.resolve("output-manifest.json"));
        assertTrue(manifestContent.contains("transactionId"));
        assertTrue(manifestContent.contains("timestamp"));
        assertTrue(manifestContent.contains("files"));
        assertTrue(manifestContent.contains("relativePath"));
        assertTrue(manifestContent.contains("sizeBytes"));
        assertTrue(manifestContent.contains("sha256Hash"));
    }

    @Test
    void testCommit_preservesFileOrder() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        // Add files in specific order
        manager.addOutput("a.txt", "A");
        manager.addOutput("b.txt", "B");
        manager.addOutput("c.txt", "C");

        OutputManifest manifest = manager.commit();

        // Verify order is preserved
        assertEquals("a.txt", manifest.getFiles().get(0).getRelativePath());
        assertEquals("b.txt", manifest.getFiles().get(1).getRelativePath());
        assertEquals("c.txt", manifest.getFiles().get(2).getRelativePath());
    }

    @Test
    void testCommit_emptyOutputs_succeeds() throws IOException {
        ConsistencyReport report = new ConsistencyReport();
        report.setSuccess(true);
        manager.checkPreconditions(report, null);

        // Commit with no outputs
        OutputManifest manifest = manager.commit();

        assertEquals(TransactionState.COMMITTED, manager.getState());
        assertEquals(0, manifest.getFileCount());
        assertTrue(Files.exists(targetDir.resolve("output-manifest.json")));
    }

    @Test
    void testAtomicOutputException_exitCodes() {
        AtomicOutputException ex1 = AtomicOutputException.outputNotWritable("/path");
        assertEquals(ExitCodes.OUTPUT_NOT_WRITABLE, ex1.getExitCode());

        AtomicOutputException ex2 = AtomicOutputException.insufficientDiskSpace(1000, 500);
        assertEquals(ExitCodes.INSUFFICIENT_DISK_SPACE, ex2.getExitCode());

        AtomicOutputException ex3 = AtomicOutputException.consistencyValidationFailed("reason");
        assertEquals(ExitCodes.CONSISTENCY_VALIDATION_FAILED, ex3.getExitCode());

        AtomicOutputException ex4 = AtomicOutputException.messageValidationFailed("reason");
        assertEquals(ExitCodes.MESSAGE_VALIDATION_FAILED, ex4.getExitCode());

        AtomicOutputException ex5 = AtomicOutputException.atomicCommitFailed("reason");
        assertEquals(ExitCodes.ATOMIC_COMMIT_FAILED, ex5.getExitCode());

        AtomicOutputException ex6 = AtomicOutputException.rollbackFailed("reason");
        assertEquals(ExitCodes.ROLLBACK_FAILED, ex6.getExitCode());
    }
}
