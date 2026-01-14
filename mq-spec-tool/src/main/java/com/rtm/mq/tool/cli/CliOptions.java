package com.rtm.mq.tool.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI argument parser and option holder.
 *
 * <p>Parses command-line arguments using Apache Commons CLI.
 * Supports global options that can be used with any command.</p>
 *
 * <p>Option priority: CLI arguments > Config file > Environment variables</p>
 */
public final class CliOptions {

    private static final String PROGRAM_NAME = "mq-spec-tool";

    private final String command;
    private final List<Path> inputPaths;
    private final Path outputPath;
    private final Path configPath;
    private final Map<String, String> overrides;
    private final boolean helpRequested;

    private CliOptions(String command, List<Path> inputPaths, Path outputPath,
                       Path configPath, Map<String, String> overrides, boolean helpRequested) {
        this.command = command;
        this.inputPaths = inputPaths;
        this.outputPath = outputPath;
        this.configPath = configPath;
        this.overrides = overrides;
        this.helpRequested = helpRequested;
    }

    /**
     * Returns the resolved command name.
     *
     * @return the command name, or null if not specified
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the list of input file paths.
     *
     * @return list of input paths
     */
    public List<Path> getInputPaths() {
        return inputPaths;
    }

    /**
     * Returns the output directory path.
     *
     * @return the output path, or null if not specified
     */
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * Returns the configuration file path.
     *
     * @return the config path, or null if not specified
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Returns CLI overrides for configuration values.
     *
     * @return map of option names to values
     */
    public Map<String, String> getOverrides() {
        return overrides;
    }

    /**
     * Checks if help was requested.
     *
     * @return true if --help or -h was specified
     */
    public boolean isHelpRequested() {
        return helpRequested;
    }

    /**
     * Parses command-line arguments.
     *
     * @param args the command-line arguments
     * @return parsed CLI options
     * @throws CliParseException if parsing fails
     */
    public static CliOptions parse(String[] args) throws CliParseException {
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            // First pass: extract command and separate from options
            String command = null;
            List<String> argList = new ArrayList<>();
            boolean foundCommand = false;

            for (String arg : args) {
                if (!foundCommand && !arg.startsWith("-") && !arg.startsWith("--")) {
                    command = arg;
                    foundCommand = true;
                } else {
                    argList.add(arg);
                }
            }

            // Parse remaining options
            CommandLine cmd = parser.parse(options, argList.toArray(new String[0]));

            // Check for help request
            boolean helpRequested = cmd.hasOption("help");

            // Extract input paths
            List<Path> inputPaths = new ArrayList<>();
            if (cmd.hasOption("input")) {
                for (String input : cmd.getOptionValues("input")) {
                    inputPaths.add(Paths.get(input));
                }
            }

            // Extract MQ message path if present
            if (cmd.hasOption("mq-message")) {
                inputPaths.add(Paths.get(cmd.getOptionValue("mq-message")));
            }

            // Extract output path
            Path outputPath = cmd.hasOption("output")
                    ? Paths.get(cmd.getOptionValue("output"))
                    : null;

            // Extract config path
            Path configPath = cmd.hasOption("config")
                    ? Paths.get(cmd.getOptionValue("config"))
                    : null;

            // Build overrides map
            Map<String, String> overrides = new LinkedHashMap<>();
            extractOverride(cmd, "output-dir", overrides);
            extractOverride(cmd, "max-nesting-depth", overrides);
            extractOverride(cmd, "logging-level", overrides);
            extractOverride(cmd, "use-lombok", overrides);
            extractOverride(cmd, "openapi-version", overrides);
            extractOverride(cmd, "split-schemas", overrides);
            extractOverride(cmd, "hash-outputs", overrides);
            extractOverride(cmd, "redact-file-paths", overrides);
            extractOverride(cmd, "redact-payload", overrides);
            extractOverride(cmd, "xml-namespace-inbound", overrides);
            extractOverride(cmd, "xml-namespace-outbound", overrides);
            extractOverride(cmd, "xml-project-groupId", overrides);
            extractOverride(cmd, "xml-project-artifactId", overrides);
            extractOverride(cmd, "java-package", overrides);

            return new CliOptions(command, inputPaths, outputPath, configPath, overrides, helpRequested);

        } catch (ParseException e) {
            throw new CliParseException("Failed to parse arguments: " + e.getMessage(), e);
        }
    }

    private static void extractOverride(CommandLine cmd, String optionName, Map<String, String> overrides) {
        if (cmd.hasOption(optionName)) {
            overrides.put(optionName, cmd.getOptionValue(optionName));
        }
    }

    /**
     * Builds the CLI options definition.
     *
     * @return the Options object
     */
    private static Options buildOptions() {
        Options options = new Options();

        // Help option
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());

        // Input/output options
        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .desc("Input Excel specification file(s)")
                .build());

        options.addOption(Option.builder("m")
                .longOpt("mq-message")
                .hasArg()
                .desc("MQ message Excel file (for field reference)")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .desc("Output directory")
                .build());

        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg()
                .desc("Configuration file path")
                .build());

        // Configuration overrides
        options.addOption(Option.builder()
                .longOpt("output-dir")
                .hasArg()
                .desc("Override output.rootDir")
                .build());

        options.addOption(Option.builder()
                .longOpt("max-nesting-depth")
                .hasArg()
                .desc("Override parser.maxNestingDepth")
                .build());

        options.addOption(Option.builder()
                .longOpt("logging-level")
                .hasArg()
                .desc("Override logging level (DEBUG, INFO, WARN, ERROR)")
                .build());

        options.addOption(Option.builder()
                .longOpt("use-lombok")
                .hasArg()
                .desc("Override java.useLombok (true/false)")
                .build());

        options.addOption(Option.builder()
                .longOpt("openapi-version")
                .hasArg()
                .desc("Override openapi.version")
                .build());

        options.addOption(Option.builder()
                .longOpt("split-schemas")
                .hasArg()
                .desc("Override openapi.splitSchemas (true/false)")
                .build());

        options.addOption(Option.builder()
                .longOpt("hash-outputs")
                .hasArg()
                .desc("Override audit.hashOutputs (true/false)")
                .build());

        options.addOption(Option.builder()
                .longOpt("redact-file-paths")
                .hasArg()
                .desc("Override audit.redactFilePaths (true/false)")
                .build());

        options.addOption(Option.builder()
                .longOpt("redact-payload")
                .hasArg()
                .desc("Override validation.redactPayload (true/false)")
                .build());

        options.addOption(Option.builder()
                .longOpt("xml-namespace-inbound")
                .hasArg()
                .desc("Override xml.namespace.inbound")
                .build());

        options.addOption(Option.builder()
                .longOpt("xml-namespace-outbound")
                .hasArg()
                .desc("Override xml.namespace.outbound")
                .build());

        options.addOption(Option.builder()
                .longOpt("xml-project-groupId")
                .hasArg()
                .desc("Override xml.project.groupId")
                .build());

        options.addOption(Option.builder()
                .longOpt("xml-project-artifactId")
                .hasArg()
                .desc("Override xml.project.artifactId")
                .build());

        options.addOption(Option.builder()
                .longOpt("java-package")
                .hasArg()
                .desc("Override java.packageName")
                .build());

        return options;
    }

    /**
     * Generates a help message string.
     *
     * @return the formatted help message
     */
    public static String getHelpMessage() {
        Options options = buildOptions();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, 80,
                PROGRAM_NAME + " <command> [options]",
                "\nCommands: generate, validate, parse, version, help\n\nOptions:",
                options,
                2, 4,
                "\nExamples:\n" +
                        "  " + PROGRAM_NAME + " generate -i spec.xlsx -o output/\n" +
                        "  " + PROGRAM_NAME + " validate -i spec.xlsx\n" +
                        "  " + PROGRAM_NAME + " parse -i spec.xlsx -m mq-message.xlsx\n" +
                        "  " + PROGRAM_NAME + " version\n" +
                        "  " + PROGRAM_NAME + " help\n",
                true);
        pw.flush();
        return sw.toString();
    }
}
