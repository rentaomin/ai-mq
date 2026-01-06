package com.rtm.mq.tool.generator.xml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.MessageModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outbound XML Bean Generator for MQ request messages.
 *
 * <p>This generator produces {@code outbound-converter.xml} files containing
 * Spring XML bean definitions for message serialization/deserialization.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Generates fix-length-outbound-converter XML structure</li>
 *   <li>Preserves field order from Excel/JSON Tree specification</li>
 *   <li>Correctly handles transitory fields (groupId, occurrenceCount)</li>
 *   <li>Generates CompositeField for nested objects</li>
 *   <li>Generates RepeatingField for arrays</li>
 * </ul>
 *
 * @see XmlTemplateEngine
 * @see XmlGenerator
 */
public class OutboundXmlGenerator implements XmlGenerator {

    /** The output filename for outbound converter XML. */
    public static final String OUTPUT_FILENAME = "outbound-converter.xml";

    /** The relative path within output directory. */
    private static final String OUTPUT_SUBDIR = "xml";

    private final Config config;
    private final XmlTemplateEngine templateEngine;
    private MessageModel model;
    private String generatedContent;

    /**
     * Constructs an OutboundXmlGenerator with the given configuration.
     *
     * @param config the configuration containing XML generation settings
     */
    public OutboundXmlGenerator(Config config) {
        this.config = config;
        this.templateEngine = new XmlTemplateEngine(config);
    }

    /**
     * Constructs an OutboundXmlGenerator with the given configuration and template engine.
     *
     * <p>This constructor allows dependency injection of the template engine for testing.</p>
     *
     * @param config the configuration containing XML generation settings
     * @param templateEngine the template engine to use for XML generation
     */
    public OutboundXmlGenerator(Config config, XmlTemplateEngine templateEngine) {
        this.config = config;
        this.templateEngine = templateEngine;
    }

    /**
     * Sets the message model to generate XML from.
     *
     * @param model the message model containing request fields
     */
    public void setModel(MessageModel model) {
        this.model = model;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates artifacts including the outbound-converter.xml file.</p>
     */
    @Override
    public Map<String, String> generate(MessageModel model, Path outputDir) {
        this.model = model;
        Map<String, String> artifacts = new LinkedHashMap<>();

        String xmlContent = generateOutbound();
        String relativePath = OUTPUT_SUBDIR + "/" + OUTPUT_FILENAME;
        artifacts.put(relativePath, xmlContent);

        // Write to file
        Path outputPath = outputDir.resolve(relativePath);
        writeToFile(outputPath, xmlContent);

        return artifacts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "xml-outbound";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the outbound XML for request message serialization.</p>
     *
     * @throws GenerationException if request fields are empty or operationId is missing
     */
    @Override
    public String generateOutbound() {
        validateModel();

        FieldGroup request = model.getRequest();
        String operationId = model.getMetadata().getOperationId();

        this.generatedContent = templateEngine.generateOutbound(request, operationId);
        return generatedContent;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is not implemented for OutboundXmlGenerator.
     * Use {@link com.rtm.mq.tool.generator.xml.InboundXmlGenerator} for inbound generation.</p>
     *
     * @throws UnsupportedOperationException always, as this generator only supports outbound
     */
    @Override
    public String generateInbound() {
        throw new UnsupportedOperationException(
            "OutboundXmlGenerator does not support inbound generation. Use InboundXmlGenerator instead.");
    }

    /**
     * Generates outbound XML from a JSON Tree file.
     *
     * <p>Reads the JSON Tree file, deserializes it to a MessageModel,
     * and generates the outbound XML.</p>
     *
     * @param jsonTreePath the path to the JSON Tree file
     * @return the generated XML content
     * @throws GenerationException if reading or generation fails
     */
    public String generateFromJsonTree(Path jsonTreePath) {
        MessageModel loadedModel = loadMessageModel(jsonTreePath);
        this.model = loadedModel;
        return generateOutbound();
    }

    /**
     * Writes the generated XML content to the output file.
     *
     * <p>Creates the output directory if it doesn't exist.</p>
     *
     * @throws GenerationException if writing fails
     */
    public void writeOutput() {
        if (generatedContent == null) {
            throw new GenerationException("No content generated. Call generateOutbound() first.")
                .withGenerator("OutboundXmlGenerator");
        }

        Path outputPath = getOutputPath();
        writeToFile(outputPath, generatedContent);
    }

    /**
     * Gets the full output file path.
     *
     * @return the output file path
     */
    public Path getOutputPath() {
        return Path.of(config.getOutput().getRootDir(), OUTPUT_SUBDIR, OUTPUT_FILENAME);
    }

    /**
     * Gets the generated XML content.
     *
     * @return the generated content, or null if not yet generated
     */
    public String getGeneratedContent() {
        return generatedContent;
    }

    /**
     * Validates the model before generation.
     *
     * @throws GenerationException if validation fails
     */
    private void validateModel() {
        if (model == null) {
            throw new GenerationException("MessageModel is null")
                .withGenerator("OutboundXmlGenerator");
        }

        FieldGroup request = model.getRequest();
        if (request == null || request.getFields().isEmpty()) {
            throw new GenerationException("Request fields are empty, cannot generate outbound XML")
                .withGenerator("OutboundXmlGenerator")
                .withArtifact(OUTPUT_FILENAME);
        }

        if (model.getMetadata() == null) {
            throw new GenerationException("Metadata is required for XML generation")
                .withGenerator("OutboundXmlGenerator");
        }

        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new GenerationException("Operation ID is required for XML generation")
                .withGenerator("OutboundXmlGenerator")
                .withArtifact(OUTPUT_FILENAME);
        }
    }

    /**
     * Writes content to a file, creating directories as needed.
     *
     * @param outputPath the output file path
     * @param content the content to write
     * @throws GenerationException if writing fails
     */
    private void writeToFile(Path outputPath, String content) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenerationException("Failed to write outbound XML: " + e.getMessage(), e)
                .withGenerator("OutboundXmlGenerator")
                .withArtifact(OUTPUT_FILENAME);
        }
    }

    /**
     * Loads a MessageModel from a JSON Tree file.
     *
     * @param jsonTreePath the path to the JSON Tree file
     * @return the deserialized MessageModel
     * @throws GenerationException if reading or parsing fails
     */
    private MessageModel loadMessageModel(Path jsonTreePath) {
        try {
            String json = Files.readString(jsonTreePath, StandardCharsets.UTF_8);
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(json, MessageModel.class);
        } catch (IOException e) {
            throw new GenerationException("Failed to read JSON Tree: " + e.getMessage(), e)
                .withGenerator("OutboundXmlGenerator");
        }
    }
}
