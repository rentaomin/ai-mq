package com.rtm.mq.tool.output;

import java.util.Objects;

/**
 * Represents a single file entry in the output manifest.
 *
 * <p>Contains metadata about an output file including:</p>
 * <ul>
 *   <li>Relative file path from output directory</li>
 *   <li>File size in bytes</li>
 *   <li>SHA-256 hash for integrity verification</li>
 * </ul>
 */
public final class OutputFileEntry {
    private final String relativePath;
    private final long sizeBytes;
    private final String sha256Hash;

    /**
     * Creates a new output file entry.
     *
     * @param relativePath the relative path from output directory
     * @param sizeBytes the file size in bytes
     * @param sha256Hash the SHA-256 hash of the file content
     */
    public OutputFileEntry(String relativePath, long sizeBytes, String sha256Hash) {
        this.relativePath = Objects.requireNonNull(relativePath, "relativePath must not be null");
        this.sizeBytes = sizeBytes;
        this.sha256Hash = Objects.requireNonNull(sha256Hash, "sha256Hash must not be null");
    }

    /**
     * Gets the relative file path.
     *
     * @return the relative path from output directory
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Gets the file size in bytes.
     *
     * @return the file size
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * Gets the SHA-256 hash of the file content.
     *
     * @return the SHA-256 hash as a hex string
     */
    public String getSha256Hash() {
        return sha256Hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OutputFileEntry that = (OutputFileEntry) obj;
        return sizeBytes == that.sizeBytes
                && Objects.equals(relativePath, that.relativePath)
                && Objects.equals(sha256Hash, that.sha256Hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, sizeBytes, sha256Hash);
    }

    @Override
    public String toString() {
        return "OutputFileEntry{" +
                "relativePath='" + relativePath + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", sha256Hash='" + sha256Hash + '\'' +
                '}';
    }
}
