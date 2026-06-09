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

public class TransferCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"引数を入力してください。");
            return true;
        }

        Player player = (Player) sender;
        Player transferTo = Bukkit.getPlayer(args.get(0));
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

        if (transferTo == null || transferTo.equals(player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"該当するプレイヤーはオンラインでないか、存在しません。");
            return true;
        }

        requestTransfer(sender, region, transferTo);
        transferTo.sendMessage(String.format(ChatColor.DARK_GREEN+"■ "+ChatColor.GREEN+"%sから%sをの移管リクエストが来ています。/pg confirmで移管を受け付けてください。", region.getId(), player.getName()));

        return true;
    }

    /**
     * region を transferTo へ譲渡するリクエストを発行する。
     * 上限超過時は false を返す。受理は ConfirmCommand 経由。
     */
    public static boolean requestTransfer(CommandSender requester, ProtectedRegion region, Player transferTo) {
        PlayerGuard inst = PlayerGuard.getInstance();

        if (inst.getProtectionUsed(transferTo) + region.volume() > inst.getProtectLimit(transferTo)) {
            requester.sendMessage(ChatColor.DARK_RED + "■ " + ChatColor.RED + "この相手に譲渡することはできません。");
            return false;
        }

        ConfirmCommand.addConfirm(transferTo.getUniqueId(), () -> {
            if (inst.getProtectionUsed(transferTo) + region.volume() > inst.getProtectLimit(transferTo)) {
                requester.sendMessage(ChatColor.DARK_RED + "■ " + ChatColor.RED + "この移管リクエストを受理することができませんでした。");
                return;
            }
            region.getOwners().clear();
            region.getMembers().clear();
            region.getOwners().addPlayer(transferTo.getUniqueId());
            transferTo.sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "%sを%sに移管をしました。", region.getId(), transferTo.getName()));
        });

        requester.sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN + "%sを%sに移管をリクエストしました。", region.getId(), transferTo.getName()));
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
