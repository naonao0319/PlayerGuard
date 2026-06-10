package net.nekozouneko.playerguard.command.sub.playerguard;

import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ConfirmCommand extends SubCommand {

    private final static Map<UUID, Runnable> confirms = new HashMap<>();
    private final static Map<UUID, Long> timeouts = new HashMap<>();

    public static synchronized void addConfirm(UUID player, Runnable runnable) {
        confirms.put(player, runnable);
        timeouts.put(player, System.currentTimeMillis() + 60000);
    }

    public static synchronized void removeConfirm(UUID player) {
        confirms.remove(player);
        timeouts.remove(player);
    }

    public static synchronized Runnable getConfirm(UUID player) {
        Runnable task = confirms.get(player);
        Long timeout = timeouts.get(player);

        if (timeout == null || System.currentTimeMillis() > timeout) {
            removeConfirm(player);
            return null;
        }

        return task;
    }

    public static synchronized Map<UUID, Runnable> getConfirms() {
        new HashSet<>(confirms.keySet()).forEach(uuid -> {
            Long timeout = timeouts.get(uuid);
            if (timeout == null || System.currentTimeMillis() > timeout) {
                removeConfirm(uuid);
            }
        });

        return new HashMap<>(confirms);
    }

    public static synchronized void clearConfirms() {
        confirms.clear();
        timeouts.clear();
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            return true;
        }

        Runnable confirm = getConfirm(sender instanceof Player ? ((Player) sender).getUniqueId() : null);

        if (confirm == null) {
            sender.sendMessage(PGMessages.warn("確定できる処理が見つかりませんでした。"));
            return true;
        }

        confirm.run();
        removeConfirm(sender instanceof Player ? ((Player) sender).getUniqueId() : null);
        sender.sendMessage(PGMessages.success("操作を続行しました。"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.playerguard";
    }
}
