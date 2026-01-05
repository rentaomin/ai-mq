package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParseException}.
 */
class ParseExceptionTest {

    @Test
    void testBasicConstructor() {
        ParseException exception = new ParseException("Invalid value");

        assertEquals("Invalid value", exception.getMessage());
        assertEquals(ExitCodes.PARSE_ERROR, exception.getExitCode());
        assertNull(exception.getSheetName());
        assertNull(exception.getRowIndex());
        assertNull(exception.getFieldName());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new NumberFormatException("Not a number");
        ParseException exception = new ParseException("Parse failed", cause);

        assertEquals("Parse failed", exception.getMessage());
        assertEquals(ExitCodes.PARSE_ERROR, exception.getExitCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testWithContext() {
        ParseException exception = new ParseException("Invalid Seg lvl value '0'")
            .withContext("Request", 15);

        assertEquals("Request", exception.getSheetName());
        assertEquals(15, exception.getRowIndex());
        assertEquals("Invalid Seg lvl value '0' in sheet 'Request' at row 15", exception.getMessage());
    }

    @Test
    void testWithField() {
        ParseException exception = new ParseException("Duplicate field name")
            .withField("domicleBranche");

        assertEquals("domicleBranche", exception.getFieldName());
        assertEquals("Duplicate field name (field: 'domicleBranche')", exception.getMessage());
    }

    @Test
    void testWithContextAndField() {
        ParseException exception = new ParseException("Duplicate field name")
            .withContext("Request", 18)
            .withField("domicleBranche");

        assertEquals("Request", exception.getSheetName());
        assertEquals(18, exception.getRowIndex());
        assertEquals("domicleBranche", exception.getFieldName());
        assertEquals("Duplicate field name in sheet 'Request' at row 18 (field: 'domicleBranche')", exception.getMessage());
    }

    @Test
    void testMethodChaining() {
        ParseException exception = new ParseException("Error");
        ParseException returned = exception.withContext("Sheet1", 10);

        assertSame(exception, returned);

        returned = exception.withField("myField");
        assertSame(exception, returned);
    }

    @Test
    void testInheritsMqToolException() {
        ParseException exception = new ParseException("Error");
        assertTrue(exception instanceof MqToolException);
    }

    @Test
    void testObjectDefinitionErrorMessage() {
        // Example from task spec
        ParseException exception = new ParseException(
            "Invalid object definition 'CreateAppCreateApplication'. Expected format: 'fieldName:ClassName'"
        ).withContext("Request", 9);

        String expected = "Invalid object definition 'CreateAppCreateApplication'. Expected format: 'fieldName:ClassName' in sheet 'Request' at row 9";
        assertEquals(expected, exception.getMessage());
    }

    @Test
    void testOnlySheetNameSet() {
        ParseException exception = new ParseException("Error");
        exception.withContext("OnlySheet", 1);

        assertTrue(exception.getMessage().contains("sheet 'OnlySheet'"));
        assertTrue(exception.getMessage().contains("at row 1"));
    }
}
