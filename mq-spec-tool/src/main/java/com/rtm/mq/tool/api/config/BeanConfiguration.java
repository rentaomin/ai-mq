package com.rtm.mq.tool.api.config;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ConfigLoader;
import com.rtm.mq.tool.generator.java.JavaBeanGenerator;
import com.rtm.mq.tool.generator.java.JavaGenerator;
import com.rtm.mq.tool.generator.openapi.OpenApiGenerator;
import com.rtm.mq.tool.generator.openapi.OpenApiGeneratorImpl;
import com.rtm.mq.tool.generator.xml.CompositeXmlGenerator;
import com.rtm.mq.tool.generator.xml.XmlGenerator;
import com.rtm.mq.tool.output.AtomicOutputManager;
import com.rtm.mq.tool.parser.ExcelParser;
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
 *
 * <p>All generators and parsers are configured with default configuration and can be
 * overridden through the {@link ConfigLoader} and {@link Config} classes.</p>
 */
@Configuration
public class BeanConfiguration {

    /**
     * Creates default configuration with all default values.
     *
     * @return config instance with default values
     */
    private Config createDefaultConfig() {
        Config config = new Config();
        config.setDefaults();
        return config;
    }

    /**
     * Creates Parser bean for Excel specification parsing.
     *
     * <p>Returns an ExcelParser instance configured with default settings.</p>
     *
     * @return parser instance
     */
    @Bean
    public Parser parser() {
        Config config = createDefaultConfig();
        return new ExcelParser(config);
    }

    /**
     * Creates XmlGenerator bean.
     *
     * <p>Returns a CompositeXmlGenerator that coordinates both OutboundXmlGenerator
     * and InboundXmlGenerator to produce complete XML bean definitions.</p>
     *
     * @return XML generator instance
     */
    @Bean
    public XmlGenerator xmlGenerator() {
        Config config = createDefaultConfig();
        return new CompositeXmlGenerator(config);
    }

    /**
     * Creates JavaGenerator bean.
     *
     * <p>Returns a JavaBeanGenerator instance configured with default settings.</p>
     *
     * @return Java generator instance
     */
    @Bean
    public JavaGenerator javaGenerator() {
        Config config = createDefaultConfig();
        return new JavaBeanGenerator(config);
    }

    /**
     * Creates OpenApiGenerator bean.
     *
     * <p>Returns an OpenApiGeneratorImpl instance configured with default settings.</p>
     *
     * @return OpenAPI generator instance
     */
    @Bean
    public OpenApiGenerator openApiGenerator() {
        Config config = createDefaultConfig();
        return new OpenApiGeneratorImpl(config);
    }

    /**
     * Creates ConfigLoader bean.
     *
     * <p>The ConfigLoader is responsible for loading and merging configuration
     * from YAML files and CLI arguments.</p>
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
     * <p>The AtomicOutputManager ensures that all output files are written atomically,
     * providing rollback capability if any step fails.</p>
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
     * <p>Configuration:</p>
     * <ul>
     *   <li>Max upload size: 50MB</li>
     *   <li>Max in-memory size: 1MB</li>
     * </ul>
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
