package com.rtm.mq.tool.api.config;

import com.rtm.mq.tool.config.ConfigLoader;
import com.rtm.mq.tool.generator.java.JavaGenerator;
import com.rtm.mq.tool.generator.openapi.OpenApiGenerator;
import com.rtm.mq.tool.generator.xml.XmlGenerator;
import com.rtm.mq.tool.output.AtomicOutputManager;
import com.rtm.mq.tool.parser.Parser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * Spring Bean configuration for MQ Spec Tool components.
 *
 * <p>This configuration class creates Spring beans for:</p>
 * <ul>
 *   <li>Core parsers and generators</li>
 *   <li>Configuration loaders</li>
 *   <li>Output managers</li>
 *   <li>Multipart file upload support</li>
 * </ul>
 */
@Configuration
public class BeanConfiguration {

    /**
     * Creates Parser bean.
     *
     * <p>NOTE: This requires implementation class. Replace with actual implementation.</p>
     *
     * @return parser instance
     */
    @Bean
    public Parser parser() {
        // TODO: Replace with actual Parser implementation
        // Example: return new ExcelParserImpl();
        throw new UnsupportedOperationException(
                "Parser bean not configured. " +
                "Please provide concrete implementation of Parser interface."
        );
    }

    /**
     * Creates XmlGenerator bean.
     *
     * @return XML generator instance
     */
    @Bean
    public XmlGenerator xmlGenerator() {
        // TODO: Replace with actual XmlGenerator implementation
        // Example: return new XmlGeneratorImpl();
        throw new UnsupportedOperationException(
                "XmlGenerator bean not configured. " +
                "Please provide concrete implementation of XmlGenerator interface."
        );
    }

    /**
     * Creates JavaGenerator bean.
     *
     * @return Java generator instance
     */
    @Bean
    public JavaGenerator javaGenerator() {
        // TODO: Replace with actual JavaGenerator implementation
        // Example: return new JavaBeanGenerator();
        throw new UnsupportedOperationException(
                "JavaGenerator bean not configured. " +
                "Please provide concrete implementation of JavaGenerator interface."
        );
    }

    /**
     * Creates OpenApiGenerator bean.
     *
     * @return OpenAPI generator instance
     */
    @Bean
    public OpenApiGenerator openApiGenerator() {
        // TODO: Replace with actual OpenApiGenerator implementation
        // Example: return new OpenApiGeneratorImpl();
        throw new UnsupportedOperationException(
                "OpenApiGenerator bean not configured. " +
                "Please provide concrete implementation of OpenApiGenerator interface."
        );
    }

    /**
     * Creates ConfigLoader bean.
     *
     * @return config loader instance
     */
    @Bean
    public ConfigLoader configLoader() {
        return new ConfigLoader();
    }

    /**
     * Creates AtomicOutputManager bean.
     *
     * @return atomic output manager instance
     */
    @Bean
    public AtomicOutputManager atomicOutputManager() {
        return new AtomicOutputManager();
    }

    /**
     * Configures multipart file upload resolver.
     *
     * <p>Max file size: 50MB</p>
     * <p>Max request size: 100MB</p>
     *
     * @return multipart resolver
     */
    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(52428800); // 50MB
        resolver.setMaxInMemorySize(1048576); // 1MB
        return resolver;
    }
}
