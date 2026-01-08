package com.rtm.mq.tool.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommandRegistry.
 */
class CommandRegistryTest {

    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
    }

    @Test
    void register_addsCommand() {
        Command cmd = createTestCommand("test", "Test description");
        registry.register(cmd);

        assertTrue(registry.hasCommand("test"));
        assertEquals(1, registry.size());
    }

    @Test
    void register_nullCommand_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }

    @Test
    void register_commandWithNullName_throwsException() {
        Command cmd = new Command() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public int execute(CliContext context) {
                return 0;
            }

            @Override
            public String getDescription() {
                return "Test";
            }
        };

        assertThrows(IllegalArgumentException.class, () -> registry.register(cmd));
    }

    @Test
    void register_commandWithEmptyName_throwsException() {
        Command cmd = new Command() {
            @Override
            public String getName() {
                return "";
            }

            @Override
            public int execute(CliContext context) {
                return 0;
            }

            @Override
            public String getDescription() {
                return "Test";
            }
        };

        assertThrows(IllegalArgumentException.class, () -> registry.register(cmd));
    }

    @Test
    void lookup_findsRegisteredCommand() {
        Command cmd = createTestCommand("generate", "Generate artifacts");
        registry.register(cmd);

        Optional<Command> found = registry.lookup("generate");

        assertTrue(found.isPresent());
        assertEquals("generate", found.get().getName());
    }

    @Test
    void lookup_caseInsensitive() {
        Command cmd = createTestCommand("generate", "Generate artifacts");
        registry.register(cmd);

        assertTrue(registry.lookup("GENERATE").isPresent());
        assertTrue(registry.lookup("Generate").isPresent());
        assertTrue(registry.lookup("gEnErAtE").isPresent());
    }

    @Test
    void lookup_notFound_returnsEmpty() {
        Optional<Command> found = registry.lookup("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void lookup_nullName_returnsEmpty() {
        Optional<Command> found = registry.lookup(null);
        assertFalse(found.isPresent());
    }

    @Test
    void lookup_emptyName_returnsEmpty() {
        Optional<Command> found = registry.lookup("");
        assertFalse(found.isPresent());
    }

    @Test
    void hasCommand_returnsTrueForRegistered() {
        registry.register(createTestCommand("test", "Test"));
        assertTrue(registry.hasCommand("test"));
    }

    @Test
    void hasCommand_returnsFalseForUnregistered() {
        assertFalse(registry.hasCommand("nonexistent"));
    }

    @Test
    void hasCommand_caseInsensitive() {
        registry.register(createTestCommand("test", "Test"));
        assertTrue(registry.hasCommand("TEST"));
        assertTrue(registry.hasCommand("Test"));
    }

    @Test
    void getAllCommands_returnsAllRegistered() {
        registry.register(createTestCommand("cmd1", "Command 1"));
        registry.register(createTestCommand("cmd2", "Command 2"));
        registry.register(createTestCommand("cmd3", "Command 3"));

        assertEquals(3, registry.getAllCommands().size());
    }

    @Test
    void getAllCommands_preservesOrder() {
        registry.register(createTestCommand("first", "First"));
        registry.register(createTestCommand("second", "Second"));
        registry.register(createTestCommand("third", "Third"));

        String[] names = registry.getCommandNames().toArray(new String[0]);
        assertEquals("first", names[0]);
        assertEquals("second", names[1]);
        assertEquals("third", names[2]);
    }

    @Test
    void register_replacesSameNameCommand() {
        Command cmd1 = createTestCommand("test", "First");
        Command cmd2 = createTestCommand("test", "Second");

        registry.register(cmd1);
        registry.register(cmd2);

        assertEquals(1, registry.size());
        assertEquals("Second", registry.lookup("test").get().getDescription());
    }

    private Command createTestCommand(String name, String description) {
        return new Command() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int execute(CliContext context) {
                return 0;
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}
