package com.rtm.mq.tool.cli.command;

import com.rtm.mq.tool.cli.CliContext;
import com.rtm.mq.tool.cli.Command;
import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.version.VersionRegistry;

import java.util.Properties;

/**
 * Command handler for the 'version' command.
 *
 * <p>Displays version information from the version registry.</p>
 */
public final class VersionCommand implements Command {

    private static final String NAME = "version";
    private static final String DESCRIPTION = "Display tool version information";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int execute(CliContext context) {
        try {
            System.out.println("MQ Spec Tool");
            System.out.println("============");
            System.out.println();
            System.out.println("Tool version:     " + VersionRegistry.getToolVersion());
            System.out.println("Parser version:   " + VersionRegistry.getParserVersion());
            System.out.println("XML template:     " + VersionRegistry.getXmlTemplateVersion());
            System.out.println("Java template:    " + VersionRegistry.getJavaTemplateVersion());
            System.out.println("YAML template:    " + VersionRegistry.getYamlTemplateVersion());
            System.out.println("Rules version:    " + VersionRegistry.getRulesVersion());
            System.out.println();
            System.out.println("All versions: " + VersionRegistry.getVersionSummary());

            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            System.err.println("Error retrieving version information: " + e.getMessage());
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
