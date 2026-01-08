package com.rtm.mq.tool.cli.command;

import com.rtm.mq.tool.cli.CliContext;
import com.rtm.mq.tool.cli.CliOptions;
import com.rtm.mq.tool.cli.Command;
import com.rtm.mq.tool.cli.CommandRegistry;
import com.rtm.mq.tool.exception.ExitCodes;

/**
 * Command handler for the 'help' command.
 *
 * <p>Displays help information including available commands and options.</p>
 */
public final class HelpCommand implements Command {

    private static final String NAME = "help";
    private static final String DESCRIPTION = "Display help information";

    private final CommandRegistry registry;

    /**
     * Creates a new HelpCommand.
     *
     * @param registry the command registry to list commands from
     */
    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int execute(CliContext context) {
        System.out.println("MQ Spec Tool - Message Queue Specification Code Generator");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();

        // Print all registered commands with descriptions
        for (Command cmd : registry.getAllCommands()) {
            System.out.printf("  %-12s %s%n", cmd.getName(), cmd.getDescription());
        }

        System.out.println();
        System.out.println(CliOptions.getHelpMessage());

        return ExitCodes.SUCCESS;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
