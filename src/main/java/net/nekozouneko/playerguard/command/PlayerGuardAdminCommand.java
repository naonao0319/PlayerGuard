package net.nekozouneko.playerguard.command;

import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.command.sub.SubCommandManager;
import net.nekozouneko.playerguard.command.sub.admin.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerGuardAdminCommand implements CommandExecutor, TabCompleter {

    private final SubCommandManager manager = new SubCommandManager();

    public PlayerGuardAdminCommand() {
        manager.register("delete", new DeleteCommand());
        manager.register("expand", new ExpandCommand());
        manager.register("lookup", new LookupSectionCommand());
        manager.register("reload", new ReloadCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return true;
        }

        SubCommand sc = manager.getCommand(args[0]);
        if (sc != null && sender.hasPermission(sc.getPermission())) {
            return sc.execute(sender, command, label, Arrays.asList(args).subList(1, args.length));
        }

        sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"権限がないかそのようなコマンドはありません。");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return TabCompletes.sorted(args[0], manager.getCommandNames().stream()
                    .filter(name -> sender.hasPermission(manager.getCommand(name).getPermission()))
                    .collect(Collectors.toList())
            );
        }
        else {
            SubCommand sc = manager.getCommand(args[0]);
            if (sc != null && sender.hasPermission(sc.getPermission())) {
                return sc.tabComplete(sender, command, label, Arrays.asList(args).subList(1, args.length));
            }
        }

        return Collections.emptyList();
    }

}
