package com.rtm.mq.tool.cli;

import com.rtm.mq.tool.config.Config;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CliContext.
 */
class CliContextTest {

    @Test
    void builder_createsContextWithAllFields() {
        Config config = new Config();
        List<Path> inputs = Arrays.asList(Paths.get("input1.xlsx"), Paths.get("input2.xlsx"));
        Path output = Paths.get("output/");
        Path configPath = Paths.get("config.yaml");

        CliContext context = CliContext.builder()
                .config(config)
                .commandName("generate")
                .inputPaths(inputs)
                .outputPath(output)
                .configPath(configPath)
                .option("key1", "value1")
                .option("key2", "value2")
                .build();

        assertEquals(config, context.getConfig());
        assertEquals("generate", context.getCommandName());
        assertEquals(2, context.getInputPaths().size());
        assertEquals(output, context.getOutputPath());
        assertEquals(configPath, context.getConfigPath());
        assertEquals("value1", context.getOption("key1"));
        assertEquals("value2", context.getOption("key2"));
    }

    @Test
    void builder_nullInputPaths_returnsEmptyList() {
        CliContext context = CliContext.builder()
                .commandName("test")
                .inputPaths(null)
                .build();

        assertNotNull(context.getInputPaths());
        assertTrue(context.getInputPaths().isEmpty());
    }

    @Test
    void getInputPaths_isUnmodifiable() {
        List<Path> inputs = Arrays.asList(Paths.get("input.xlsx"));
        CliContext context = CliContext.builder()
                .inputPaths(inputs)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> context.getInputPaths().add(Paths.get("another.xlsx")));
    }

    @Test
    void getOptions_isUnmodifiable() {
        CliContext context = CliContext.builder()
                .option("key", "value")
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> context.getOptions().put("newKey", "newValue"));
    }

    @Test
    void hasOption_returnsTrueForExistingOption() {
        CliContext context = CliContext.builder()
                .option("existing", "value")
                .build();

        assertTrue(context.hasOption("existing"));
    }

    @Test
    void hasOption_returnsFalseForMissingOption() {
        CliContext context = CliContext.builder()
                .build();

        assertFalse(context.hasOption("nonexistent"));
    }

    @Test
    void getOption_returnsNullForMissingOption() {
        CliContext context = CliContext.builder()
                .build();

        assertNull(context.getOption("nonexistent"));
    }

    @Test
    void builder_options_mergesWithExisting() {
        Map<String, String> additional = new HashMap<>();
        additional.put("key2", "value2");
        additional.put("key3", "value3");

        CliContext context = CliContext.builder()
                .option("key1", "value1")
                .options(additional)
                .build();

        assertEquals(3, context.getOptions().size());
        assertEquals("value1", context.getOption("key1"));
        assertEquals("value2", context.getOption("key2"));
        assertEquals("value3", context.getOption("key3"));
    }

    @Test
    void builder_preservesOptionsOrder() {
        CliContext context = CliContext.builder()
                .option("first", "1")
                .option("second", "2")
                .option("third", "3")
                .build();

        String[] keys = context.getOptions().keySet().toArray(new String[0]);
        assertEquals("first", keys[0]);
        assertEquals("second", keys[1]);
        assertEquals("third", keys[2]);
    }
}
