package com.rtm.mq.tool.cli;

/**
 * Interface for CLI command handlers.
 *
 * <p>Each supported command (generate, validate, parse, version, help)
 * implements this interface. Commands are resolved by name and delegated
 * to without inlining business logic in the CLI entry point.</p>
 */
public interface Command {

    /**
     * Returns the name of this command.
     *
     * <p>This name is used for command resolution from CLI arguments.</p>
     *
     * @return the command name (e.g., "generate", "validate", "parse", "version", "help")
     */
    String getName();

    /**
     * Executes the command with the given context.
     *
     * <p>Implementations should perform their specific logic and return
     * an appropriate exit code. Exceptions should be caught and converted
     * to exit codes where possible.</p>
     *
     * @param context the CLI context containing configuration, options, and audit logger
     * @return the exit code for this command's execution
     */
    int execute(CliContext context);

    /**
     * Returns a short description of this command for help output.
     *
     * @return a one-line description of what this command does
     */
    String getDescription();
}
