package com.rtm.mq.tool.api.controller;

import com.rtm.mq.tool.api.dto.GenerationRequest;
import com.rtm.mq.tool.api.dto.GenerationResponse;
import com.rtm.mq.tool.api.service.GenerationOrchestrator;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.exception.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST controller for MQ spec generation API.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /api/v1/generate - Generate code from uploaded Excel spec</li>
 *   <li>GET /api/v1/health - Health check endpoint</li>
 *   <li>GET /api/v1/version - Tool version information</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class SpecGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(SpecGenerationController.class);

    private final GenerationOrchestrator orchestrator;

    public SpecGenerationController(GenerationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Generates XML/Java/OpenAPI artifacts from uploaded MQ spec Excel file.
     *
     * <p>Request parameters:</p>
     * <ul>
     *   <li><b>specFile</b> (required): MQ message spec Excel file</li>
     *   <li><b>sharedHeaderFile</b> (optional): Shared header Excel file</li>
     *   <li><b>config</b> (optional): JSON configuration overrides</li>
     * </ul>
     *
     * <p>Returns ZIP archive containing all generated files.</p>
     *
     * @param specFile the MQ spec Excel file
     * @param sharedHeaderFile optional shared header file
     * @param xmlNamespaceInbound XML namespace for inbound messages
     * @param xmlNamespaceOutbound XML namespace for outbound messages
     * @param xmlProjectGroupId Maven groupId for XML project
     * @param xmlProjectArtifactId Maven artifactId for XML project
     * @param javaPackageName Java package name for beans
     * @param useLombok whether to use Lombok annotations
     * @param openApiVersion OpenAPI spec version
     * @param splitSchemas whether to split OpenAPI schemas
     * @param request HTTP request
     * @return ZIP file download response
     * @throws ParseException if Excel parsing fails
     * @throws GenerationException if generation fails
     * @throws IOException if I/O error occurs
     */
    @PostMapping(value = "/generate", produces = "application/zip")
    public ResponseEntity<Resource> generate(
            @RequestParam("specFile") MultipartFile specFile,
            @RequestParam(value = "sharedHeaderFile", required = false) MultipartFile sharedHeaderFile,
            @RequestParam(value = "xmlNamespaceInbound", required = false) String xmlNamespaceInbound,
            @RequestParam(value = "xmlNamespaceOutbound", required = false) String xmlNamespaceOutbound,
            @RequestParam(value = "xmlProjectGroupId", required = false) String xmlProjectGroupId,
            @RequestParam(value = "xmlProjectArtifactId", required = false) String xmlProjectArtifactId,
            @RequestParam(value = "javaPackageName", required = false) String javaPackageName,
            @RequestParam(value = "useLombok", required = false, defaultValue = "false") Boolean useLombok,
            @RequestParam(value = "openApiVersion", required = false, defaultValue = "3.0.3") String openApiVersion,
            @RequestParam(value = "splitSchemas", required = false, defaultValue = "true") Boolean splitSchemas,
            HttpServletRequest request) throws ParseException, GenerationException, IOException {

        logger.info("Received generation request for spec file: {}", specFile.getOriginalFilename());

        // 1. Validate input
        if (specFile.isEmpty()) {
            throw new IllegalArgumentException("Spec file cannot be empty");
        }

        // 2. Save uploaded files to temp directory
        Path tempSpecFile = Files.createTempFile("spec-", ".xlsx");
        Path tempSharedHeaderFile = null;

        try {
            specFile.transferTo(tempSpecFile.toFile());
            logger.debug("Saved spec file to: {}", tempSpecFile);

            if (sharedHeaderFile != null && !sharedHeaderFile.isEmpty()) {
                tempSharedHeaderFile = Files.createTempFile("shared-header-", ".xlsx");
                sharedHeaderFile.transferTo(tempSharedHeaderFile.toFile());
                logger.debug("Saved shared header file to: {}", tempSharedHeaderFile);
            }

            // 3. Execute generation
            GenerationResponse response = orchestrator.generate(
                    tempSpecFile,
                    tempSharedHeaderFile
            );

            logger.info("Generation completed. Transaction ID: {}", response.getTransactionId());

            // 4. Create ZIP archive from output directory
            Path outputDir = orchestrator.getOutputDirectory(response.getTransactionId());
            Path zipFile = Files.createTempFile("mq-spec-output-", ".zip");

            createZipArchive(outputDir, zipFile);

            // 5. Return ZIP file as download
            Resource resource = new FileSystemResource(zipFile);
            String filename = "mq-spec-output-" + response.getTransactionId() + ".zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } finally {
            // Cleanup temporary input files
            Files.deleteIfExists(tempSpecFile);
            if (tempSharedHeaderFile != null) {
                Files.deleteIfExists(tempSharedHeaderFile);
            }
        }
    }

    /**
     * Creates a ZIP archive from a directory.
     *
     * @param sourceDir the source directory
     * @param zipFile the target ZIP file
     * @throws IOException if I/O error occurs
     */
    private void createZipArchive(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to add file to ZIP: " + path, e);
                        }
                    });
        }
        logger.debug("Created ZIP archive: {}", zipFile);
    }

    /**
     * Health check endpoint.
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Version information endpoint.
     *
     * @return tool version information
     */
    @GetMapping("/version")
    public ResponseEntity<String> version() {
        // TODO: Load from versions.properties
        return ResponseEntity.ok("{\"version\": \"1.0.0-SNAPSHOT\", \"tool\": \"MQ Spec Tool\"}");
    }

}
