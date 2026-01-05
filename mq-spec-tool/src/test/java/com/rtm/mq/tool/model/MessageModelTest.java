package com.rtm.mq.tool.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageModel class.
 */
class MessageModelTest {

    @Test
    void testMessageModelAssembly() {
        MessageModel model = new MessageModel();

        // Set metadata
        Metadata metadata = new Metadata();
        metadata.setSourceFile("test.xlsx");
        metadata.setOperationName("TestOperation");
        model.setMetadata(metadata);

        // Set shared header
        FieldGroup sharedHeader = new FieldGroup();
        sharedHeader.addField(FieldNode.builder().originalName("HEADER_FIELD").build());
        model.setSharedHeader(sharedHeader);

        // Set request
        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder().originalName("REQ_FIELD").build());
        model.setRequest(request);

        // Set response
        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder().originalName("RESP_FIELD").build());
        model.setResponse(response);

        // Verify
        assertNotNull(model.getMetadata());
        assertEquals("test.xlsx", model.getMetadata().getSourceFile());
        assertEquals("TestOperation", model.getMetadata().getOperationName());

        assertNotNull(model.getSharedHeader());
        assertEquals(1, model.getSharedHeader().getFields().size());
        assertEquals("HEADER_FIELD", model.getSharedHeader().getFields().get(0).getOriginalName());

        assertNotNull(model.getRequest());
        assertEquals(1, model.getRequest().getFields().size());
        assertEquals("REQ_FIELD", model.getRequest().getFields().get(0).getOriginalName());

        assertNotNull(model.getResponse());
        assertEquals(1, model.getResponse().getFields().size());
        assertEquals("RESP_FIELD", model.getResponse().getFields().get(0).getOriginalName());
    }

    @Test
    void testMessageModelWithNullFields() {
        MessageModel model = new MessageModel();

        assertNull(model.getMetadata());
        assertNull(model.getSharedHeader());
        assertNull(model.getRequest());
        assertNull(model.getResponse());
    }
}
