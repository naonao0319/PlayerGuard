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
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class InfoCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        ProtectedRegion region;
        World world;

        if (args.isEmpty()) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(PGMessages.warn("領域IDを指定してください。"));
                return true;
            }

            region = PGUtil.getCurrentPositionRegion((Player) sender);
            world = ((Player) sender).getWorld();
        }
        else {
            Map.Entry<ProtectedRegion, World> result = PGUtil.findPlayerGuardRegions(args.get(0));
            if (result == null) {
                region = null;
                world = null;
            }
            else {
                region = result.getKey();
                world = result.getValue();
            }
        }

        if (region == null) {
            sender.sendMessage(PGMessages.error("対象の保護領域が見つかりません。"));
            return true;
        }

        sender.sendMessage(PGMessages.title("領域情報 %s", PGMessages.highlight(region.getId())));
        sender.sendMessage(PGMessages.detail("ワールド", PGMessages.highlight(world.getName())));
        sender.sendMessage(PGMessages.detail("範囲", String.format("(%d, %d, %d) -> (%d, %d, %d)",
                region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()
        )));
        sender.sendMessage(PGMessages.detail("所有者", region.getOwners().getUniqueIds().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .collect(Collectors.joining(", "))));
        String members = region.getMembers().getUniqueIds().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .collect(Collectors.joining(", "));
        sender.sendMessage(PGMessages.detail("メンバー", members.isEmpty() ? "-" : members));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        if (args.size() == 1) {
            RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
            Set<String> regions = new HashSet<>();
            rc.getLoaded().forEach(rm ->
                    rm.getRegions().values().stream()
                            .filter(pr -> StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                            .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                            .filter(pr -> {
                                if (sender instanceof Player) {
                                    return pr.getOwners().contains(((Player) sender).getUniqueId());
                                } else return true;
                            })
                            .map(ProtectedRegion::getId)
                            .forEach(regions::add)
            );

            return TabCompletes.sorted(args.get(0), regions);
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.playerguard";
    }
}
