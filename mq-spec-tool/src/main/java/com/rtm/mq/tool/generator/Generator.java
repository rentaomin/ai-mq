package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.MessageModel;
import java.nio.file.Path;
import java.util.Map;

/**
 * Common interface for code generators.
 *
 * <p>This interface defines the contract for all artifact generators in the system,
 * including XML bean generators, Java model generators, and OpenAPI YAML generators.</p>
 *
 * <p>All generators share the common pattern of:</p>
 * <ul>
 *   <li>Receiving a parsed {@link MessageModel} as input</li>
 *   <li>Producing one or more output files</li>
 *   <li>Returning a map of relative file paths to their generated content</li>
 * </ul>
 *
 * @see MessageModel
 */
public interface Generator {

    /**
     * Generates artifacts from the given message model.
     *
     * <p>This method produces all files associated with this generator type.
     * The returned map contains entries where:</p>
     * <ul>
     *   <li>Key: relative file path from the output directory (e.g., "beans/Request.xml")</li>
     *   <li>Value: the generated file content as a string</li>
     * </ul>
     *
     * <p>The implementation should not write files directly; the caller is responsible
     * for persisting the returned content to the specified output directory.</p>
     *
     * @param model the message model to generate from (must not be null)
     * @param outputDir the base output directory for resolving relative paths
     * @return a map of relative file paths to their generated content;
     *         never null but may be empty if no files are generated
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     */
    Map<String, String> generate(MessageModel model, Path outputDir);

    /**
     * Returns the type identifier for this generator.
     *
     * <p>The type identifier is used for logging, configuration, and
     * distinguishing between different generator implementations.</p>
     *
     * <p>Example return values: "xml", "java", "openapi"</p>
     *
     * @return a non-null, non-empty string identifying this generator type
     */
    String getType();
}
