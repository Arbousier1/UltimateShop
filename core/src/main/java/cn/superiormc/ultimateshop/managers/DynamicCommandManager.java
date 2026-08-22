package cn.superiormc.ultimateshop.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicCommandManager {

    private static final Set<BukkitCommand> COMMANDS = ConcurrentHashMap.newKeySet();

    private DynamicCommandManager() {
    }

    public static void register(BukkitCommand command) {
        if (command == null) {
            return;
        }

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return;
        }

        commandMap.register(command.getName(), "ultimateshop", command);
        COMMANDS.add(command);
    }

    public static void unregisterAll() {
        if (COMMANDS.isEmpty()) {
            return;
        }

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return;
        }

        Set<BukkitCommand> registeredCommands = Set.copyOf(COMMANDS);
        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands != null) {
            knownCommands.entrySet().removeIf(entry -> registeredCommands.contains(entry.getValue()));
        }

        for (BukkitCommand command : registeredCommands) {
            command.unregister(commandMap);
        }
        COMMANDS.removeAll(registeredCommands);
    }

    private static CommandMap getCommandMap() {
        try {
            Method method = Bukkit.class.getMethod("getCommandMap");
            return (CommandMap) method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Fall back to APIs used by older Bukkit versions.
        }
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException ignored) {
            // Fall back to CraftServer's field.
        }
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            Method method = commandMap.getClass().getMethod("getKnownCommands");
            return (Map<String, Command>) method.invoke(commandMap);
        } catch (ReflectiveOperationException ignored) {
            Class<?> currentClass = commandMap.getClass();
            while (currentClass != null) {
                try {
                    Field field = currentClass.getDeclaredField("knownCommands");
                    field.setAccessible(true);
                    return (Map<String, Command>) field.get(commandMap);
                } catch (NoSuchFieldException exception) {
                    currentClass = currentClass.getSuperclass();
                } catch (ReflectiveOperationException exception) {
                    exception.printStackTrace();
                    return null;
                }
            }
            return null;
        }
    }
}
