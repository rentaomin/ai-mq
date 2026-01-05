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

    /** Internal error - unexpected system failure. */
    public static final int INTERNAL_ERROR = 99;

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
            case INTERNAL_ERROR:
                return "Internal error";
            default:
                return "Unknown error";
        }
    }
}
