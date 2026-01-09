package com.rtm.mq.tool.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for spec generation API.
 *
 * <p>This class provides transaction metadata and file manifest after successful generation.</p>
 */
public class GenerationResponse {

    /**
     * Unique transaction ID for this generation operation.
     */
    private String transactionId;

    /**
     * Transaction status (PENDING, COMMITTED, ROLLED_BACK).
     */
    private String status;

    /**
     * Generation timestamp (ISO 8601 format).
     */
    private String timestamp;

    /**
     * List of generated file paths (relative to output directory).
     */
    private List<String> generatedFiles;

    /**
     * Optional message (e.g., warnings, info).
     */
    private String message;

    // Constructors

    public GenerationResponse() {
        this.timestamp = Instant.now().toString();
    }

    public GenerationResponse(String transactionId, String status, List<String> generatedFiles) {
        this();
        this.transactionId = transactionId;
        this.status = status;
        this.generatedFiles = generatedFiles;
    }

    // Getters and setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
