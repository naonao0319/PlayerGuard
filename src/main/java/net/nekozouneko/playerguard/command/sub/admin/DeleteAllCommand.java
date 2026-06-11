package net.nekozouneko.playerguard.command.sub.admin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.command.sub.playerguard.ConfirmCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DeleteAllCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();

        ConfirmCommand.addConfirm(uuid, () ->
            PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
                rc.getLoaded().forEach(rm ->
                        rm.getRegions().values().stream()
                            .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                            .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                            .forEach(pr -> {
                                rm.removeRegion(pr.getId());
                                sender.sendMessage(String.format(
                                        ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "保護領域「%s」を削除しました。", pr.getId()
                                ));
                            })
                );

                sender.sendMessage(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "PlayerGuardに登録された保護領域の削除が完了しました。");
            })
        );

        sender.sendMessage(ChatColor.GOLD + "■ " + ChatColor.YELLOW + "削除するには/playerguard confirmを実行してください。");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.admin.deleteall";
    }
}
