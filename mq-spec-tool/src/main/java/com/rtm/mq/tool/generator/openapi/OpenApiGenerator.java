package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.generator.Generator;

/**
 * OpenAPI YAML generator interface.
 *
 * <p>This interface extends the base {@link Generator} interface with methods
 * specific to OpenAPI 3.x specification generation. It produces YAML files
 * compatible with the OpenAPI Specification (OAS 3.x).</p>
 *
 * <p>The generator produces:</p>
 * <ul>
 *   <li>A main API file defining endpoints and operations</li>
 *   <li>Individual schema files for request/response models</li>
 * </ul>
 *
 * @see Generator
 */
public interface OpenApiGenerator extends Generator {

    /**
     * Generates the main API definition file.
     *
     * <p>This method produces the root OpenAPI document (typically api.yaml)
     * containing:</p>
     * <ul>
     *   <li>OpenAPI version declaration</li>
     *   <li>Info section with API metadata</li>
     *   <li>Paths section with endpoint definitions</li>
     *   <li>Component references to schemas</li>
     * </ul>
     *
     * @return the generated api.yaml content as a string;
     *         never null
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     */
    String generateMainApi();

    /**
     * Generates a schema definition file for the specified schema name.
     *
     * <p>This method produces an OpenAPI schema component definition that can
     * be referenced from the main API file or other schemas. The generated
     * schema follows JSON Schema draft-07 as embedded in OpenAPI 3.x.</p>
     *
     * @param schemaName the name of the schema to generate (e.g., "Request", "Response")
     * @return the generated schema YAML content as a string;
     *         never null
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     * @throws IllegalArgumentException if schemaName is null or empty
     */
    String generateSchema(String schemaName);
}
