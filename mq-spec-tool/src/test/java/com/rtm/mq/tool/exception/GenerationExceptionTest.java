package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GenerationException}.
 */
class GenerationExceptionTest {

    @Test
    void testBasicConstructor() {
        GenerationException exception = new GenerationException("Template not found");

        assertEquals("Template not found", exception.getMessage());
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
        assertNull(exception.getGeneratorType());
        assertNull(exception.getArtifactName());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new RuntimeException("IO error");
        GenerationException exception = new GenerationException("Generation failed", cause);

        assertEquals("Generation failed", exception.getMessage());
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testWithGenerator() {
        GenerationException exception = new GenerationException("Template not found")
            .withGenerator("XmlBeanGenerator");

        assertEquals("XmlBeanGenerator", exception.getGeneratorType());
        assertEquals("Template not found [Generator: XmlBeanGenerator]", exception.getMessage());
    }

    @Test
    void testWithArtifact() {
        GenerationException exception = new GenerationException("Write failed")
            .withArtifact("CreateAppRequest.xml");

        assertEquals("CreateAppRequest.xml", exception.getArtifactName());
        assertEquals("Write failed [Artifact: CreateAppRequest.xml]", exception.getMessage());
    }

    @Test
    void testWithGeneratorAndArtifact() {
        GenerationException exception = new GenerationException("Template not found")
            .withGenerator("XmlBeanGenerator")
            .withArtifact("CreateAppRequest.xml");

        assertEquals("XmlBeanGenerator", exception.getGeneratorType());
        assertEquals("CreateAppRequest.xml", exception.getArtifactName());
        assertEquals("Template not found [Generator: XmlBeanGenerator] [Artifact: CreateAppRequest.xml]", exception.getMessage());
    }

    @Test
    void testMethodChaining() {
        GenerationException exception = new GenerationException("Error");
        GenerationException returned = exception.withGenerator("Gen");

        assertSame(exception, returned);

        returned = exception.withArtifact("Art");
        assertSame(exception, returned);
    }

    @Test
    void testInheritsMqToolException() {
        GenerationException exception = new GenerationException("Error");
        assertTrue(exception instanceof MqToolException);
    }

    @Test
    void testMessageOrderConsistent() {
        // Test that generator always comes before artifact
        GenerationException exception1 = new GenerationException("Error")
            .withGenerator("Gen")
            .withArtifact("Art");

        GenerationException exception2 = new GenerationException("Error")
            .withArtifact("Art")
            .withGenerator("Gen");

        assertEquals(exception1.getMessage(), exception2.getMessage());
    }
}
