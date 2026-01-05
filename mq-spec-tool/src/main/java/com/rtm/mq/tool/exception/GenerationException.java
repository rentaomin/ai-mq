package com.rtm.mq.tool.exception;

/**
 * Exception thrown when code generation fails.
 *
 * <p>This exception provides context about:</p>
 * <ul>
 *   <li>The type of generator that failed (e.g., "XmlBeanGenerator", "JavaBeanGenerator")</li>
 *   <li>The artifact being generated when the failure occurred</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new GenerationException("Template not found")
 *     .withGenerator("XmlBeanGenerator")
 *     .withArtifact("CreateAppRequest.xml");
 * // Output: Template not found [Generator: XmlBeanGenerator] [Artifact: CreateAppRequest.xml]
 * }</pre>
 */
public class GenerationException extends MqToolException {

    private String generatorType;
    private String artifactName;

    /**
     * Creates a new GenerationException with the specified message.
     *
     * @param message the error message
     */
    public GenerationException(String message) {
        super(message, ExitCodes.GENERATION_ERROR);
    }

    /**
     * Creates a new GenerationException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public GenerationException(String message, Throwable cause) {
        super(message, cause, ExitCodes.GENERATION_ERROR);
    }

    /**
     * Adds generator type context to this exception.
     *
     * @param type the type/name of the generator
     * @return this exception for method chaining
     */
    public GenerationException withGenerator(String type) {
        this.generatorType = type;
        return this;
    }

    /**
     * Adds artifact name context to this exception.
     *
     * @param name the name of the artifact being generated
     * @return this exception for method chaining
     */
    public GenerationException withArtifact(String name) {
        this.artifactName = name;
        return this;
    }

    /**
     * Returns the full error message including context information.
     *
     * @return the formatted error message
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (generatorType != null) {
            sb.append(" [Generator: ").append(generatorType).append("]");
        }
        if (artifactName != null) {
            sb.append(" [Artifact: ").append(artifactName).append("]");
        }
        return sb.toString();
    }

    /**
     * Gets the generator type that caused the error.
     *
     * @return the generator type, or null if not set
     */
    public String getGeneratorType() {
        return generatorType;
    }

    /**
     * Gets the artifact name involved in the error.
     *
     * @return the artifact name, or null if not set
     */
    public String getArtifactName() {
        return artifactName;
    }
}
