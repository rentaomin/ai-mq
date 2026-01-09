package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.MessageModel;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Composite XML generator that coordinates both outbound and inbound XML generation.
 *
 * <p>This generator delegates to {@link OutboundXmlGenerator} and {@link InboundXmlGenerator}
 * to generate both request and response XML bean definitions in a single call.</p>
 *
 * <p>The generated artifacts include:</p>
 * <ul>
 *   <li>xml/outbound-converter.xml - Request message bean definition</li>
 *   <li>xml/inbound-converter.xml - Response message bean definition</li>
 * </ul>
 *
 * @see XmlGenerator
 * @see OutboundXmlGenerator
 * @see InboundXmlGenerator
 */
public class CompositeXmlGenerator implements XmlGenerator {

    private static final String GENERATOR_TYPE = "xml";

    private final OutboundXmlGenerator outboundGenerator;
    private final InboundXmlGenerator inboundGenerator;

    /**
     * Constructs a CompositeXmlGenerator with the given configuration.
     *
     * @param config the configuration containing XML generation settings
     */
    public CompositeXmlGenerator(Config config) {
        this.outboundGenerator = new OutboundXmlGenerator(config);
        this.inboundGenerator = new InboundXmlGenerator(config);
    }

    /**
     * Constructs a CompositeXmlGenerator with explicit generator instances.
     *
     * <p>This constructor allows dependency injection of the individual generators for testing.</p>
     *
     * @param outboundGenerator the outbound XML generator
     * @param inboundGenerator the inbound XML generator
     */
    public CompositeXmlGenerator(OutboundXmlGenerator outboundGenerator, InboundXmlGenerator inboundGenerator) {
        this.outboundGenerator = outboundGenerator;
        this.inboundGenerator = inboundGenerator;
    }

    /**
     * Generates both outbound and inbound XML bean definitions.
     *
     * <p>This method delegates to both OutboundXmlGenerator and InboundXmlGenerator
     * to produce a complete set of XML bean definitions.</p>
     *
     * @param model the message model containing request and response definitions
     * @param outputDir the base output directory
     * @return a map containing both outbound and inbound XML files
     * @throws GenerationException if either generator fails
     */
    @Override
    public Map<String, String> generate(MessageModel model, Path outputDir) {
        Map<String, String> artifacts = new LinkedHashMap<>();

        try {
            // Generate outbound (request) XML
            Map<String, String> outboundArtifacts = outboundGenerator.generate(model, outputDir);
            artifacts.putAll(outboundArtifacts);

            // Generate inbound (response) XML
            Map<String, String> inboundArtifacts = inboundGenerator.generate(model, outputDir);
            artifacts.putAll(inboundArtifacts);

            return artifacts;
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GenerationException("Failed to generate XML beans: " + e.getMessage(), e)
                    .withGenerator(GENERATOR_TYPE);
        }
    }

    /**
     * Generates the outbound (request) XML bean definition.
     *
     * @return the generated XML content as a string
     * @throws GenerationException if generation fails
     */
    @Override
    public String generateOutbound() {
        return outboundGenerator.generateOutbound();
    }

    /**
     * Generates the inbound (response) XML bean definition.
     *
     * @return the generated XML content as a string
     * @throws GenerationException if generation fails
     */
    @Override
    public String generateInbound() {
        return inboundGenerator.generateInbound();
    }

    /**
     * Returns the type identifier for this generator.
     *
     * @return "xml"
     */
    @Override
    public String getType() {
        return GENERATOR_TYPE;
    }
}
