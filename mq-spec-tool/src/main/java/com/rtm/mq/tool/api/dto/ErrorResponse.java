package com.rtm.mq.tool.api.dto;

/**
 * Standard error response DTO for REST API.
 */
public class ErrorResponse {

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error code (maps to ExitCodes).
     */
    private String code;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Error timestamp.
     */
    private String timestamp;

    /**
     * Request path that caused the error.
     */
    private String path;

    // Constructors

    public ErrorResponse() {
        this.timestamp = java.time.Instant.now().toString();
    }

    public ErrorResponse(int status, String code, String message, String path) {
        this();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    // Getters and setters

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
