package net.nekozouneko.playerguard.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.GuardFlags;
import net.nekozouneko.playerguard.flag.GuardRegisteredFlag;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.selection.SelectionStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        SelectionStorage ss = PlayerGuard.getInstance().getSelectionStorage();
        Player p = (Player) sender;

        CuboidRegion cr = ss.getSelection(p.getUniqueId());

        if (cr == null) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"金の斧で保護する範囲を指定してください。");
            return true;
        }

        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rm = rc.get(BukkitAdapter.adapt(p.getWorld()));

        long used = PlayerGuard.getInstance().getProtectionUsed(p);
        long limit = PlayerGuard.getInstance().getProtectLimit(p);

        if (36 > cr.getVolume()) {
            sender.sendMessage(ChatColor.DARK_RED + "■ "+ChatColor.RED+"保護領域の最小サイズは36ブロックです。(36 > "+cr.getVolume()+")");
            return true;
        }

        if (limit <= used + cr.getVolume()) {
            ss.clear(p.getUniqueId());
            p.sendMessage(String.format(ChatColor.DARK_RED +"■ "+ChatColor.RED+"保護領域の制限を超過しています。(%d (%d) > %d)", used + cr.getVolume(), cr.getVolume(), limit));
            return true;
        }

        Set<String> allRegionIds = new HashSet<>();
        rc.getLoaded().forEach(rm2 -> allRegionIds.addAll(rm2.getRegions().keySet()));

        String id = Integer.toHexString(new Random().nextInt(0x10000000));
        int timeout = 30;
        boolean timedout = false;
        while (allRegionIds.contains(id)) {
            id = Integer.toHexString(new Random().nextInt(0x10000000));
            timeout--;

            if (timeout <= 0) {
                timedout = true;
                break;
            }
        }

        if (timedout) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"IDを生成できませんでした。再度試行してください。");
            return true;
        }

        ProtectedRegion protect = new ProtectedCuboidRegion(id, cr.getPos1(), cr.getPos2());
        protect.getOwners().addPlayer(p.getUniqueId());
        RegionRoles.setPrimaryOwner(protect, p.getUniqueId());
        GuardFlags.initRegionFlags(protect);
        protect.setFlag(PlayerGuard.getGuardRegisteredFlag(), StateFlag.State.ALLOW);

        ApplicableRegionSet ars = rm.getApplicableRegions(protect);
        long count = ars.getRegions().stream()
                .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                .count();

        if (count > 0 || ars.testState(WorldGuardPlugin.inst().wrapPlayer(p), PlayerGuard.getGuardIgnoredFlag())) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"領域が重複しています。");
            return true;
        }

        long minDistance = Long.MAX_VALUE;
        for (ProtectedRegion otherRegion : rm.getRegions().values()) {
            if (otherRegion.getFlag(PlayerGuard.getGuardRegisteredFlag()) != StateFlag.State.ALLOW) continue;
            if (!PGConfig.doApplyToSamePlayerSRegion() && otherRegion.getOwners().contains(p.getUniqueId())) continue;

            long d = PGUtil.distanceBetweenRegions(protect, otherRegion);
            if (d < 0) continue;
            minDistance = Math.min(minDistance, d);
        }

        if (minDistance <= PGConfig.getMinSpacingBetweenRegions()) {
            sender.sendMessage(String.format(ChatColor.DARK_RED+"■ "+ChatColor.RED+"領域が他の領域と近いです。(%d)", minDistance));
            return true;
        }

        rm.addRegion(protect);
        ss.clear(p.getUniqueId());

        p.sendMessage(String.format(ChatColor.DARK_GREEN+"■ "+ChatColor.GREEN+"保護領域「%s」を設定しました。(残り%dブロック)", id, limit-(used+protect.volume())));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
