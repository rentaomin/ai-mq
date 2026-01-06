package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.OpenApiConfig;
import com.rtm.mq.tool.config.OpenApiConfig.SplitStrategy;
import com.rtm.mq.tool.exception.MqToolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SchemaSplitter.
 */
class SchemaSplitterTest {

    @TempDir
    Path tempDir;

    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
    }

    @Test
    void testSplitSchemas_withNoneStrategy_returnsEmptyList() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.NONE);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createSampleOpenApiFile();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertTrue(result.isEmpty());
        assertFalse(Files.exists(mainFile.getParent().resolve("schemas")));
    }

    @Test
    void testSplitSchemas_withByObjectStrategy_createsSchemaFiles() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createSampleOpenApiFile();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertEquals(3, result.size());

        Path schemasDir = mainFile.getParent().resolve("schemas");
        assertTrue(Files.exists(schemasDir));
        assertTrue(Files.isDirectory(schemasDir));

        // Check individual schema files exist
        Path requestFile = schemasDir.resolve("CreateApplicationRequest.yaml");
        Path responseFile = schemasDir.resolve("CreateApplicationResponse.yaml");
        Path arrayFile = schemasDir.resolve("CBACardArray.yaml");

        assertTrue(Files.exists(requestFile));
        assertTrue(Files.exists(responseFile));
        assertTrue(Files.exists(arrayFile));

        // Verify content of schema files
        String requestContent = Files.readString(requestFile, StandardCharsets.UTF_8);
        assertTrue(requestContent.contains("CreateApplicationRequest"));
        assertTrue(requestContent.contains("customerId"));

        String arrayContent = Files.readString(arrayFile, StandardCharsets.UTF_8);
        assertTrue(arrayContent.contains("CBACardArray"));
        assertTrue(arrayContent.contains("cardNo"));
    }

    @Test
    void testSplitSchemas_updatesMainFileReferences() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createSampleOpenApiFile();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        String updatedContent = Files.readString(mainFile, StandardCharsets.UTF_8);

        // Verify schemas section is removed
        assertFalse(updatedContent.contains("components:\n  schemas:") ||
                    updatedContent.contains("components:\r\n  schemas:"));

        // Verify $ref references are updated
        assertTrue(updatedContent.contains("./schemas/CreateApplicationRequest.yaml#/CreateApplicationRequest"));
        assertTrue(updatedContent.contains("./schemas/CreateApplicationResponse.yaml#/CreateApplicationResponse"));

        // Verify old references are gone
        assertFalse(updatedContent.contains("#/components/schemas/CreateApplicationRequest"));
        assertFalse(updatedContent.contains("#/components/schemas/CreateApplicationResponse"));
    }

    @Test
    void testSplitSchemas_updatesNestedReferences() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createSampleOpenApiFileWithNestedRefs();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        Path schemasDir = mainFile.getParent().resolve("schemas");
        Path requestFile = schemasDir.resolve("CreateApplicationRequest.yaml");

        String requestContent = Files.readString(requestFile, StandardCharsets.UTF_8);

        // Verify nested references in schema files are also updated
        assertTrue(requestContent.contains("./CBACardArray.yaml#/CBACardArray"));
        assertFalse(requestContent.contains("#/components/schemas/CBACardArray"));
    }

    @Test
    void testSplitSchemas_withEmptySchemas_returnsEmptyList() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createOpenApiFileWithoutSchemas();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSplitSchemas_withNoComponentsSection_returnsEmptyList() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createMinimalOpenApiFile();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSplitSchemas_withInvalidPath_throwsException() {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path invalidFile = tempDir.resolve("nonexistent.yaml");

        // When/Then
        assertThrows(MqToolException.class, () -> splitter.splitSchemas(invalidFile));
    }

    @Test
    void testGetStrategy_returnsConfiguredStrategy() {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_MESSAGE);
        config.setOpenapi(openApiConfig);

        // When
        SchemaSplitter splitter = new SchemaSplitter(config);

        // Then
        assertEquals(SplitStrategy.BY_MESSAGE, splitter.getStrategy());
    }

    @Test
    void testSplitSchemas_preservesYamlFormatting() throws Exception {
        // Given
        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setSplitStrategy(SplitStrategy.BY_OBJECT);
        config.setOpenapi(openApiConfig);

        SchemaSplitter splitter = new SchemaSplitter(config);

        Path mainFile = createSampleOpenApiFile();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        Path schemasDir = mainFile.getParent().resolve("schemas");
        Path requestFile = schemasDir.resolve("CreateApplicationRequest.yaml");

        String content = Files.readString(requestFile, StandardCharsets.UTF_8);

        // Verify YAML is in block style (not flow style)
        assertFalse(content.contains("{"));
        assertFalse(content.contains("}"));

        // Verify proper indentation (2 spaces)
        assertTrue(content.contains("  type:") || content.contains("  properties:"));
    }

    // Helper methods to create test files

    private Path createSampleOpenApiFile() throws Exception {
        String content = "openapi: 3.0.3\n" +
                "info:\n" +
                "  title: Test API\n" +
                "  version: 1.0.0\n" +
                "\n" +
                "paths:\n" +
                "  /create-application:\n" +
                "    post:\n" +
                "      operationId: CreateApplication\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              $ref: '#/components/schemas/CreateApplicationRequest'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/CreateApplicationResponse'\n" +
                "\n" +
                "components:\n" +
                "  schemas:\n" +
                "    CreateApplicationRequest:\n" +
                "      type: object\n" +
                "      required:\n" +
                "        - customerId\n" +
                "      properties:\n" +
                "        customerId:\n" +
                "          type: string\n" +
                "          maxLength: 20\n" +
                "          description: CUST_ID\n" +
                "        applicationAmount:\n" +
                "          type: number\n" +
                "          format: decimal\n" +
                "          description: APPLICATION_AMT\n" +
                "    CreateApplicationResponse:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        resultCode:\n" +
                "          type: string\n" +
                "          maxLength: 4\n" +
                "          description: RESULT_CODE\n" +
                "    CBACardArray:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        cardNo:\n" +
                "          type: string\n" +
                "          maxLength: 16\n" +
                "          description: CARD_NO\n";

        Path file = tempDir.resolve("api.yaml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private Path createSampleOpenApiFileWithNestedRefs() throws Exception {
        String content = "openapi: 3.0.3\n" +
                "info:\n" +
                "  title: Test API\n" +
                "  version: 1.0.0\n" +
                "\n" +
                "paths:\n" +
                "  /create-application:\n" +
                "    post:\n" +
                "      operationId: CreateApplication\n" +
                "      requestBody:\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              $ref: '#/components/schemas/CreateApplicationRequest'\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "          content:\n" +
                "            application/json:\n" +
                "              schema:\n" +
                "                $ref: '#/components/schemas/CreateApplicationResponse'\n" +
                "\n" +
                "components:\n" +
                "  schemas:\n" +
                "    CreateApplicationRequest:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        customerId:\n" +
                "          type: string\n" +
                "        cards:\n" +
                "          type: array\n" +
                "          items:\n" +
                "            $ref: '#/components/schemas/CBACardArray'\n" +
                "    CreateApplicationResponse:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        resultCode:\n" +
                "          type: string\n" +
                "    CBACardArray:\n" +
                "      type: object\n" +
                "      properties:\n" +
                "        cardNo:\n" +
                "          type: string\n";

        Path file = tempDir.resolve("api-nested.yaml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private Path createOpenApiFileWithoutSchemas() throws Exception {
        String content = "openapi: 3.0.3\n" +
                "info:\n" +
                "  title: Test API\n" +
                "  version: 1.0.0\n" +
                "\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      operationId: Test\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n" +
                "\n" +
                "components:\n" +
                "  schemas: {}\n";

        Path file = tempDir.resolve("api-empty.yaml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private Path createMinimalOpenApiFile() throws Exception {
        String content = "openapi: 3.0.3\n" +
                "info:\n" +
                "  title: Test API\n" +
                "  version: 1.0.0\n" +
                "\n" +
                "paths:\n" +
                "  /test:\n" +
                "    get:\n" +
                "      operationId: Test\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Success\n";

        Path file = tempDir.resolve("api-minimal.yaml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
