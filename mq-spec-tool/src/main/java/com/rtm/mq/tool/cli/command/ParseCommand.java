package com.rtm.mq.tool.cli.command;

import com.rtm.mq.tool.cli.CliContext;
import com.rtm.mq.tool.cli.Command;
import com.rtm.mq.tool.exception.ExitCodes;

/**
 * Command handler for the 'parse' command.
 *
 * <p>Delegates to the parser without implementing business logic.
 * Parses Excel spec and outputs intermediate JSON representation.</p>
 */
public final class ParseCommand implements Command {

    private static final String NAME = "parse";
    private static final String DESCRIPTION = "Parse Excel spec and output intermediate JSON tree";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int execute(CliContext context) {
        // Delegate to parse orchestrator - no business logic here

        if (context.getInputPaths().isEmpty()) {
            System.err.println("Error: No input files specified. Use -i or --input option.");
            return ExitCodes.INPUT_VALIDATION_ERROR;
        }

        // Audit logging
        if (context.getAuditLogger() != null) {
            context.getAuditLogger().logProcess("PARSE", "STARTED", null);
        }

        try {
            // Delegate to orchestrator (to be implemented in T-310)
            // ParseOrchestrator orchestrator = ServiceLocator.getParseOrchestrator();
            // return orchestrator.execute(context);

            // Placeholder return - actual delegation will be wired later
            System.out.println("Parse command: delegation point for parse pipeline");
            System.out.println("Input paths: " + context.getInputPaths());
            if (context.getOutputPath() != null) {
                System.out.println("Output path: " + context.getOutputPath());
            }

            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("PARSE", "COMPLETED", null);
            }

            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("PARSE", "FAILED", e.getMessage());
            }
            System.err.println("Error during parsing: " + e.getMessage());
            return ExitCodes.PARSE_ERROR;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
