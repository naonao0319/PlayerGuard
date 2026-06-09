package net.nekozouneko.playerguard.command.sub.playerguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class AddCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"引数を入力してください。");
            return true;
        }

        Player add = Bukkit.getPlayer(args.get(0));

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
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当する保護領域がありません。");
            return true;
        }

        if (add == null || add.equals(player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当するプレイヤーはオンラインでないか、存在しません。");
            return true;
        }

        switch (net.nekozouneko.playerguard.region.RegionMembers.add(region, add.getUniqueId())) {
            case ALREADY_MEMBER:
                sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当するプレイヤーはすでに追加されています。");
                return true;
            case IS_OWNER:
                sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当するプレイヤーはオーナーです。");
                return true;
            case INVALID:
                sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"追加できませんでした。");
                return true;
            case ADDED:
                sender.sendMessage(String.format(ChatColor.DARK_GREEN+"■ "+ChatColor.GREEN+"%sを追加しました。", add.getName()));
                return true;
        }
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
