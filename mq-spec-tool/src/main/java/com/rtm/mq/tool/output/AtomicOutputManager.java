package com.rtm.mq.tool.output;

import com.rtm.mq.tool.model.ConsistencyReport;
import com.rtm.mq.tool.model.ValidationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Manages atomic output operations for generated artifacts.
 *
 * <p>This class ensures that all generated artifacts are written in a fully atomic manner:
 * either all outputs are committed successfully, or no output state is changed.</p>
 *
 * <h2>Transaction Semantics (T-307)</h2>
 * <ul>
 *   <li>All output operations are executed within a single transaction</li>
 *   <li>A transaction has exactly two terminal states: COMMITTED or ROLLED_BACK</li>
 *   <li>Partial commit is forbidden</li>
 * </ul>
 *
 * <h2>Atomic Write Strategy</h2>
 * <ul>
 *   <li>All outputs are written to a temporary location first</li>
 *   <li>No file is written directly to the target output directory</li>
 *   <li>Commit replaces the target output atomically</li>
 *   <li>On failure, the original output state remains unchanged</li>
 * </ul>
 */
public class AtomicOutputManager {

    private static final String MANIFEST_FILENAME = "output-manifest.json";
    private static final String TEMP_DIR_PREFIX = ".mq-temp-";

    private Path targetDir;
    private String transactionId;
    private Path tempDir;
    private TransactionState state;
    private final Map<String, byte[]> pendingOutputs;
    private OutputManifest manifest;

    public AtomicOutputManager() {
        this.pendingOutputs = new LinkedHashMap<>();
        this.state = TransactionState.PENDING;
    }

    public AtomicOutputManager(Path targetDir) {
        this.targetDir = Objects.requireNonNull(targetDir, "targetDir must not be null");
        this.transactionId = UUID.randomUUID().toString();
        this.state = TransactionState.PENDING;
        this.pendingOutputs = new LinkedHashMap<>();
    }

    public void initialize(String transactionId, Path outputDir) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.targetDir = Objects.requireNonNull(outputDir, "outputDir must not be null");
        this.state = TransactionState.PENDING;
        this.tempDir = null;
        this.pendingOutputs.clear();
        this.manifest = null;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Path getOutputDirectory(String transactionId) {
        if (this.transactionId == null || !this.transactionId.equals(transactionId)) {
            throw new IllegalStateException("Transaction ID mismatch or not initialized");
        }
        return this.targetDir;
    }

    /**
     * Gets the current transaction state.
     *
     * @return the transaction state
     */
    public TransactionState getState() {
        return state;
    }

    /**
     * Gets the output manifest (available after successful commit).
     *
     * @return the manifest, or null if not committed
     */
    public OutputManifest getManifest() {
        return manifest;
    }

    /**
     * Checks commit preconditions.
     *
     * <p>A transaction may be committed ONLY if:</p>
     * <ul>
     *   <li>Consistency validation result indicates PASS</li>
     *   <li>Message validation result indicates PASS (if enabled)</li>
     *   <li>Output directory is writable</li>
     *   <li>Sufficient disk space is available</li>
     * </ul>
     *
     * @param consistencyReport the consistency validation result (required)
     * @param messageValidation the message validation result (may be null if disabled)
     * @throws AtomicOutputException if any precondition fails
     */
    public void checkPreconditions(ConsistencyReport consistencyReport,
                                   ValidationResult messageValidation) {
        Objects.requireNonNull(consistencyReport, "consistencyReport must not be null");

        // Check consistency validation result
        if (!consistencyReport.isSuccess()) {
            throw AtomicOutputException.consistencyValidationFailed(
                    "Consistency report has " + consistencyReport.getErrorCount() + " error(s)");
        }

        // Check message validation result (if enabled)
        if (messageValidation != null && !messageValidation.isSuccess()) {
            throw AtomicOutputException.messageValidationFailed(
                    "Message validation has " + messageValidation.getErrors().size() + " error(s)");
        }

        // Check output directory is writable
        checkOutputWritable();

        // Check disk space (estimated based on pending outputs)
        checkDiskSpace();
    }

    /**
     * Adds an output file to the pending transaction.
     *
     * <p>The file is NOT written to disk until commit() is called.</p>
     *
     * @param relativePath the relative path from output directory
     * @param content the file content
     * @throws IllegalStateException if transaction is not in PENDING state
     */
    public void addOutput(String relativePath, String content) {
        addOutput(relativePath, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds an output file to the pending transaction.
     *
     * <p>The file is NOT written to disk until commit() is called.</p>
     *
     * @param relativePath the relative path from output directory
     * @param content the file content as bytes
     * @throws IllegalStateException if transaction is not in PENDING state
     */
    public void addOutput(String relativePath, byte[] content) {
        if (state != TransactionState.PENDING) {
            throw new IllegalStateException(
                    "Cannot add output: transaction is " + state);
        }
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Objects.requireNonNull(content, "content must not be null");
        pendingOutputs.put(relativePath, content);
    }

    /**
     * Adds multiple outputs from a generator result map.
     *
     * @param outputs map of relative paths to content strings
     * @throws IllegalStateException if transaction is not in PENDING state
     */
    public void addOutputs(Map<String, String> outputs) {
        if (outputs == null) {
            return;
        }
        for (Map.Entry<String, String> entry : outputs.entrySet()) {
            addOutput(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Commits all pending outputs atomically.
     *
     * <p>This operation:</p>
     * <ol>
     *   <li>Writes all outputs to a temporary directory</li>
     *   <li>Generates the output manifest</li>
     *   <li>Atomically moves files to the target directory</li>
     *   <li>Cleans up the temporary directory</li>
     * </ol>
     *
     * <p>If any step fails, rollback() is called automatically.</p>
     *
     * @return the generated output manifest
     * @throws AtomicOutputException if commit fails
     * @throws IllegalStateException if transaction is not in PENDING state
     */
    public OutputManifest commit() {
        if (state != TransactionState.PENDING) {
            throw new IllegalStateException(
                    "Cannot commit: transaction is " + state);
        }

        try {
            // Create temporary directory
            createTempDir();

            // Write all outputs to temp directory
            List<OutputFileEntry> fileEntries = writeToTempDir();

            // Generate manifest
            this.manifest = new OutputManifest(transactionId, Instant.now().toString(), fileEntries);

            // Write manifest to temp directory
            String manifestJson = serializeManifest(manifest);
            Path manifestTempPath = tempDir.resolve(MANIFEST_FILENAME);
            Files.write(manifestTempPath, manifestJson.getBytes(StandardCharsets.UTF_8));

            // Ensure target directory exists
            Files.createDirectories(targetDir);

            // Atomic move: move all files from temp to target
            moveToTarget();

            // Clean up temp directory
            deleteTempDir();

            state = TransactionState.COMMITTED;
            return manifest;

        } catch (Exception e) {
            // Attempt rollback on any failure
            try {
                rollback();
            } catch (Exception rollbackEx) {
                throw AtomicOutputException.rollbackFailed(
                        "Failed during commit recovery: " + rollbackEx.getMessage(), e);
            }

            if (e instanceof AtomicOutputException) {
                throw (AtomicOutputException) e;
            }
            throw AtomicOutputException.atomicCommitFailed(e.getMessage(), e);
        }
    }

    /**
     * Rolls back the transaction, removing all temporary outputs.
     *
     * <p>After rollback:</p>
     * <ul>
     *   <li>All temporary outputs are removed</li>
     *   <li>Target output directory remains unchanged</li>
     * </ul>
     *
     * @throws AtomicOutputException if rollback fails
     */
    public void rollback() {
        if (state == TransactionState.COMMITTED) {
            throw new IllegalStateException(
                    "Cannot rollback: transaction is already COMMITTED");
        }

        try {
            deleteTempDir();
            state = TransactionState.ROLLED_BACK;
        } catch (Exception e) {
            throw AtomicOutputException.rollbackFailed(
                    "Failed to delete temporary directory: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helper methods
    // -------------------------------------------------------------------------

    private void checkOutputWritable() {
        try {
            Path parentDir = targetDir.getParent();
            if (parentDir == null) {
                parentDir = targetDir;
            }

            // If target exists, check if writable
            if (Files.exists(targetDir)) {
                if (!Files.isWritable(targetDir)) {
                    throw AtomicOutputException.outputNotWritable(targetDir.toString());
                }
            } else {
                // Check if parent is writable (for creating target)
                if (Files.exists(parentDir) && !Files.isWritable(parentDir)) {
                    throw AtomicOutputException.outputNotWritable(parentDir.toString());
                }
            }
        } catch (AtomicOutputException e) {
            throw e;
        } catch (Exception e) {
            throw AtomicOutputException.outputNotWritable(
                    targetDir.toString() + " - " + e.getMessage());
        }
    }

    private void checkDiskSpace() {
        long requiredBytes = pendingOutputs.values().stream()
                .mapToLong(bytes -> bytes.length)
                .sum();

        // Add buffer for manifest and overhead (10% or minimum 1KB)
        requiredBytes = (long) (requiredBytes * 1.1) + 1024;

        try {
            Path checkPath = Files.exists(targetDir) ? targetDir : targetDir.getParent();
            if (checkPath == null) {
                checkPath = Path.of(".");
            }
            if (!Files.exists(checkPath)) {
                Files.createDirectories(checkPath);
            }

            FileStore store = Files.getFileStore(checkPath);
            long availableBytes = store.getUsableSpace();

            if (availableBytes < requiredBytes) {
                throw AtomicOutputException.insufficientDiskSpace(requiredBytes, availableBytes);
            }
        } catch (AtomicOutputException e) {
            throw e;
        } catch (IOException e) {
            // If we can't check disk space, proceed cautiously
            // (actual write will fail if space is insufficient)
        }
    }

    private void createTempDir() throws IOException {
        Path parentDir = targetDir.getParent();
        if (parentDir == null) {
            parentDir = Path.of(".");
        }
        Files.createDirectories(parentDir);
        tempDir = Files.createTempDirectory(parentDir, TEMP_DIR_PREFIX + transactionId.substring(0, 8) + "-");
    }

    private List<OutputFileEntry> writeToTempDir() throws IOException, NoSuchAlgorithmException {
        List<OutputFileEntry> entries = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (Map.Entry<String, byte[]> entry : pendingOutputs.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content = entry.getValue();

            // Create parent directories if needed
            Path filePath = tempDir.resolve(relativePath);
            Files.createDirectories(filePath.getParent());

            // Write file
            Files.write(filePath, content);

            // Calculate hash
            digest.reset();
            byte[] hashBytes = digest.digest(content);
            String hashHex = bytesToHex(hashBytes);

            entries.add(new OutputFileEntry(relativePath, content.length, hashHex));
        }

        return entries;
    }

    private void moveToTarget() throws IOException {
        // First, move all files from temp to target
        try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(sourcePath -> {
                        try {
                            Path relativePath = tempDir.relativize(sourcePath);
                            Path targetPath = targetDir.resolve(relativePath);
                            Files.createDirectories(targetPath.getParent());
                            Files.move(sourcePath, targetPath,
                                    StandardCopyOption.REPLACE_EXISTING,
                                    StandardCopyOption.ATOMIC_MOVE);
                        } catch (IOException e) {
                            // On Windows, ATOMIC_MOVE might not be supported across volumes
                            // Fall back to copy + delete
                            try {
                                Path relativePath = tempDir.relativize(sourcePath);
                                Path targetPath = targetDir.resolve(relativePath);
                                Files.createDirectories(targetPath.getParent());
                                Files.copy(sourcePath, targetPath,
                                        StandardCopyOption.REPLACE_EXISTING);
                                Files.delete(sourcePath);
                            } catch (IOException copyEx) {
                                throw new RuntimeException(
                                        "Failed to move file: " + sourcePath, copyEx);
                            }
                        }
                    });
        }
    }

    private void deleteTempDir() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Best effort cleanup
                            }
                        });
            }
        }
    }

    private String serializeManifest(OutputManifest manifest) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"transactionId\": \"").append(escapeJson(manifest.getTransactionId())).append("\",\n");
        sb.append("  \"timestamp\": \"").append(escapeJson(manifest.getTimestamp())).append("\",\n");
        sb.append("  \"files\": [\n");

        List<OutputFileEntry> files = manifest.getFiles();
        for (int i = 0; i < files.size(); i++) {
            OutputFileEntry file = files.get(i);
            sb.append("    {\n");
            sb.append("      \"relativePath\": \"").append(escapeJson(file.getRelativePath())).append("\",\n");
            sb.append("      \"sizeBytes\": ").append(file.getSizeBytes()).append(",\n");
            sb.append("      \"sha256Hash\": \"").append(escapeJson(file.getSha256Hash())).append("\"\n");
            sb.append("    }");
            if (i < files.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
