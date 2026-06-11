package net.nekozouneko.playerguard.command.sub.admin;

import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;

public class ExpandCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"引数を入力してください。");
            return true;
        }

        Player target = Bukkit.getPlayer(args.get(0));
        long expand;
        try {
            expand = Long.parseLong(args.get(1));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"無効な引数です。");
            return true;
        }

        if (target == null) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当するプレイヤーはオンラインでないか、存在しません。");
            return true;
        }

        final long expandValue = expand;
        PlayerGuard.getInstance().getScheduler().runOnEntity(target, () ->
                target.getPersistentDataContainer().set(
                        new NamespacedKey(PlayerGuard.getInstance(), "limit-extends"),
                        PersistentDataType.LONG, expandValue
                )
        );
        sender.sendMessage(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "プレイヤーの保護制限を設定しました。");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() == 1) {
            return TabCompletes.players(args.get(0), Bukkit.getOnlinePlayers());
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.admin.expand";
    }
}
