package com.rtm.mq.tool.api.service;

import com.rtm.mq.tool.api.dto.GenerationResponse;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.generator.Generator;
import com.rtm.mq.tool.generator.java.JavaGenerator;
import com.rtm.mq.tool.generator.openapi.OpenApiGenerator;
import com.rtm.mq.tool.generator.xml.XmlGenerator;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.output.AtomicOutputManager;
import com.rtm.mq.tool.output.OutputManifest;
import com.rtm.mq.tool.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer orchestrator for spec generation workflow.
 *
 * <p>This class encapsulates the end-to-end generation pipeline:</p>
 * <ol>
 *   <li>Excel parsing via {@link Parser}</li>
 *   <li>Code generation via {@link Generator} implementations</li>
 *   <li>Atomic output management via {@link AtomicOutputManager}</li>
 * </ol>
 *
 * <p>This service is designed to be used by REST controllers without exposing
 * low-level CLI implementation details.</p>
 */
@Service
public class GenerationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(GenerationOrchestrator.class);

    private final Parser parser;
    private final XmlGenerator xmlGenerator;
    private final JavaGenerator javaGenerator;
    private final OpenApiGenerator openApiGenerator;
    private final AtomicOutputManager outputManager;

    /**
     * Creates a new orchestrator with injected dependencies.
     *
     * @param parser Excel parser
     * @param xmlGenerator XML bean generator
     * @param javaGenerator Java bean generator
     * @param openApiGenerator OpenAPI generator
     * @param outputManager atomic output manager
     */
    public GenerationOrchestrator(
            Parser parser,
            XmlGenerator xmlGenerator,
            JavaGenerator javaGenerator,
            OpenApiGenerator openApiGenerator,
            AtomicOutputManager outputManager) {
        this.parser = parser;
        this.xmlGenerator = xmlGenerator;
        this.javaGenerator = javaGenerator;
        this.openApiGenerator = openApiGenerator;
        this.outputManager = outputManager;
    }

    /**
     * Executes the full generation pipeline.
     *
     * @param specFile path to MQ spec Excel file
     * @param sharedHeaderFile path to shared header Excel file (optional, can be null)
     * @return generation response with transaction ID and file manifest
     * @throws ParseException if Excel parsing fails
     * @throws GenerationException if code generation fails
     */
    public GenerationResponse generate(
            Path specFile,
            Path sharedHeaderFile) throws ParseException, GenerationException {

        String transactionId = UUID.randomUUID().toString();
        logger.info("Starting generation transaction: {}", transactionId);

        try {
            // 1. Parse Excel spec
            logger.info("Parsing spec file: {}", specFile);
            MessageModel model = parser.parse(specFile, sharedHeaderFile);
            logger.info("Parsing completed successfully");

            // 2. Create temporary output directory
            Path outputDir = Files.createTempDirectory("mq-spec-tool-" + transactionId);
            logger.info("Using temporary output directory: {}", outputDir);

            // 3. Initialize atomic output manager
            outputManager.initialize(transactionId, outputDir);

            // 4. Generate artifacts
            Map<String, String> allGeneratedFiles = new HashMap<>();

            logger.info("Generating XML beans...");
            Map<String, String> xmlFiles = xmlGenerator.generate(model, outputDir);
            allGeneratedFiles.putAll(xmlFiles);

            logger.info("Generating Java beans...");
            Map<String, String> javaFiles = javaGenerator.generate(model, outputDir);
            allGeneratedFiles.putAll(javaFiles);

            logger.info("Generating OpenAPI YAML...");
            Map<String, String> openapiFiles = openApiGenerator.generate(model, outputDir);
            allGeneratedFiles.putAll(openapiFiles);

            // 5. Commit transaction
            logger.info("Committing transaction with {} files", allGeneratedFiles.size());
            OutputManifest manifest = outputManager.commit();

            // 6. Build response
            GenerationResponse response = new GenerationResponse(
                    transactionId,
                    outputManager.getState().name(),
                    new ArrayList<>(allGeneratedFiles.keySet())
            );

            response.setMessage("Generation completed successfully. " +
                    allGeneratedFiles.size() + " files generated.");

            logger.info("Transaction {} completed successfully", transactionId);

            return response;

        } catch (IOException e) {
            logger.error("I/O error during generation transaction {}", transactionId, e);
            outputManager.rollback();
            throw new GenerationException("I/O error during generation: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during generation transaction {}", transactionId, e);
            outputManager.rollback();
            throw new GenerationException("Generation failed: " + e.getMessage(), e);
        }
    }


    public Path getOutputDirectory(String transactionId) {
        return outputManager.getOutputDirectory(transactionId);
    }
}
