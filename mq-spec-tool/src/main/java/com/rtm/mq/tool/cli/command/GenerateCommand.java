package com.rtm.mq.tool.cli.command;

import com.rtm.mq.tool.cli.CliContext;
import com.rtm.mq.tool.cli.Command;
import com.rtm.mq.tool.exception.ExitCodes;

/**
 * Command handler for the 'generate' command.
 *
 * <p>Delegates to the generation pipeline without implementing business logic.
 * Orchestrates: parse, generate XML/Java/OpenAPI, output.</p>
 */
public final class GenerateCommand implements Command {

    private static final String NAME = "generate";
    private static final String DESCRIPTION = "Generate XML beans, Java models, and OpenAPI YAML from Excel spec";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int execute(CliContext context) {
        // Delegate to generation orchestrator - no business logic here
        // This is a routing stub; actual implementation will be injected
        // via a service locator or dependency injection pattern

        if (context.getInputPaths().isEmpty()) {
            System.err.println("Error: No input files specified. Use -i or --input option.");
            return ExitCodes.INPUT_VALIDATION_ERROR;
        }

        if (context.getOutputPath() == null && context.getConfig().getOutput() == null) {
            System.err.println("Error: No output directory specified. Use -o or --output option.");
            return ExitCodes.INPUT_VALIDATION_ERROR;
        }

        // Audit logging
        if (context.getAuditLogger() != null) {
            context.getAuditLogger().logProcess("GENERATE", "STARTED", null);
        }

        try {
            // Delegate to orchestrator (to be implemented in T-310)
            // GenerationOrchestrator orchestrator = ServiceLocator.getGenerationOrchestrator();
            // return orchestrator.execute(context);

            // Placeholder return - actual delegation will be wired later
            System.out.println("Generate command: delegation point for generation pipeline");
            System.out.println("Input paths: " + context.getInputPaths());
            System.out.println("Output path: " + (context.getOutputPath() != null
                    ? context.getOutputPath()
                    : context.getConfig().getOutput().getRootDir()));

            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("GENERATE", "COMPLETED", null);
            }

            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            if (context.getAuditLogger() != null) {
                context.getAuditLogger().logProcess("GENERATE", "FAILED", e.getMessage());
            }
            System.err.println("Error during generation: " + e.getMessage());
            return ExitCodes.GENERATION_ERROR;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
