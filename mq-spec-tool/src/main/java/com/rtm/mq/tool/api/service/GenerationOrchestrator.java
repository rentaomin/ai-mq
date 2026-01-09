package com.rtm.mq.tool.api.service;

import com.rtm.mq.tool.api.dto.GenerationRequest;
import com.rtm.mq.tool.api.dto.GenerationResponse;
import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ConfigLoader;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer orchestrator for spec generation workflow.
 *
 * <p>This class encapsulates the end-to-end generation pipeline:</p>
 * <ol>
 *   <li>Configuration merge (request + defaults)</li>
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
    private final ConfigLoader configLoader;
    private final AtomicOutputManager outputManager;

    /**
     * Creates a new orchestrator with injected dependencies.
     *
     * @param parser Excel parser
     * @param xmlGenerator XML bean generator
     * @param javaGenerator Java bean generator
     * @param openApiGenerator OpenAPI generator
     * @param configLoader configuration loader
     * @param outputManager atomic output manager
     */
    public GenerationOrchestrator(
            Parser parser,
            XmlGenerator xmlGenerator,
            JavaGenerator javaGenerator,
            OpenApiGenerator openApiGenerator,
            ConfigLoader configLoader,
            AtomicOutputManager outputManager) {
        this.parser = parser;
        this.xmlGenerator = xmlGenerator;
        this.javaGenerator = javaGenerator;
        this.openApiGenerator = openApiGenerator;
        this.configLoader = configLoader;
        this.outputManager = outputManager;
    }

    /**
     * Executes the full generation pipeline.
     *
     * @param specFile path to MQ spec Excel file
     * @param sharedHeaderFile path to shared header Excel file (optional, can be null)
     * @param request generation configuration
     * @return generation response with transaction ID and file manifest
     * @throws ParseException if Excel parsing fails
     * @throws GenerationException if code generation fails
     */
    public GenerationResponse generate(
            Path specFile,
            Path sharedHeaderFile,
            GenerationRequest request) throws ParseException, GenerationException {

        String transactionId = UUID.randomUUID().toString();
        logger.info("Starting generation transaction: {}", transactionId);

        try {
            // 1. Build configuration
            Config config = buildConfig(request);

            // 2. Parse Excel spec
            logger.info("Parsing spec file: {}", specFile);
            MessageModel model = parser.parse(specFile, sharedHeaderFile);
            logger.info("Parsing completed successfully");

            // 3. Create temporary output directory
            Path outputDir = Files.createTempDirectory("mq-spec-tool-" + transactionId);
            logger.info("Using temporary output directory: {}", outputDir);

            // 4. Initialize atomic output manager
            outputManager.initialize(transactionId, outputDir);

            // 5. Generate artifacts
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

            // 6. Commit transaction
            logger.info("Committing transaction with {} files", allGeneratedFiles.size());
            OutputManifest manifest = outputManager.commit();

            // 7. Build response
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

    /**
     * Builds merged configuration from request + defaults.
     *
     * @param request the generation request
     * @return merged configuration
     */
    private Config buildConfig(GenerationRequest request) {
        // Start with default configuration
        Config config = configLoader.loadDefaults();

        // Apply request overrides
        if (request.getXmlNamespaceInbound() != null) {
            config.getXml().getNamespace().setInbound(request.getXmlNamespaceInbound());
        }
        if (request.getXmlNamespaceOutbound() != null) {
            config.getXml().getNamespace().setOutbound(request.getXmlNamespaceOutbound());
        }
        if (request.getXmlProjectGroupId() != null) {
            config.getXml().getProject().setGroupId(request.getXmlProjectGroupId());
        }
        if (request.getXmlProjectArtifactId() != null) {
            config.getXml().getProject().setArtifactId(request.getXmlProjectArtifactId());
        }
        if (request.getJavaPackageName() != null) {
            config.getJava().setPackageName(request.getJavaPackageName());
        }
        if (request.getUseLombok() != null) {
            config.getJava().setUseLombok(request.getUseLombok());
        }
        if (request.getOpenApiVersion() != null) {
            config.getOpenapi().setVersion(request.getOpenApiVersion());
        }
        if (request.getSplitSchemas() != null) {
            config.getOpenapi().setSplitSchemas(request.getSplitSchemas());
        }

        // Apply custom overrides
        if (request.getOverrides() != null && !request.getOverrides().isEmpty()) {
            applyCustomOverrides(config, request.getOverrides());
            logger.debug("Custom overrides applied: {}", request.getOverrides());
        }

        return config;
    }

    private void applyCustomOverrides(Config config, Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith("xml.")) {
                applyXmlOverride(config, key.substring(4), value);
            } else if (key.startsWith("java.")) {
                applyJavaOverride(config, key.substring(5), value);
            } else if (key.startsWith("openapi.")) {
                applyOpenApiOverride(config, key.substring(8), value);
            } else {
                logger.warn("Unknown override key: {}", key);
            }
        }
    }

    private void applyXmlOverride(Config config, String path, Object value) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return;
        }

        if ("namespace".equals(parts[0]) && parts.length == 2) {
            String key = parts[1];
            if ("inbound".equals(key)) {
                config.getXml().getNamespace().setInbound(String.valueOf(value));
            } else if ("outbound".equals(key)) {
                config.getXml().getNamespace().setOutbound(String.valueOf(value));
            } else {
                logger.warn("Unsupported namespace override key: {}", key);
            }
        } else if ("project".equals(parts[0]) && parts.length == 2) {
            String key = parts[1];
            if ("groupId".equals(key)) {
                config.getXml().getProject().setGroupId(String.valueOf(value));
            } else if ("artifactId".equals(key)) {
                config.getXml().getProject().setArtifactId(String.valueOf(value));
            } else {
                logger.warn("Unsupported project override key: {}", key);
            }
        } else {
            logger.warn("Unsupported xml override path: xml.{}", path);
        }
    }

    private void applyJavaOverride(Config config, String path, Object value) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return;
        }

        if ("packageName".equals(parts[0])) {
            config.getJava().setPackageName(String.valueOf(value));
        } else if ("useLombok".equals(parts[0])) {
            config.getJava().setUseLombok(Boolean.parseBoolean(String.valueOf(value)));
        } else {
            logger.warn("Unsupported java override path: java.{}", path);
        }
    }

    private void applyOpenApiOverride(Config config, String path, Object value) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return;
        }

        if ("version".equals(parts[0])) {
            config.getOpenapi().setVersion(String.valueOf(value));
        } else if ("splitSchemas".equals(parts[0])) {
            config.getOpenapi().setSplitSchemas(Boolean.parseBoolean(String.valueOf(value)));
        } else {
            logger.warn("Unsupported openapi override path: openapi.{}", path);
        }
    }

    public Path getOutputDirectory(String transactionId) {
        return outputManager.getOutputDirectory(transactionId);
    }
}
