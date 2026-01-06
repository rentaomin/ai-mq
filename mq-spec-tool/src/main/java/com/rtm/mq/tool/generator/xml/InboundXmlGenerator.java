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
 * Inbound XML Bean Generator for MQ response messages.
 *
 * <p>This generator produces {@code inbound-converter.xml} files containing
 * Spring XML bean definitions for response message serialization/deserialization.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Generates fix-length-inbound-converter XML structure</li>
 *   <li>Preserves field order from Excel/JSON Tree specification</li>
 *   <li>Correctly handles transitory fields (groupId, occurrenceCount)</li>
 *   <li>Generates CompositeField for nested objects</li>
 *   <li>Generates RepeatingField for arrays</li>
 *   <li>Handles empty Response by generating minimal XML (does not throw exception)</li>
 * </ul>
 *
 * <p>Differences from OutboundXmlGenerator:</p>
 * <ul>
 *   <li>Root element: fix-length-inbound-converter (vs outbound)</li>
 *   <li>Converter id: resp_converter (vs req_converter)</li>
 *   <li>forType suffix: Response (vs Request)</li>
 *   <li>Data source: MessageModel.response (vs request)</li>
 *   <li>Empty data: generates empty XML (vs throws exception)</li>
 * </ul>
 *
 * @see XmlTemplateEngine
 * @see XmlGenerator
 */
public class InboundXmlGenerator implements XmlGenerator {

    /** The output filename for inbound converter XML. */
    public static final String OUTPUT_FILENAME = "inbound-converter.xml";

    /** The relative path within output directory. */
    private static final String OUTPUT_SUBDIR = "xml";

    private final Config config;
    private final XmlTemplateEngine templateEngine;
    private MessageModel model;
    private String generatedContent;

    /**
     * Constructs an InboundXmlGenerator with the given configuration.
     *
     * @param config the configuration containing XML generation settings
     */
    public InboundXmlGenerator(Config config) {
        this.config = config;
        this.templateEngine = new XmlTemplateEngine(config);
    }

    /**
     * Constructs an InboundXmlGenerator with the given configuration and template engine.
     *
     * <p>This constructor allows dependency injection of the template engine for testing.</p>
     *
     * @param config the configuration containing XML generation settings
     * @param templateEngine the template engine to use for XML generation
     */
    public InboundXmlGenerator(Config config, XmlTemplateEngine templateEngine) {
        this.config = config;
        this.templateEngine = templateEngine;
    }

    /**
     * Sets the message model to generate XML from.
     *
     * @param model the message model containing response fields
     */
    public void setModel(MessageModel model) {
        this.model = model;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates artifacts including the inbound-converter.xml file.</p>
     */
    @Override
    public Map<String, String> generate(MessageModel model, Path outputDir) {
        this.model = model;
        Map<String, String> artifacts = new LinkedHashMap<>();

        String xmlContent = generateInbound();
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
        return "xml-inbound";
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is not implemented for InboundXmlGenerator.
     * Use {@link com.rtm.mq.tool.generator.xml.OutboundXmlGenerator} for outbound generation.</p>
     *
     * @throws UnsupportedOperationException always, as this generator only supports inbound
     */
    @Override
    public String generateOutbound() {
        throw new UnsupportedOperationException(
            "InboundXmlGenerator does not support outbound generation. Use OutboundXmlGenerator instead.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the inbound XML for response message serialization.</p>
     *
     * <p>Unlike OutboundXmlGenerator, empty response fields do not throw an exception.
     * Instead, an empty inbound-converter.xml with only root elements is generated.</p>
     *
     * @throws GenerationException if operationId is missing and response is non-empty
     */
    @Override
    public String generateInbound() {
        if (model == null) {
            throw new GenerationException("MessageModel is null")
                .withGenerator("InboundXmlGenerator");
        }

        FieldGroup response = model.getResponse();

        // Response can be empty (some messages only have Request)
        // Generate empty inbound-converter.xml in this case (AC13)
        if (response == null || response.getFields().isEmpty()) {
            this.generatedContent = templateEngine.generateEmptyInbound();
            return generatedContent;
        }

        // Validate metadata and operationId only when response is non-empty (AC14)
        if (model.getMetadata() == null) {
            throw new GenerationException("Metadata is required for XML generation")
                .withGenerator("InboundXmlGenerator");
        }

        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new GenerationException("Operation ID is required for XML generation")
                .withGenerator("InboundXmlGenerator")
                .withArtifact(OUTPUT_FILENAME);
        }

        // Generate inbound XML using template engine
        this.generatedContent = templateEngine.generateInbound(response, operationId);
        return generatedContent;
    }

    /**
     * Generates inbound XML from a JSON Tree file.
     *
     * <p>Reads the JSON Tree file, deserializes it to a MessageModel,
     * and generates the inbound XML.</p>
     *
     * @param jsonTreePath the path to the JSON Tree file
     * @return the generated XML content
     * @throws GenerationException if reading or generation fails
     */
    public String generateFromJsonTree(Path jsonTreePath) {
        MessageModel loadedModel = loadMessageModel(jsonTreePath);
        this.model = loadedModel;
        return generateInbound();
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
            throw new GenerationException("No content generated. Call generateInbound() first.")
                .withGenerator("InboundXmlGenerator");
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
     * Checks if the model has response data.
     *
     * @param model the message model to check
     * @return true if response has fields, false otherwise
     */
    public boolean hasResponseData(MessageModel model) {
        FieldGroup response = model.getResponse();
        return response != null && !response.getFields().isEmpty();
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
            throw new GenerationException("Failed to write inbound XML: " + e.getMessage(), e)
                .withGenerator("InboundXmlGenerator")
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
                .withGenerator("InboundXmlGenerator");
        }
    }
}
