package net.nekozouneko.playerguard.command.sub.admin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.command.sub.playerguard.ConfirmCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeleteCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"削除する領域IDを指定してください。 (/pg info や /rg info で確認できます)");
            return true;
        }

        Player p = (Player) sender;
        String id = args.get(0);

        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(p.getWorld()));

        ProtectedRegion region = rm.getRegion(id);
        if (region == null
                || region instanceof GlobalProtectedRegion
                || !StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag()))) {
            sender.sendMessage(String.format(
                    ChatColor.DARK_RED+"■ "+ChatColor.RED+"このワールドにID「%s」の保護領域は見つかりません。", id
            ));
            return true;
        }

        final String targetId = region.getId();
        ConfirmCommand.addConfirm(p.getUniqueId(), () ->
            PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
                rm.removeRegion(targetId);
                sender.sendMessage(String.format(
                        ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "保護領域「%s」を削除しました。", targetId
                ));
            })
        );

        sender.sendMessage(String.format(
                ChatColor.GOLD + "■ " + ChatColor.YELLOW + "保護領域「%s」を削除するには/playerguard confirmを実行してください。", targetId
        ));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() == 1 && sender instanceof Player) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(((Player) sender).getWorld()));

            Set<String> regions = new HashSet<>();
            rm.getRegions().values().stream()
                    .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                    .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                    .map(ProtectedRegion::getId)
                    .forEach(regions::add);

            return TabCompletes.sorted(args.get(0), regions);
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.admin.delete";
    }
}
