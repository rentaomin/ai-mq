package com.rtm.mq.tool.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CliOptions.
 */
class CliOptionsTest {

    @Test
    void parseCommand_extractsCommandName() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate"});
        assertEquals("generate", options.getCommand());
    }

    @Test
    void parseCommand_extractsValidateCommand() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"validate"});
        assertEquals("validate", options.getCommand());
    }

    @Test
    void parseCommand_extractsParseCommand() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"parse"});
        assertEquals("parse", options.getCommand());
    }

    @Test
    void parseCommand_extractsVersionCommand() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"version"});
        assertEquals("version", options.getCommand());
    }

    @Test
    void parseCommand_extractsHelpCommand() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"help"});
        assertEquals("help", options.getCommand());
    }

    @Test
    void parseInputOption_shortForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "-i", "spec.xlsx"});
        assertEquals(1, options.getInputPaths().size());
        assertEquals(Paths.get("spec.xlsx"), options.getInputPaths().get(0));
    }

    @Test
    void parseInputOption_longForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "--input", "spec.xlsx"});
        assertEquals(1, options.getInputPaths().size());
        assertEquals(Paths.get("spec.xlsx"), options.getInputPaths().get(0));
    }

    @Test
    void parseMultipleInputs() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "-i", "spec1.xlsx", "-i", "spec2.xlsx"
        });
        assertEquals(2, options.getInputPaths().size());
        assertEquals(Paths.get("spec1.xlsx"), options.getInputPaths().get(0));
        assertEquals(Paths.get("spec2.xlsx"), options.getInputPaths().get(1));
    }

    @Test
    void parseOutputOption_shortForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "-o", "output/"});
        assertEquals(Paths.get("output/"), options.getOutputPath());
    }

    @Test
    void parseOutputOption_longForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "--output", "output/"});
        assertEquals(Paths.get("output/"), options.getOutputPath());
    }

    @Test
    void parseConfigOption_shortForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "-c", "config.yaml"});
        assertEquals(Paths.get("config.yaml"), options.getConfigPath());
    }

    @Test
    void parseConfigOption_longForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"generate", "--config", "config.yaml"});
        assertEquals(Paths.get("config.yaml"), options.getConfigPath());
    }

    @Test
    void parseHelpOption_shortForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"-h"});
        assertTrue(options.isHelpRequested());
    }

    @Test
    void parseHelpOption_longForm() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"--help"});
        assertTrue(options.isHelpRequested());
    }

    @Test
    void parseOverride_outputDir() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "--output-dir", "/custom/output"
        });
        Map<String, String> overrides = options.getOverrides();
        assertEquals("/custom/output", overrides.get("output-dir"));
    }

    @Test
    void parseOverride_maxNestingDepth() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "--max-nesting-depth", "10"
        });
        Map<String, String> overrides = options.getOverrides();
        assertEquals("10", overrides.get("max-nesting-depth"));
    }

    @Test
    void parseOverride_loggingLevel() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "--logging-level", "DEBUG"
        });
        Map<String, String> overrides = options.getOverrides();
        assertEquals("DEBUG", overrides.get("logging-level"));
    }

    @Test
    void parseOverride_useLombok() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "--use-lombok", "true"
        });
        Map<String, String> overrides = options.getOverrides();
        assertEquals("true", overrides.get("use-lombok"));
    }

    @Test
    void parseOverride_javaPackage() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate", "--java-package", "com.example.model"
        });
        Map<String, String> overrides = options.getOverrides();
        assertEquals("com.example.model", overrides.get("java-package"));
    }

    @Test
    void parseSharedHeader() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "parse", "-i", "spec.xlsx", "-s", "shared-header.xlsx"
        });
        assertEquals(2, options.getInputPaths().size());
        assertEquals(Paths.get("spec.xlsx"), options.getInputPaths().get(0));
        assertEquals(Paths.get("shared-header.xlsx"), options.getInputPaths().get(1));
    }

    @Test
    void parseNoCommand_returnsNullCommand() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{"-i", "spec.xlsx"});
        assertNull(options.getCommand());
    }

    @Test
    void parseComplexCommandLine() throws CliParseException {
        CliOptions options = CliOptions.parse(new String[]{
                "generate",
                "-i", "spec.xlsx",
                "-s", "shared.xlsx",
                "-o", "output/",
                "-c", "config.yaml",
                "--logging-level", "DEBUG",
                "--use-lombok", "true"
        });

        assertEquals("generate", options.getCommand());
        assertEquals(2, options.getInputPaths().size());
        assertEquals(Paths.get("output/"), options.getOutputPath());
        assertEquals(Paths.get("config.yaml"), options.getConfigPath());
        assertEquals("DEBUG", options.getOverrides().get("logging-level"));
        assertEquals("true", options.getOverrides().get("use-lombok"));
    }

    @Test
    void getHelpMessage_isNotEmpty() {
        String help = CliOptions.getHelpMessage();
        assertNotNull(help);
        assertFalse(help.isEmpty());
        assertTrue(help.contains("generate"));
        assertTrue(help.contains("validate"));
        assertTrue(help.contains("parse"));
    }
}
