package com.rtm.mq.tool.exception;

/**
 * Standard exit codes for the MQ tool.
 *
 * <p>These codes are used by the CLI to indicate the type of error
 * that occurred during execution. Each code corresponds to a specific
 * category of failure.</p>
 */
public final class ExitCodes {

    private ExitCodes() {
        // Utility class - prevent instantiation
    }

    /** Success - operation completed without errors. */
    public static final int SUCCESS = 0;

    /** Input validation error (e.g., file not found). */
    public static final int INPUT_VALIDATION_ERROR = 1;

    /** Parse error (e.g., invalid Seg lvl value). */
    public static final int PARSE_ERROR = 2;

    /** Generation error (e.g., template loading failure). */
    public static final int GENERATION_ERROR = 3;

    /** Validation error (e.g., cross-artifact consistency failure). */
    public static final int VALIDATION_ERROR = 4;

    /** Configuration error (e.g., required configuration missing). */
    public static final int CONFIG_ERROR = 5;

    /** I/O error (e.g., file read/write failure). */
    public static final int IO_ERROR = 6;

    /** Output error (e.g., output generation failure). */
    public static final int OUTPUT_ERROR = 7;

    /** Cross-artifact consistency validation failure. */
    public static final int CONSISTENCY_ERROR = 44;

    /** CLI argument parsing error. */
    public static final int CLI_ARGUMENT_ERROR = 10;

    /** CLI unknown command error. */
    public static final int CLI_UNKNOWN_COMMAND = 11;

    /** CLI missing command error. */
    public static final int CLI_MISSING_COMMAND = 12;

    /** Internal error - unexpected system failure. */
    public static final int INTERNAL_ERROR = 99;

    // Atomic Output Manager Exit Codes (T-307)

    /** Output directory is not writable. */
    public static final int OUTPUT_NOT_WRITABLE = 61;

    /** Insufficient disk space for output. */
    public static final int INSUFFICIENT_DISK_SPACE = 62;

    /** Consistency validation failed - cannot commit outputs. */
    public static final int CONSISTENCY_VALIDATION_FAILED = 63;

    /** Message validation failed - cannot commit outputs. */
    public static final int MESSAGE_VALIDATION_FAILED = 64;

    /** Atomic commit operation failed. */
    public static final int ATOMIC_COMMIT_FAILED = 65;

    /** Rollback operation failed. */
    public static final int ROLLBACK_FAILED = 66;

    // E2E Verification Exit Codes (T-310)

    /** E2E completeness verification failed. */
    public static final int E2E_COMPLETENESS_FAILED = 81;

    /** E2E determinism verification failed. */
    public static final int E2E_DETERMINISM_FAILED = 82;

    /** E2E atomicity verification failed. */
    public static final int E2E_ATOMICITY_FAILED = 83;

    /** E2E audit integrity verification failed. */
    public static final int E2E_AUDIT_INTEGRITY_FAILED = 84;

    /** E2E CLI integration verification failed. */
    public static final int E2E_CLI_INTEGRATION_FAILED = 85;

    /**
     * Gets a human-readable description for an exit code.
     *
     * @param code the exit code
     * @return a description of the exit code
     */
    public static String getDescription(int code) {
        switch (code) {
            case SUCCESS:
                return "Success";
            case INPUT_VALIDATION_ERROR:
                return "Input validation error";
            case PARSE_ERROR:
                return "Parse error";
            case GENERATION_ERROR:
                return "Generation error";
            case VALIDATION_ERROR:
                return "Validation error";
            case CONFIG_ERROR:
                return "Configuration error";
            case IO_ERROR:
                return "I/O error";
            case OUTPUT_ERROR:
                return "Output error";
            case CONSISTENCY_ERROR:
                return "Cross-artifact consistency error";
            case CLI_ARGUMENT_ERROR:
                return "CLI argument error";
            case CLI_UNKNOWN_COMMAND:
                return "Unknown command";
            case CLI_MISSING_COMMAND:
                return "Missing command";
            case INTERNAL_ERROR:
                return "Internal error";
            case OUTPUT_NOT_WRITABLE:
                return "Output directory not writable";
            case INSUFFICIENT_DISK_SPACE:
                return "Insufficient disk space";
            case CONSISTENCY_VALIDATION_FAILED:
                return "Consistency validation failed";
            case MESSAGE_VALIDATION_FAILED:
                return "Message validation failed";
            case ATOMIC_COMMIT_FAILED:
                return "Atomic commit failed";
            case ROLLBACK_FAILED:
                return "Rollback failed";
            case E2E_COMPLETENESS_FAILED:
                return "E2E completeness verification failed";
            case E2E_DETERMINISM_FAILED:
                return "E2E determinism verification failed";
            case E2E_ATOMICITY_FAILED:
                return "E2E atomicity verification failed";
            case E2E_AUDIT_INTEGRITY_FAILED:
                return "E2E audit integrity verification failed";
            case E2E_CLI_INTEGRATION_FAILED:
                return "E2E CLI integration verification failed";
            default:
                return "Unknown error";
        }
    }
}
