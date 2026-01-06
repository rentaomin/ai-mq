package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.OpenApiConfig.SplitStrategy;
import com.rtm.mq.tool.exception.MqToolException;
import com.rtm.mq.tool.exception.ExitCodes;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * OpenAPI Schema Splitter
 *
 * Extracts schemas from main OpenAPI YAML file and splits them into separate files.
 */
public class SchemaSplitter {

    private final Config config;
    private final SplitStrategy strategy;
    private final Yaml yaml;

    public SchemaSplitter(Config config) {
        this.config = config;
        this.strategy = config.getOpenapi().getSplitStrategy();
        this.yaml = createYaml();
    }

    /**
     * Split schemas from main OpenAPI file
     *
     * @param mainFilePath path to main OpenAPI YAML file
     * @return list of generated schema file paths
     * @throws MqToolException if splitting fails
     */
    public List<Path> splitSchemas(Path mainFilePath) throws MqToolException {
        if (strategy == SplitStrategy.NONE) {
            // No splitting, return empty list
            return Collections.emptyList();
        }

        try {
            // Read main file
            String mainContent = Files.readString(mainFilePath, StandardCharsets.UTF_8);
            Map<String, Object> mainDoc = yaml.load(mainContent);

            // Extract schemas
            @SuppressWarnings("unchecked")
            Map<String, Object> components = (Map<String, Object>) mainDoc.get("components");
            if (components == null || !components.containsKey("schemas")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

            // Create schemas directory
            Path schemasDir = mainFilePath.getParent().resolve("schemas");
            Files.createDirectories(schemasDir);

            // Split and write separate files
            List<Path> generatedFiles = new ArrayList<>();
            for (Map.Entry<String, Object> entry : schemas.entrySet()) {
                String schemaName = entry.getKey();
                Object schemaContent = entry.getValue();

                Path schemaFile = schemasDir.resolve(schemaName + ".yaml");
                writeSchemaFile(schemaFile, schemaName, schemaContent);
                generatedFiles.add(schemaFile);
            }

            // Update main file references
            updateMainFileReferences(mainFilePath, mainDoc, schemas.keySet());

            return generatedFiles;

        } catch (IOException e) {
            throw new MqToolException(
                "Failed to split OpenAPI schemas: " + e.getMessage(),
                e,
                ExitCodes.IO_ERROR
            );
        }
    }

    /**
     * Write individual schema file
     */
    private void writeSchemaFile(Path schemaFile, String schemaName, Object schemaContent)
            throws IOException {

        Map<String, Object> schemaDoc = new LinkedHashMap<>();
        schemaDoc.put(schemaName, schemaContent);

        String yamlContent = yaml.dump(schemaDoc);
        Files.writeString(schemaFile, yamlContent, StandardCharsets.UTF_8);
    }

    /**
     * Update $ref references in main file
     */
    private void updateMainFileReferences(Path mainFilePath,
                                         Map<String, Object> mainDoc,
                                         Set<String> schemaNames) throws IOException {

        // Remove components.schemas section
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) mainDoc.get("components");
        components.remove("schemas");

        // Recursively update all $ref references
        updateReferences(mainDoc, schemaNames);

        // Write back to main file
        String updatedContent = yaml.dump(mainDoc);
        Files.writeString(mainFilePath, updatedContent, StandardCharsets.UTF_8);
    }

    /**
     * Recursively update $ref references
     */
    @SuppressWarnings("unchecked")
    private void updateReferences(Object node, Set<String> schemaNames) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;

            // Check for $ref field
            if (map.containsKey("$ref")) {
                String ref = (String) map.get("$ref");
                // Convert from "#/components/schemas/SchemaName" to "./schemas/SchemaName.yaml#/SchemaName"
                for (String schemaName : schemaNames) {
                    String oldRef = "#/components/schemas/" + schemaName;
                    if (ref.equals(oldRef)) {
                        String newRef = "./schemas/" + schemaName + ".yaml#/" + schemaName;
                        map.put("$ref", newRef);
                        break;
                    }
                }
            }

            // Recursively process child nodes
            for (Object value : map.values()) {
                updateReferences(value, schemaNames);
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (Object item : list) {
                updateReferences(item, schemaNames);
            }
        }
    }

    /**
     * Create YAML parser with proper configuration
     */
    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setIndentWithIndicator(false);
        return new Yaml(options);
    }

    /**
     * Get the split strategy
     */
    public SplitStrategy getStrategy() {
        return strategy;
    }
}
