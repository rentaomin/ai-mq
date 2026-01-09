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
 * Unit tests for InboundXmlGenerator.
 *
 * Tests map to Acceptance Criteria from T-202:
 * AC1-15 as specified in the task spec.
 */
class InboundXmlGeneratorTest {

    private InboundXmlGenerator generator;
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

        generator = new InboundXmlGenerator(config);
    }

    // AC1: Generated XML file conforms to Spring beans XML format
    @Test
    void testGeneratedXmlConformsToSpringBeansFormat() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateInbound();

        // Verify XML declaration (AC12)
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have correct XML declaration");
    }

    // AC8: CompositeField correctly generates nested structure
    @Test
    void testCompositeFieldNested() {
        MessageModel model = createModelWithNestedObject();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("type=\"CompositeField\""), "Should have CompositeField type");
        assertTrue(xml.contains("name=\"responseData\""), "Should have nested object name");
        assertTrue(xml.contains("forType=\"com.rtm.test.ResponseData\""),
            "Should have nested object forType");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"applicationId\""), "Should have nested child field");
    }

    // AC9: RepeatingField correctly generates array structure with fixedCount
    @Test
    void testRepeatingFieldArray() {
        MessageModel model = createModelWithArrayField();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("type=\"RepeatingField\""), "Should have RepeatingField type");
        assertTrue(xml.contains("name=\"errorDetails\""), "Should have array field name");
        assertTrue(xml.contains("fixedCount=\"10\""), "Should have fixedCount attribute");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"errorCode\""), "Should have array child field");
    }


    // Helper methods to create test models

    private MessageModel createValidModel() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();
        FieldNode field = FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("String")
            .length(4)
            .build();
        response.addField(field);
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithNestedObject() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();

        // Nested object field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("applicationId")
            .dataType("String")
            .length(20)
            .build());

        FieldNode nestedObject = FieldNode.builder()
            .isObject(true)
            .camelCaseName("responseData")
            .className("ResponseData")
            .children(childFields)
            .build();
        response.addField(nestedObject);

        model.setResponse(response);
        return model;
    }

    private MessageModel createModelWithArrayField() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();

        // Array field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("errorCode")
            .dataType("String")
            .length(10)
            .build());

        FieldNode arrayField = FieldNode.builder()
            .isArray(true)
            .camelCaseName("errorDetails")
            .className("ErrorDetail")
            .occurrenceCount("0..10")
            .children(childFields)
            .build();
        response.addField(arrayField);

        model.setResponse(response);
        return model;
    }
}
