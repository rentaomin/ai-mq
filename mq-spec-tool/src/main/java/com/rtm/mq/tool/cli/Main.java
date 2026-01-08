package com.rtm.mq.tool.cli;

import com.rtm.mq.tool.audit.AuditLogger;
import com.rtm.mq.tool.cli.command.GenerateCommand;
import com.rtm.mq.tool.cli.command.HelpCommand;
import com.rtm.mq.tool.cli.command.ParseCommand;
import com.rtm.mq.tool.cli.command.ValidateCommand;
import com.rtm.mq.tool.cli.command.VersionCommand;
import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ConfigException;
import com.rtm.mq.tool.config.ConfigLoader;
import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.exception.MqToolException;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Main entry point for the MQ Spec Tool CLI.
 *
 * <p>This class implements the CLI entry point gate as specified in T-309.
 * It handles:</p>
 * <ul>
 *   <li>Command-line argument parsing</li>
 *   <li>Configuration loading with priority: CLI > Config file > Environment</li>
 *   <li>Command resolution and routing</li>
 *   <li>Audit initialization and finalization</li>
 *   <li>Exit code propagation</li>
 * </ul>
 *
 * <p>Supported commands: generate, validate, parse, version, help</p>
 */
public final class Main {

    private final CommandRegistry registry;
    private final ConfigLoader configLoader;
    private final PrintStream out;
    private final PrintStream err;
    private AuditLogger auditLogger;

    /**
     * Creates a new Main instance with default configuration.
     */
    public Main() {
        this(System.out, System.err);
    }

    /**
     * Creates a new Main instance with custom output streams.
     *
     * @param out standard output stream
     * @param err error output stream
     */
    public Main(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.registry = new CommandRegistry();
        this.configLoader = new ConfigLoader();
        registerCommands();
    }

    /**
     * Registers all supported commands.
     */
    private void registerCommands() {
        registry.register(new GenerateCommand());
        registry.register(new ValidateCommand());
        registry.register(new ParseCommand());
        registry.register(new VersionCommand());
        registry.register(new HelpCommand(registry));
    }

    /**
     * Sets the audit logger for this CLI instance.
     *
     * @param auditLogger the audit logger to use
     */
    public void setAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Runs the CLI with the given arguments.
     *
     * @param args command-line arguments
     * @return the exit code
     */
    public int run(String[] args) {
        int exitCode = ExitCodes.SUCCESS;
        CliOptions options = null;
        String commandName = null;

        try {
            // 1. Parse command-line arguments
            options = CliOptions.parse(args);
            commandName = options.getCommand();

            // 2. Handle help request
            if (options.isHelpRequested() || commandName == null) {
                if (commandName == null && !options.isHelpRequested()) {
                    err.println("Error: No command specified.");
                    err.println();
                    exitCode = ExitCodes.CLI_MISSING_COMMAND;
                }
                printHelp();
                return commandName == null && !options.isHelpRequested()
                        ? ExitCodes.CLI_MISSING_COMMAND
                        : ExitCodes.SUCCESS;
            }

            // 3. Resolve command
            Optional<Command> commandOpt = registry.lookup(commandName);
            if (!commandOpt.isPresent()) {
                err.println("Error: Unknown command '" + commandName + "'");
                err.println("Use 'help' to see available commands.");
                return ExitCodes.CLI_UNKNOWN_COMMAND;
            }

            Command command = commandOpt.get();

            // 4. Load configuration with priority: CLI > Config file > Defaults
            Config config = loadConfiguration(options);

            // 5. Initialize audit logger
            if (auditLogger != null) {
                auditLogger.initialize(commandName, options.getInputPaths(), options.getOutputPath());
                auditLogger.logLifecycle("TOOL_START", "Command: " + commandName);
            }

            // 6. Build context
            CliContext context = CliContext.builder()
                    .config(config)
                    .commandName(commandName)
                    .inputPaths(options.getInputPaths())
                    .outputPath(options.getOutputPath())
                    .configPath(options.getConfigPath())
                    .auditLogger(auditLogger)
                    .options(options.getOverrides())
                    .build();

            // 7. Execute command (delegate without inlining business logic)
            exitCode = command.execute(context);

            // 8. Log completion
            if (auditLogger != null) {
                auditLogger.logLifecycle("TOOL_COMPLETE", "Exit code: " + exitCode);
            }

        } catch (CliParseException e) {
            err.println("Error: " + e.getMessage());
            err.println("Use 'help' for usage information.");
            exitCode = e.getExitCode();
            if (auditLogger != null) {
                auditLogger.logLifecycle("TOOL_FAILURE", e.getMessage());
            }
        } catch (ConfigException e) {
            err.println("Configuration error: " + e.getMessage());
            exitCode = ExitCodes.CONFIG_ERROR;
            if (auditLogger != null) {
                auditLogger.logLifecycle("TOOL_FAILURE", e.getMessage());
            }
        } catch (MqToolException e) {
            err.println("Error: " + e.getMessage());
            exitCode = e.getExitCode();
            if (auditLogger != null) {
                auditLogger.logLifecycle("TOOL_FAILURE", e.getMessage());
            }
        } catch (Exception e) {
            err.println("Unexpected error: " + e.getMessage());
            exitCode = ExitCodes.INTERNAL_ERROR;
            if (auditLogger != null) {
                auditLogger.logLifecycle("TOOL_FAILURE", "Internal error: " + e.getMessage());
            }
        } finally {
            // 9. Finalize audit (exactly once per invocation)
            if (auditLogger != null) {
                auditLogger.finalize(exitCode);
            }
        }

        return exitCode;
    }

    /**
     * Loads configuration with priority: CLI > Config file > Defaults.
     *
     * @param options the parsed CLI options
     * @return the merged configuration
     */
    private Config loadConfiguration(CliOptions options) {
        Path configPath = options.getConfigPath();

        // Environment variable override for config path
        if (configPath == null) {
            String envConfigPath = System.getenv("MQ_SPEC_TOOL_CONFIG");
            if (envConfigPath != null && !envConfigPath.isEmpty()) {
                configPath = Path.of(envConfigPath);
            }
        }

        return configLoader.load(configPath, options.getOverrides());
    }

    /**
     * Prints help information.
     */
    private void printHelp() {
        out.println("MQ Spec Tool - Message Queue Specification Code Generator");
        out.println();
        out.println("Available commands:");
        out.println();
        for (Command cmd : registry.getAllCommands()) {
            out.printf("  %-12s %s%n", cmd.getName(), cmd.getDescription());
        }
        out.println();
        out.println(CliOptions.getHelpMessage());
    }

    /**
     * Main entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Main cli = new Main();
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }
}
