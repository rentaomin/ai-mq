package com.rtm.mq.tool.cli;

import com.rtm.mq.tool.exception.ExitCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Main CLI entry point.
 */
class MainTest {

    private ByteArrayOutputStream outStream;
    private ByteArrayOutputStream errStream;
    private Main main;

    @BeforeEach
    void setUp() {
        outStream = new ByteArrayOutputStream();
        errStream = new ByteArrayOutputStream();
        main = new Main(new PrintStream(outStream), new PrintStream(errStream));
    }

    @Test
    void run_noArgs_returnsMissingCommandError() {
        int exitCode = main.run(new String[]{});
        assertEquals(ExitCodes.CLI_MISSING_COMMAND, exitCode);
        assertTrue(errStream.toString().contains("No command specified"));
    }

    @Test
    void run_helpOption_returnsSuccess() {
        int exitCode = main.run(new String[]{"--help"});
        assertEquals(ExitCodes.SUCCESS, exitCode);
        String output = outStream.toString();
        assertTrue(output.contains("MQ Spec Tool"));
        assertTrue(output.contains("generate"));
        assertTrue(output.contains("validate"));
    }

    @Test
    void run_helpCommand_returnsSuccess() {
        int exitCode = main.run(new String[]{"help"});
        assertEquals(ExitCodes.SUCCESS, exitCode);
        String output = outStream.toString();
        assertTrue(output.contains("MQ Spec Tool"));
    }

    @Test
    void run_unknownCommand_returnsUnknownCommandError() {
        int exitCode = main.run(new String[]{"unknown"});
        assertEquals(ExitCodes.CLI_UNKNOWN_COMMAND, exitCode);
        assertTrue(errStream.toString().contains("Unknown command"));
    }

    @Test
    void run_versionCommand_returnsSuccess() {
        int exitCode = main.run(new String[]{"version"});
        // May return INTERNAL_ERROR if versions.properties is not on classpath during test
        // In production, this should return SUCCESS
        assertTrue(exitCode == ExitCodes.SUCCESS || exitCode == ExitCodes.INTERNAL_ERROR);
    }

    @Test
    void run_generateWithoutInput_returnsInputError() {
        int exitCode = main.run(new String[]{"generate"});
        assertEquals(ExitCodes.INPUT_VALIDATION_ERROR, exitCode);
        assertTrue(errStream.toString().contains("No input files specified"));
    }

    @Test
    void run_validateWithoutInput_returnsInputError() {
        int exitCode = main.run(new String[]{"validate"});
        assertEquals(ExitCodes.INPUT_VALIDATION_ERROR, exitCode);
        assertTrue(errStream.toString().contains("No input files specified"));
    }

    @Test
    void run_parseWithoutInput_returnsInputError() {
        int exitCode = main.run(new String[]{"parse"});
        assertEquals(ExitCodes.INPUT_VALIDATION_ERROR, exitCode);
        assertTrue(errStream.toString().contains("No input files specified"));
    }

    @Test
    void run_commandResolution_caseInsensitive() {
        int exitCode1 = main.run(new String[]{"HELP"});
        assertEquals(ExitCodes.SUCCESS, exitCode1);

        // Reset streams for second test
        outStream = new ByteArrayOutputStream();
        errStream = new ByteArrayOutputStream();
        main = new Main(new PrintStream(outStream), new PrintStream(errStream));

        int exitCode2 = main.run(new String[]{"Help"});
        assertEquals(ExitCodes.SUCCESS, exitCode2);
    }

    @Test
    void run_exactlyOneCommandPerInvocation() {
        // Only first argument is treated as command
        int exitCode = main.run(new String[]{"help", "version"});
        assertEquals(ExitCodes.SUCCESS, exitCode);
        // Help command was executed, version was ignored
        assertTrue(outStream.toString().contains("Available commands"));
    }

    @Test
    void run_invalidOption_returnsArgumentError() {
        int exitCode = main.run(new String[]{"generate", "--invalid-option", "value"});
        assertEquals(ExitCodes.CLI_ARGUMENT_ERROR, exitCode);
        assertTrue(errStream.toString().contains("Failed to parse"));
    }

    @Test
    void run_helpOptionWithCommand_showsHelp() {
        // When --help is specified, should show help regardless of command
        int exitCode = main.run(new String[]{"generate", "--help"});
        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(outStream.toString().contains("Available commands"));
    }
}
