package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.generator.Generator;
import com.rtm.mq.tool.model.FieldNode;
import java.util.List;

/**
 * Java Bean generator interface.
 *
 * <p>This interface extends the base {@link Generator} interface with methods
 * specific to Java model class generation. It supports generating Java beans
 * with configurable features such as Lombok annotations.</p>
 *
 * <p>Generated Java classes follow standard Java bean conventions and can be
 * customized through implementation-specific configuration.</p>
 *
 * @see Generator
 * @see FieldNode
 */
public interface JavaGenerator extends Generator {

    /**
     * Generates a single Java class from the given field definitions.
     *
     * <p>This method produces the source code for a Java bean class with
     * the specified name and fields. The generated code includes:</p>
     * <ul>
     *   <li>Package declaration</li>
     *   <li>Import statements</li>
     *   <li>Class declaration with fields</li>
     *   <li>Getters and setters (or Lombok annotations if enabled)</li>
     * </ul>
     *
     * @param className the name of the class to generate (must be a valid Java identifier)
     * @param fields the list of fields to include in the class (must not be null)
     * @return the generated Java source code as a string
     * @throws com.rtm.mq.tool.exception.GenerationException if generation fails
     * @throws IllegalArgumentException if className is not a valid Java identifier
     */
    String generateClass(String className, List<FieldNode> fields);

    /**
     * Returns whether Lombok annotations should be used.
     *
     * <p>When Lombok is enabled, the generator produces classes with Lombok
     * annotations (such as {@code @Data}, {@code @Getter}, {@code @Setter})
     * instead of manually generated getters and setters.</p>
     *
     * @return {@code true} if Lombok annotations should be used;
     *         {@code false} for traditional getter/setter methods
     */
    boolean isUseLombok();
}
