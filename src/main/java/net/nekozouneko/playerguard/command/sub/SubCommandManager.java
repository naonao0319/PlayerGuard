package net.nekozouneko.playerguard.command.sub;

import java.util.*;

public class SubCommandManager {

    private final Map<String, SubCommand> commands = new HashMap<>();
    /** alias -> 正規コマンド名 */
    private final Map<String, String> aliases = new HashMap<>();

    public void register(String name, SubCommand command, String... aliases) {
        commands.put(name, command);
        for (String alias : aliases) {
            if (commands.containsKey(alias) || this.aliases.containsKey(alias)) {
                throw new IllegalArgumentException(
                        "Alias '" + alias + "' conflicts with an existing command or alias");
            }
            this.aliases.put(alias, name);
        }
    }

    public SubCommand getCommand(String name) {
        return commands.get(aliases.getOrDefault(name, name));
    }

    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    public Set<String> getCommandNamesAndAliases() {
        Set<String> set = new HashSet<>(commands.keySet());
        set.addAll(aliases.keySet());
        return Collections.unmodifiableSet(set);
    }
}
