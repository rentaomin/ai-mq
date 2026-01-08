package com.rtm.mq.tool.audit;

import java.nio.file.Path;
import java.util.List;

/**
 * Audit Logger interface for recording execution events.
 *
 * <p>This interface defines the contract for audit logging as specified in T-308.
 * Implementations are responsible for recording deterministic, structured audit
 * information covering tool execution, inputs, outputs, and validation results.</p>
 *
 * <p>Required audit categories:</p>
 * <ul>
 *   <li>Lifecycle events (start, completion, failure)</li>
 *   <li>Input file metadata (path, hash, size, modification time)</li>
 *   <li>Process events (parse/generation started/completed)</li>
 *   <li>Validation results</li>
 *   <li>Output transaction events</li>
 * </ul>
 */
public interface AuditLogger {

    /**
     * Initializes the audit logger for a new execution.
     *
     * <p>This method MUST be called before any audit events are recorded.
     * It generates a correlation identifier that is shared across all
     * audit records for this execution.</p>
     *
     * @param commandName the name of the command being executed
     * @param inputPaths list of input file paths
     * @param outputPath the output directory path, may be null
     */
    void initialize(String commandName, List<Path> inputPaths, Path outputPath);

    /**
     * Records a lifecycle event.
     *
     * @param eventType the type of lifecycle event (e.g., "TOOL_START", "TOOL_COMPLETE", "TOOL_FAILURE")
     * @param message optional message with additional context
     */
    void logLifecycle(String eventType, String message);

    /**
     * Records input file metadata.
     *
     * @param inputPath the path to the input file
     */
    void logInput(Path inputPath);

    /**
     * Records a process event.
     *
     * @param processName the name of the process (e.g., "PARSE", "GENERATE")
     * @param eventType the event type (e.g., "STARTED", "COMPLETED")
     * @param message optional message
     */
    void logProcess(String processName, String eventType, String message);

    /**
     * Records a validation result.
     *
     * @param validationType the type of validation (e.g., "CONSISTENCY", "MESSAGE")
     * @param passed whether the validation passed
     * @param issueCount the number of issues found
     */
    void logValidation(String validationType, boolean passed, int issueCount);

    /**
     * Records an output transaction event.
     *
     * @param eventType the event type (e.g., "STARTED", "COMMITTED", "ROLLED_BACK")
     * @param message optional message
     */
    void logOutput(String eventType, String message);

    /**
     * Finalizes the audit log and writes output files.
     *
     * <p>This method MUST be called exactly once at the end of execution.
     * It writes the audit log files (audit-log.json and audit-log.txt)
     * and records the final exit code.</p>
     *
     * @param exitCode the exit code of the execution
     */
    void finalize(int exitCode);

    /**
     * Returns the correlation identifier for this execution.
     *
     * @return the correlation ID, or null if not initialized
     */
    String getCorrelationId();
}
