package com.rtm.mq.tool.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for CLI commands.
 *
 * <p>Maintains the set of supported commands and provides lookup by name.
 * Commands are registered in a deterministic order.</p>
 */
public final class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    /**
     * Creates a new empty command registry.
     */
    public CommandRegistry() {
    }

    /**
     * Registers a command.
     *
     * <p>If a command with the same name already exists, it is replaced.</p>
     *
     * @param command the command to register
     * @throws IllegalArgumentException if command is null or has null/empty name
     */
    public void register(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        String name = command.getName();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be null or empty");
        }
        commands.put(name.toLowerCase(), command);
    }

    /**
     * Looks up a command by name.
     *
     * @param name the command name (case-insensitive)
     * @return an Optional containing the command if found, empty otherwise
     */
    public Optional<Command> lookup(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    /**
     * Returns all registered commands in registration order.
     *
     * @return unmodifiable collection of commands
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Returns all registered command names in registration order.
     *
     * @return unmodifiable collection of command names
     */
    public Collection<String> getCommandNames() {
        return Collections.unmodifiableCollection(commands.keySet());
    }

    /**
     * Checks if a command is registered.
     *
     * @param name the command name (case-insensitive)
     * @return true if the command is registered
     */
    public boolean hasCommand(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return commands.containsKey(name.toLowerCase());
    }

    /**
     * Returns the number of registered commands.
     *
     * @return the count of registered commands
     */
    public int size() {
        return commands.size();
    }
}
