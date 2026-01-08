package com.rtm.mq.tool.output;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the output manifest for an atomic transaction.
 *
 * <p>The manifest is generated as part of a successful commit and contains:</p>
 * <ul>
 *   <li>Transaction identifier (UUID)</li>
 *   <li>Generation timestamp (ISO-8601)</li>
 *   <li>List of output files with size and hash</li>
 * </ul>
 *
 * <p>The manifest is written atomically with other outputs.</p>
 */
public final class OutputManifest {
    private final String transactionId;
    private final String timestamp;
    private final List<OutputFileEntry> files;

    /**
     * Creates a new output manifest.
     *
     * @param transactionId the unique transaction identifier
     * @param timestamp the generation timestamp in ISO-8601 format
     * @param files the list of output file entries
     */
    public OutputManifest(String transactionId, String timestamp, List<OutputFileEntry> files) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.files = files != null ? new ArrayList<>(files) : new ArrayList<>();
    }

    /**
     * Creates a new output manifest with current timestamp.
     *
     * @param transactionId the unique transaction identifier
     * @param files the list of output file entries
     */
    public OutputManifest(String transactionId, List<OutputFileEntry> files) {
        this(transactionId, Instant.now().toString(), files);
    }

    /**
     * Gets the transaction identifier.
     *
     * @return the unique transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the generation timestamp.
     *
     * @return the timestamp in ISO-8601 format
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the list of output files.
     *
     * @return an unmodifiable list of file entries
     */
    public List<OutputFileEntry> getFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Gets the total number of files in the manifest.
     *
     * @return the file count
     */
    public int getFileCount() {
        return files.size();
    }

    /**
     * Gets the total size of all files in bytes.
     *
     * @return the total size
     */
    public long getTotalSizeBytes() {
        return files.stream().mapToLong(OutputFileEntry::getSizeBytes).sum();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OutputManifest that = (OutputManifest) obj;
        return Objects.equals(transactionId, that.transactionId)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, timestamp, files);
    }

    @Override
    public String toString() {
        return "OutputManifest{" +
                "transactionId='" + transactionId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", fileCount=" + files.size() +
                ", totalSizeBytes=" + getTotalSizeBytes() +
                '}';
    }
}
