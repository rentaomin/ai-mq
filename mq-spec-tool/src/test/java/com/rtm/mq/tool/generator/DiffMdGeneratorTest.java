package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiffMdGenerator.
 *
 * <p>Tests cover acceptance criteria:</p>
 * <ul>
 *   <li>AC1: DiffMdGenerator class compiles</li>
 *   <li>AC2: Generated diff.md is valid Markdown</li>
 *   <li>AC3: Contains complete metadata (timestamp, source file, version)</li>
 *   <li>AC4: Groups by sheet (Shared Header, Request, Response)</li>
 *   <li>AC5: Field order matches JSON Tree</li>
 *   <li>AC6: Recursively processes nested fields</li>
 *   <li>AC7: Shows original and camelCase names correctly</li>
 *   <li>AC8: Shows source info (sheetName, rowIndex) correctly</li>
 *   <li>AC9: Empty field groups don't output empty tables</li>
 *   <li>AC10: Markdown special characters are properly escaped</li>
 *   <li>AC11: Deterministic output (same input -> same output)</li>
 *   <li>AC12: UTF-8 encoding</li>
 *   <li>AC13: Field total count is correct</li>
 * </ul>
 */
@DisplayName("DiffMdGenerator Tests")
class DiffMdGeneratorTest {

    private DiffMdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DiffMdGenerator();
    }

    @Nested
    @DisplayName("Metadata Tests (AC3)")
    class MetadataTests {

        @Test
        @DisplayName("Should include all metadata fields")
        void shouldIncludeAllMetadataFields() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("# 字段名称映射表"));
            assertTrue(content.contains("生成时间: 2026-01-04T10:00:00Z"));
            assertTrue(content.contains("源文件: /path/to/spec.xlsx"));
            assertTrue(content.contains("解析器版本: 1.0.0"));
        }
    }

    @Nested
    @DisplayName("Sheet Grouping Tests (AC4)")
    class SheetGroupingTests {

        @Test
        @DisplayName("Should create Shared Header section")
        void shouldCreateSharedHeaderSection() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("## Shared Header"));
        }

        @Test
        @DisplayName("Should create Request section")
        void shouldCreateRequestSection() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("## Request"));
        }

        @Test
        @DisplayName("Should create Response section")
        void shouldCreateResponseSection() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("## Response"));
        }

        @Test
        @DisplayName("Should skip empty field groups (AC9)")
        void shouldSkipEmptyFieldGroups() {
            MessageModel model = createBasicModel();
            model.setResponse(new FieldGroup()); // Empty response

            String content = generator.generateContent(model);

            assertTrue(content.contains("## Request"));
            assertFalse(content.contains("## Response"));
        }
    }

    @Nested
    @DisplayName("Field Mapping Tests (AC7, AC8)")
    class FieldMappingTests {

        @Test
        @DisplayName("Should display original and camelCase names")
        void shouldDisplayOriginalAndCamelCaseNames() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("Operation Name"));
            assertTrue(content.contains("operationName"));
        }

        @Test
        @DisplayName("Should display source sheet and row index")
        void shouldDisplaySourceSheetAndRowIndex() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("SharedHeader"));
            assertTrue(content.contains("| 2 |"));
        }

        @Test
        @DisplayName("Should handle null camelCaseName")
        void shouldHandleNullCamelCaseName() {
            MessageModel model = createBasicModel();
            FieldNode field = FieldNode.builder()
                .originalName("TestField")
                .camelCaseName(null)
                .source(new SourceMetadata("Request", 10))
                .build();
            model.getRequest().getFields().add(field);

            String content = generator.generateContent(model);

            assertTrue(content.contains("TestField | - |"));
        }

        @Test
        @DisplayName("Should handle null source metadata")
        void shouldHandleNullSourceMetadata() {
            MessageModel model = createBasicModel();
            FieldNode field = FieldNode.builder()
                .originalName("TestField")
                .camelCaseName("testField")
                .source(null)
                .build();
            model.getRequest().getFields().add(field);

            String content = generator.generateContent(model);

            assertTrue(content.contains("testField | - | 0 |"));
        }
    }

    @Nested
    @DisplayName("Nested Field Tests (AC6)")
    class NestedFieldTests {

        @Test
        @DisplayName("Should recursively process nested fields")
        void shouldRecursivelyProcessNestedFields() {
            MessageModel model = createModelWithNestedFields();

            String content = generator.generateContent(model);

            // Parent field
            assertTrue(content.contains("CreateApp"));
            assertTrue(content.contains("createApp"));
            // Child fields
            assertTrue(content.contains("Account Number"));
            assertTrue(content.contains("accountNumber"));
        }

        @Test
        @DisplayName("Should maintain field order (AC5)")
        void shouldMaintainFieldOrder() {
            MessageModel model = createModelWithNestedFields();

            String content = generator.generateContent(model);

            int createAppPos = content.indexOf("createApp");
            int accountNumberPos = content.indexOf("accountNumber");
            int cardTypePos = content.indexOf("cardType");

            assertTrue(createAppPos < accountNumberPos);
            assertTrue(accountNumberPos < cardTypePos);
        }
    }

    @Nested
    @DisplayName("Markdown Escaping Tests (AC10)")
    class MarkdownEscapingTests {

        @Test
        @DisplayName("Should escape pipe character")
        void shouldEscapePipeCharacter() {
            MessageModel model = createBasicModel();
            FieldNode field = FieldNode.builder()
                .originalName("Field|With|Pipes")
                .camelCaseName("fieldWithPipes")
                .source(new SourceMetadata("Request", 10))
                .build();
            model.getRequest().getFields().add(field);

            String content = generator.generateContent(model);

            assertTrue(content.contains("Field\\|With\\|Pipes"));
        }

        @Test
        @DisplayName("Should escape backslash character")
        void shouldEscapeBackslashCharacter() {
            MessageModel model = createBasicModel();
            FieldNode field = FieldNode.builder()
                .originalName("Field\\With\\Backslash")
                .camelCaseName("fieldWithBackslash")
                .source(new SourceMetadata("Request", 10))
                .build();
            model.getRequest().getFields().add(field);

            String content = generator.generateContent(model);

            assertTrue(content.contains("Field\\\\With\\\\Backslash"));
        }

        @Test
        @DisplayName("Should handle null original name")
        void shouldHandleNullOriginalName() {
            MessageModel model = createBasicModel();
            FieldNode field = FieldNode.builder()
                .originalName(null)
                .camelCaseName("testField")
                .source(new SourceMetadata("Request", 10))
                .build();
            model.getRequest().getFields().add(field);

            String content = generator.generateContent(model);

            assertTrue(content.contains("| - | testField |"));
        }
    }

    @Nested
    @DisplayName("Field Count Tests (AC13)")
    class FieldCountTests {

        @Test
        @DisplayName("Should count total fields correctly")
        void shouldCountTotalFieldsCorrectly() {
            MessageModel model = createBasicModel();

            int count = generator.countTotalFields(model);

            assertEquals(4, count); // 1 SharedHeader + 2 Request + 1 Response
        }

        @Test
        @DisplayName("Should count nested fields")
        void shouldCountNestedFields() {
            MessageModel model = createModelWithNestedFields();

            int count = generator.countTotalFields(model);

            assertEquals(5, count); // 1 SharedHeader + 3 Request (including nested) + 1 Response
        }

        @Test
        @DisplayName("Should display correct count in summary")
        void shouldDisplayCorrectCountInSummary() {
            MessageModel model = createBasicModel();

            String content = generator.generateContent(model);

            assertTrue(content.contains("共计 4 个字段映射"));
        }
    }

    @Nested
    @DisplayName("File Generation Tests (AC1, AC2, AC12)")
    class FileGenerationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should generate file successfully")
        void shouldGenerateFileSuccessfully() throws IOException {
            MessageModel model = createBasicModel();
            Path outputPath = tempDir.resolve("diff.md");

            generator.generate(model, outputPath);

            assertTrue(Files.exists(outputPath));
        }

        @Test
        @DisplayName("Should create parent directories if needed")
        void shouldCreateParentDirectoriesIfNeeded() throws IOException {
            MessageModel model = createBasicModel();
            Path outputPath = tempDir.resolve("subdir").resolve("diff.md");

            generator.generate(model, outputPath);

            assertTrue(Files.exists(outputPath));
        }

        @Test
        @DisplayName("Should use UTF-8 encoding (AC12)")
        void shouldUseUtf8Encoding() throws IOException {
            MessageModel model = createBasicModel();
            Path outputPath = tempDir.resolve("diff.md");

            generator.generate(model, outputPath);

            String content = Files.readString(outputPath);
            assertTrue(content.contains("字段名称映射表"));
        }

        @Test
        @DisplayName("Should generate valid Markdown (AC2)")
        void shouldGenerateValidMarkdown() throws IOException {
            MessageModel model = createBasicModel();
            Path outputPath = tempDir.resolve("diff.md");

            generator.generate(model, outputPath);

            String content = Files.readString(outputPath);
            assertTrue(content.contains("# 字段名称映射表"));
            assertTrue(content.contains("## Shared Header"));
            assertTrue(content.contains("| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |"));
            assertTrue(content.contains("|---------|---------------|-----------|------|"));
        }
    }

    @Nested
    @DisplayName("Determinism Tests (AC11)")
    class DeterminismTests {

        @Test
        @DisplayName("Should produce identical output for same input")
        void shouldProduceIdenticalOutputForSameInput() {
            MessageModel model = createBasicModel();

            String output1 = generator.generateContent(model);
            String output2 = generator.generateContent(model);

            assertEquals(output1, output2);
        }
    }

    // Helper methods

    private MessageModel createBasicModel() {
        MessageModel model = new MessageModel();

        // Metadata
        Metadata metadata = new Metadata();
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setSourceFile("/path/to/spec.xlsx");
        metadata.setParserVersion("1.0.0");
        model.setMetadata(metadata);

        // Shared Header
        FieldGroup sharedHeader = new FieldGroup();
        FieldNode operationName = FieldNode.builder()
            .originalName("Operation Name")
            .camelCaseName("operationName")
            .source(new SourceMetadata("SharedHeader", 2))
            .build();
        sharedHeader.addField(operationName);
        model.setSharedHeader(sharedHeader);

        // Request
        FieldGroup request = new FieldGroup();
        FieldNode field1 = FieldNode.builder()
            .originalName("Field 1")
            .camelCaseName("field1")
            .source(new SourceMetadata("Request", 5))
            .build();
        FieldNode field2 = FieldNode.builder()
            .originalName("Field 2")
            .camelCaseName("field2")
            .source(new SourceMetadata("Request", 6))
            .build();
        request.addField(field1);
        request.addField(field2);
        model.setRequest(request);

        // Response
        FieldGroup response = new FieldGroup();
        FieldNode responseCode = FieldNode.builder()
            .originalName("Response Code")
            .camelCaseName("responseCode")
            .source(new SourceMetadata("Response", 9))
            .build();
        response.addField(responseCode);
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithNestedFields() {
        MessageModel model = new MessageModel();

        // Metadata
        Metadata metadata = new Metadata();
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setSourceFile("/path/to/spec.xlsx");
        metadata.setParserVersion("1.0.0");
        model.setMetadata(metadata);

        // Shared Header
        FieldGroup sharedHeader = new FieldGroup();
        FieldNode operationName = FieldNode.builder()
            .originalName("Operation Name")
            .camelCaseName("operationName")
            .source(new SourceMetadata("SharedHeader", 2))
            .build();
        sharedHeader.addField(operationName);
        model.setSharedHeader(sharedHeader);

        // Request with nested fields
        FieldGroup request = new FieldGroup();

        // Parent field with children
        List<FieldNode> children = new ArrayList<>();
        FieldNode accountNumber = FieldNode.builder()
            .originalName("Account Number")
            .camelCaseName("accountNumber")
            .source(new SourceMetadata("Request", 10))
            .build();
        FieldNode cardType = FieldNode.builder()
            .originalName("Card Type")
            .camelCaseName("cardType")
            .source(new SourceMetadata("Request", 11))
            .build();
        children.add(accountNumber);
        children.add(cardType);

        FieldNode createApp = FieldNode.builder()
            .originalName("CreateApp")
            .camelCaseName("createApp")
            .source(new SourceMetadata("Request", 9))
            .isObject(true)
            .children(children)
            .build();

        request.addField(createApp);
        model.setRequest(request);

        // Response
        FieldGroup response = new FieldGroup();
        FieldNode responseCode = FieldNode.builder()
            .originalName("Response Code")
            .camelCaseName("responseCode")
            .source(new SourceMetadata("Response", 9))
            .build();
        response.addField(responseCode);
        model.setResponse(response);

        return model;
    }
}
