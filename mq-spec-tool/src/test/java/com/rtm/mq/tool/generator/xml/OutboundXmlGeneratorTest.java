package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.OutputConfig;
import com.rtm.mq.tool.config.XmlConfig;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OutboundXmlGenerator.
 */
class OutboundXmlGeneratorTest {

    private OutboundXmlGenerator generator;
    private Config config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();

        // Configure output directory
        OutputConfig outputConfig = new OutputConfig();
        outputConfig.setRootDir(tempDir.toString());
        config.setOutput(outputConfig);

        // Set up XML configuration
        XmlConfig.NamespaceConfig namespaceConfig = new XmlConfig.NamespaceConfig();
        namespaceConfig.setOutbound("http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0");
        namespaceConfig.setInbound("http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0");
        config.getXml().setNamespace(namespaceConfig);

        XmlConfig.ProjectConfig projectConfig = new XmlConfig.ProjectConfig();
        projectConfig.setGroupId("com.rtm");
        projectConfig.setArtifactId("test");
        config.getXml().setProject(projectConfig);

        generator = new OutboundXmlGenerator(config);
    }

    // AC1: Generated XML file conforms to Spring beans XML format
    @Test
    void testGeneratedXmlConformsToSpringBeansFormat() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        // Verify XML declaration
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have correct XML declaration");
    }

    // AC2: Root element is <beans:beans> with correct namespace declarations
    @Test
    void testRootElementHasCorrectNamespaces() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("<beans:beans"), "Should have beans:beans root element");
        assertTrue(xml.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""),
            "Should have xsi namespace");
        assertTrue(xml.contains("xmlns=\"http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0\""),
            "Should have fix-length namespace");
        assertTrue(xml.contains("xmlns:beans=\"http://www.springframework.org/schema/beans\""),
            "Should have beans namespace");
    }

    // AC3: <fix-length-outbound-converter> element with correct attributes
    @Test
    void testFixLengthOutboundConverterElement() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("<fix-length-outbound-converter"), "Should have converter element");
        assertTrue(xml.contains("id=\"req_converter\""), "Should have correct id");
        assertTrue(xml.contains("codeGen=\"true\""), "Should have codeGen attribute");
    }

    // AC4: <message> element forType attribute format
    @Test
    void testMessageElementForTypeFormat() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId("CreateApplication");
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("<message"), "Should have message element");
        assertTrue(xml.contains("forType=\"com.rtm.test.CreateApplicationRequest\""),
            "Should have correct forType format");
    }

    // AC5: Field order matches input JSON Tree
    @Test
    void testFieldOrderPreserved() {
        MessageModel model = createModelWithOrderedFields();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        int firstFieldPos = xml.indexOf("name=\"firstField\"");
        int secondFieldPos = xml.indexOf("name=\"secondField\"");
        int thirdFieldPos = xml.indexOf("name=\"thirdField\"");

        assertTrue(firstFieldPos > 0, "First field should exist");
        assertTrue(secondFieldPos > 0, "Second field should exist");
        assertTrue(thirdFieldPos > 0, "Third field should exist");
        assertTrue(firstFieldPos < secondFieldPos, "First field should come before second");
        assertTrue(secondFieldPos < thirdFieldPos, "Second field should come before third");
    }

    // AC6: Transitory groupId field correctly generated
    @Test
    void testTransitoryGroupIdField() {
        MessageModel model = createModelWithTransitoryGroupId();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        // Transitory groupId field should NOT have name attribute
        assertTrue(xml.contains("transitory=\"true\""), "Should have transitory attribute");
        assertTrue(xml.contains("defaultValue=\"CREATEAPP\""), "Should have groupId as defaultValue");
        // The groupId transitory field should not have a name attribute
        assertTrue(xml.contains("<field type=\"DataField\""), "Should have DataField type");
    }

    // AC7: Transitory occurrenceCount field correctly generated
    @Test
    void testTransitoryOccurrenceCountField() {
        MessageModel model = createModelWithTransitoryOccurrenceCount();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("transitory=\"true\""), "Should have transitory attribute");
        assertTrue(xml.contains("counterFieldConverter"), "Should use counterFieldConverter");
        assertTrue(xml.contains("pad=\"0\""), "Should have pad=0 for right alignment");
        assertTrue(xml.contains("alignRight=\"true\""), "Should have alignRight=true");
    }

    // AC8: CompositeField correctly generates nested structure
    @Test
    void testCompositeFieldNested() {
        MessageModel model = createModelWithNestedObject();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("type=\"CompositeField\""), "Should have CompositeField type");
        assertTrue(xml.contains("name=\"applicantInfo\""), "Should have nested object name");
        assertTrue(xml.contains("forType=\"com.rtm.test.ApplicantInfo\""),
            "Should have nested object forType");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"firstName\""), "Should have nested child field");
    }

    // AC9: RepeatingField correctly generates array structure with fixedCount
    @Test
    void testRepeatingFieldArray() {
        MessageModel model = createModelWithArrayField();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.contains("type=\"RepeatingField\""), "Should have RepeatingField type");
        assertTrue(xml.contains("name=\"addresses\""), "Should have array field name");
        assertTrue(xml.contains("fixedCount=\"5\""), "Should have fixedCount attribute");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"addressLine1\""), "Should have array child field");
    }

    // AC10: File output to correct location
    @Test
    void testFileOutputLocation() throws IOException {
        MessageModel model = createValidModel();
        generator.setModel(model);

        generator.generateOutbound();
        generator.writeOutput();

        Path expectedPath = tempDir.resolve("xml").resolve("outbound-converter.xml");
        assertTrue(Files.exists(expectedPath), "Output file should exist at expected location");
    }

    // AC11: UTF-8 encoding
    @Test
    void testUtf8Encoding() throws IOException {
        MessageModel model = createValidModel();
        generator.setModel(model);

        generator.generateOutbound();
        generator.writeOutput();

        Path outputPath = tempDir.resolve("xml").resolve("outbound-converter.xml");
        String content = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("encoding=\"UTF-8\""), "Should specify UTF-8 encoding");
    }

    // AC12: Correct XML declaration
    @Test
    void testXmlDeclaration() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateOutbound();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have correct XML declaration");
    }

    // AC13: Empty Request throws GenerationException with GENERATION_ERROR exit code
    @Test
    void testEmptyRequestThrowsException() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);
        model.setRequest(new FieldGroup()); // Empty request

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateOutbound());

        assertTrue(ex.getMessage().contains("Request fields are empty"),
            "Should indicate empty request fields");
        assertEquals(3, ex.getExitCode()); // GENERATION_ERROR
    }

    // AC14: Missing operationId throws GenerationException
    @Test
    void testMissingOperationIdThrowsException() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId(null);

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateOutbound());

        assertTrue(ex.getMessage().contains("Operation ID is required"),
            "Should indicate missing operationId");
    }

    @Test
    void testMissingOperationIdBlankThrowsException() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId("   ");

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateOutbound());

        assertTrue(ex.getMessage().contains("Operation ID is required"),
            "Should indicate missing operationId");
    }

    @Test
    void testNullModelThrowsException() {
        generator.setModel(null);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateOutbound());

        assertTrue(ex.getMessage().contains("MessageModel is null"),
            "Should indicate null model");
    }

    @Test
    void testNullMetadataThrowsException() {
        MessageModel model = createValidModel();
        model.setMetadata(null);

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateOutbound());

        assertTrue(ex.getMessage().contains("Metadata is required"),
            "Should indicate missing metadata");
    }

    @Test
    void testGenerateReturnsArtifactMap() {
        MessageModel model = createValidModel();

        Map<String, String> artifacts = generator.generate(model, tempDir);

        assertEquals(1, artifacts.size(), "Should return one artifact");
        assertTrue(artifacts.containsKey("xml/outbound-converter.xml"),
            "Should have correct artifact key");
        assertNotNull(artifacts.get("xml/outbound-converter.xml"),
            "Should have content");
    }

    @Test
    void testGetType() {
        assertEquals("xml-outbound", generator.getType());
    }

    @Test
    void testGenerateInboundThrowsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () -> generator.generateInbound());
    }

    @Test
    void testGetOutputPath() {
        Path expectedPath = tempDir.resolve("xml").resolve("outbound-converter.xml");
        assertEquals(expectedPath.toString(), generator.getOutputPath().toString());
    }

    @Test
    void testGetGeneratedContentBeforeGeneration() {
        assertNull(generator.getGeneratedContent());
    }

    @Test
    void testGetGeneratedContentAfterGeneration() {
        MessageModel model = createValidModel();
        generator.setModel(model);
        generator.generateOutbound();

        assertNotNull(generator.getGeneratedContent());
        assertTrue(generator.getGeneratedContent().contains("<?xml"));
    }

    @Test
    void testWriteOutputBeforeGenerationThrowsException() {
        assertThrows(GenerationException.class, () -> generator.writeOutput());
    }

    @Test
    void testDataFieldWithStringType() {
        MessageModel model = createValidModel();
        FieldNode stringField = FieldNode.builder()
            .camelCaseName("customerId")
            .dataType("String")
            .length(20)
            .build();
        model.getRequest().getFields().clear();
        model.getRequest().addField(stringField);

        generator.setModel(model);
        String xml = generator.generateOutbound();

        assertTrue(xml.contains("name=\"customerId\""), "Should have field name");
        assertTrue(xml.contains("length=\"20\""), "Should have length");
        assertTrue(xml.contains("nullPad=\" \""), "String should have space nullPad");
        assertTrue(xml.contains("stringFieldConverter"), "Should use string converter");
    }

    @Test
    void testDataFieldWithNumericType() {
        MessageModel model = createValidModel();
        FieldNode numericField = FieldNode.builder()
            .camelCaseName("transactionCount")
            .dataType("Number")
            .length(10)
            .build();
        model.getRequest().getFields().clear();
        model.getRequest().addField(numericField);

        generator.setModel(model);
        String xml = generator.generateOutbound();

        assertTrue(xml.contains("name=\"transactionCount\""), "Should have field name");
        assertTrue(xml.contains("length=\"10\""), "Should have length");
        assertTrue(xml.contains("pad=\"0\""), "Numeric should have 0 pad");
        assertTrue(xml.contains("alignRight=\"true\""), "Numeric should be right aligned");
    }

    @Test
    void testDataFieldWithDefaultValue() {
        MessageModel model = createValidModel();
        FieldNode fieldWithDefault = FieldNode.builder()
            .camelCaseName("status")
            .dataType("String")
            .length(5)
            .defaultValue("ACTIV")
            .build();
        model.getRequest().getFields().clear();
        model.getRequest().addField(fieldWithDefault);

        generator.setModel(model);
        String xml = generator.generateOutbound();

        assertTrue(xml.contains("defaultValue=\"ACTIV\""), "Should have defaultValue");
    }

    // Helper methods to create test models

    private MessageModel createValidModel() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        FieldNode field = FieldNode.builder()
            .camelCaseName("customerId")
            .dataType("String")
            .length(20)
            .build();
        request.addField(field);
        model.setRequest(request);

        return model;
    }

    private MessageModel createModelWithOrderedFields() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOperation");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder().camelCaseName("firstField").dataType("String").length(10).build());
        request.addField(FieldNode.builder().camelCaseName("secondField").dataType("String").length(10).build());
        request.addField(FieldNode.builder().camelCaseName("thirdField").dataType("String").length(10).build());
        model.setRequest(request);

        return model;
    }

    private MessageModel createModelWithTransitoryGroupId() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        // Transitory groupId field - no name, has groupId
        FieldNode transitoryField = FieldNode.builder()
            .isTransitory(true)
            .groupId("CREATEAPP")
            .length(10)
            .build();
        request.addField(transitoryField);

        // Regular field
        FieldNode regularField = FieldNode.builder()
            .camelCaseName("customerId")
            .dataType("String")
            .length(20)
            .build();
        request.addField(regularField);
        model.setRequest(request);

        return model;
    }

    private MessageModel createModelWithTransitoryOccurrenceCount() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        // Transitory occurrenceCount field
        FieldNode transitoryField = FieldNode.builder()
            .isTransitory(true)
            .occurrenceCount("0..9")
            .length(4)
            .build();
        request.addField(transitoryField);

        // Regular field
        FieldNode regularField = FieldNode.builder()
            .camelCaseName("customerId")
            .dataType("String")
            .length(20)
            .build();
        request.addField(regularField);
        model.setRequest(request);

        return model;
    }

    private MessageModel createModelWithNestedObject() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();

        // Nested object field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("firstName")
            .dataType("String")
            .length(30)
            .build());

        FieldNode nestedObject = FieldNode.builder()
            .isObject(true)
            .camelCaseName("applicantInfo")
            .className("ApplicantInfo")
            .children(childFields)
            .build();
        request.addField(nestedObject);

        model.setRequest(request);
        return model;
    }

    private MessageModel createModelWithArrayField() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();

        // Array field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("addressLine1")
            .dataType("String")
            .length(50)
            .build());

        FieldNode arrayField = FieldNode.builder()
            .isArray(true)
            .camelCaseName("addresses")
            .className("Address")
            .occurrenceCount("0..5")
            .children(childFields)
            .build();
        request.addField(arrayField);

        model.setRequest(request);
        return model;
    }
}
