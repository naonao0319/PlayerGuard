package net.nekozouneko.playerguard.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.visitlog.VisitorLogService;
import net.nekozouneko.playerguard.visitlog.VisitorLogType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class VisitorLogListener implements Listener {

    private final VisitorLogService service;

    public VisitorLogListener(VisitorLogService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        ProtectedRegion region = regionAt(e.getTo());
        boolean isVisitor = isVisitor(region, e.getPlayer().getUniqueId());
        String worldName = e.getPlayer().getWorld().getName();
        String regionId = region != null ? region.getId() : null;
        service.trackRegion(e.getPlayer().getUniqueId(), worldName, regionId, isVisitor);
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        service.trackRegion(e.getPlayer().getUniqueId(), e.getPlayer().getWorld().getName(), null, false);
        service.forget(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        service.trackRegion(e.getPlayer().getUniqueId(), e.getFrom().getName(), null, false);
        service.forget(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        ProtectedRegion region = regionAt(e.getBlock().getLocation());
        if (!isVisitor(region, e.getPlayer().getUniqueId())) return;
        service.logAction(e.getBlock().getWorld().getName(), region.getId(), VisitorLogType.BREAK,
                e.getPlayer().getUniqueId(), formatBlock(e.getBlock().getType(), e.getBlock().getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ProtectedRegion region = regionAt(e.getBlockPlaced().getLocation());
        if (!isVisitor(region, e.getPlayer().getUniqueId())) return;
        service.logAction(e.getBlockPlaced().getWorld().getName(), region.getId(), VisitorLogType.PLACE,
                e.getPlayer().getUniqueId(), formatBlock(e.getBlockPlaced().getType(), e.getBlockPlaced().getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (!isLoggedInteract(e.getClickedBlock().getType())) return;

        ProtectedRegion region = regionAt(e.getClickedBlock().getLocation());
        if (!isVisitor(region, e.getPlayer().getUniqueId())) return;
        service.logAction(e.getClickedBlock().getWorld().getName(), region.getId(), VisitorLogType.INTERACT,
                e.getPlayer().getUniqueId(), formatBlock(e.getClickedBlock().getType(), e.getClickedBlock().getLocation()));
    }

    private ProtectedRegion regionAt(Location location) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        ApplicableRegionSet ars = rm.getApplicableRegions(BukkitAdapter.asBlockVector(location));

        return ars.getRegions().stream()
                .filter(region -> !(region instanceof GlobalProtectedRegion))
                .filter(region -> StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                .findFirst().orElse(null);
    }

    private boolean isVisitor(ProtectedRegion region, UUID uuid) {
        if (region == null || uuid == null) return false;
        return RegionRoles.roleOf(region, uuid) == RegionRoles.Role.NONE;
    }

    private boolean isLoggedInteract(Material type) {
        if (type == null) return false;
        switch (type) {
            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case ENDER_CHEST:
            case HOPPER:
            case DISPENSER:
            case DROPPER:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case SHULKER_BOX:
                return true;
            default:
                return type.name().endsWith("_SHULKER_BOX");
        }
    }

    private String formatBlock(Material type, Location location) {
        return String.format("%s (%d, %d, %d)",
                type.name(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}
