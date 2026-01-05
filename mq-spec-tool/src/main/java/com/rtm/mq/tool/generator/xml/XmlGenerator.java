package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.generator.Generator;

/**
 * XML Bean generator interface.
 *
 * <p>This interface extends the base {@link Generator} interface with methods
 * specific to XML bean generation, supporting both outbound (request) and
 * inbound (response) message formats.</p>
 *
 * <p>The generated XML follows the project's bean definition schema and is
 * suitable for use with the existing MQ message processing framework.</p>
 *
 * @see Generator
 */
public interface XmlGenerator extends Generator {

    /**
     * Generates the outbound XML bean definition (Request).
     *
     * <p>The outbound XML represents the message structure sent to the MQ system.
     * This typically corresponds to the "Request" sheet in the Excel specification.</p>
     *
     * @return the generated XML content as a string;
     *         never null but may be empty if no request definition exists
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     */
    String generateOutbound();

    /**
     * Generates the inbound XML bean definition (Response).
     *
     * <p>The inbound XML represents the message structure received from the MQ system.
     * This typically corresponds to the "Response" sheet in the Excel specification.</p>
     *
     * @return the generated XML content as a string;
     *         never null but may be empty if no response definition exists
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     */
    String generateInbound();
}
