package com.rtm.mq.tool.cli.command;

import com.rtm.mq.tool.cli.CliContext;
import com.rtm.mq.tool.cli.Command;
import com.rtm.mq.tool.exception.ExitCodes;

/**
 * Command handler for the 'validate' command.
 *
 * <p>Delegates to validators without implementing business logic.
 * Supports validating Excel specs, generated artifacts, and cross-artifact consistency.</p>
 */
public final class ValidateCommand implements Command {

    private static final String NAME = "validate";
    private static final String DESCRIPTION = "Validate Excel spec and/or generated artifacts";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int execute(CliContext context) {
        // Delegate to validation orchestrator - no business logic here

        if (context.getInputPaths().isEmpty()) {
            System.err.println("Error: No input files specified. Use -i or --input option.");
            return ExitCodes.INPUT_VALIDATION_ERROR;
        }

        // Audit logging
        if (context.getAuditLogger() != null) {
            context.getAuditLogger().logProcess("VALIDATE", "STARTED", null);
        }

        try {
            // Delegate to orchestrator (to be implemented in T-310)
            // ValidationOrchestrator orchestrator = ServiceLocator.getValidationOrchestrator();
            // return orchestrator.execute(context);

            // Placeholder return - actual delegation will be wired later
            System.out.println("Validate command: delegation point for validation pipeline");
            System.out.println("Input paths: " + context.getInputPaths());

            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("VALIDATE", "COMPLETED", null);
            }

            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("VALIDATE", "FAILED", e.getMessage());
            }
            System.err.println("Error during validation: " + e.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
