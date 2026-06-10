package net.nekozouneko.playerguard.command.sub.playerguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RemoveCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PGMessages.error("このコマンドはプレイヤーのみ実行できます。"));
            return true;
        }

        Player player = (Player) sender;

        if (args.isEmpty()) {
            sender.sendMessage(PGMessages.warn("プレイヤー名を指定してください。"));
            return true;
        }

        Player remove = Bukkit.getPlayer(args.get(0));

        ProtectedRegion region;

        if (args.size() < 2) {
            region = PGUtil.getCurrentPositionRegion(player);
        }
        else {
            Map.Entry<ProtectedRegion, World> result = PGUtil.findPlayerGuardRegions(args.get(1));
            if (result == null) {
                region = null;
            }
            else {
                region = result.getKey();
            }
        }

        if (region == null || !region.getOwners().contains(player.getUniqueId())) {
            sender.sendMessage(PGMessages.error("対象の保護領域が見つかりません。"));
            return true;
        }

        if (remove == null || remove.equals(player)) {
            sender.sendMessage(PGMessages.error("対象プレイヤーが見つからないか、オンラインではありません。"));
            return true;
        }

        if (!region.getMembers().contains(remove.getUniqueId())) {
            sender.sendMessage(PGMessages.warn("%s はメンバーではありません。", PGMessages.highlight(remove.getName())));
            return true;
        }

        region.getMembers().removePlayer(remove.getUniqueId());

        sender.sendMessage(PGMessages.success("%s をメンバーから削除しました。", PGMessages.highlight(remove.getName())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() == 1) {
            return TabCompletes.players(args.get(0), Bukkit.getOnlinePlayers());
        }
        else if (args.size() == 2) {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            Set<String> regions = new HashSet<>();
            rc.getLoaded().forEach(rm ->
                    rm.getRegions().values().stream()
                            .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                            .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                            .filter(pr -> {
                                if (sender instanceof Player) {
                                    return pr.getOwners().contains(((Player) sender).getUniqueId());
                                }
                                else return true;
                            })
                            .map(ProtectedRegion::getId)
                            .forEach(regions::add)
            );

            return TabCompletes.sorted(args.get(1), regions);
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.playerguard";
    }
}
