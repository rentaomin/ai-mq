package com.rtm.mq.tool.e2e;

/**
 * Exit codes for E2E verification as specified in T-310.
 *
 * <p>These codes are fixed and non-extensible.</p>
 */
public final class E2eExitCodes {

    private E2eExitCodes() {
        // Utility class - prevent instantiation
    }

    /** E2E verification passed. */
    public static final int E2E_VERIFICATION_PASS = 0;

    /** Completeness verification failed. */
    public static final int E2E_COMPLETENESS_FAILED = 81;

    /** Determinism verification failed. */
    public static final int E2E_DETERMINISM_FAILED = 82;

    /** Atomicity verification failed. */
    public static final int E2E_ATOMICITY_FAILED = 83;

    /** Audit integrity verification failed. */
    public static final int E2E_AUDIT_INTEGRITY_FAILED = 84;

    /** CLI integration verification failed. */
    public static final int E2E_CLI_INTEGRATION_FAILED = 85;

    /**
     * Gets a description for an E2E exit code.
     *
     * @param code the exit code
     * @return the description
     */
    public static String getDescription(int code) {
        switch (code) {
            case E2E_VERIFICATION_PASS:
                return "E2E verification passed";
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
                return "Unknown E2E exit code";
        }
    }

    /**
     * Checks if the given code is a valid E2E exit code.
     *
     * @param code the exit code to check
     * @return true if it is a valid E2E exit code
     */
    public static boolean isValidE2eExitCode(int code) {
        return code == E2E_VERIFICATION_PASS
                || code == E2E_COMPLETENESS_FAILED
                || code == E2E_DETERMINISM_FAILED
                || code == E2E_ATOMICITY_FAILED
                || code == E2E_AUDIT_INTEGRITY_FAILED
                || code == E2E_CLI_INTEGRATION_FAILED;
    }
}
