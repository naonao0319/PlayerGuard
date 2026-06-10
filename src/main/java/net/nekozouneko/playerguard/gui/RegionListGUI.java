package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RegionListGUI extends AbstractGUI {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int SLOT_INFO = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 50;

    private final List<Map.Entry<ProtectedRegion, World>> regions = new ArrayList<>();
    private int page;

    public RegionListGUI(Player player) {
        super(player, null);
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE, "■ あなたの領域一覧");
        inventory.clear();
        regions.clear();

        regions.addAll(PGUtil.getPlayerRegions(getPlayer()).entrySet());
        Collections.sort(regions, Comparator
                .comparing((Map.Entry<ProtectedRegion, World> e) -> e.getValue().getName())
                .thenComparing(e -> e.getKey().getId()));

        int maxPage = regions.isEmpty() ? 0 : (regions.size() - 1) / PAGE_SIZE;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= regions.size()) break;
            Map.Entry<ProtectedRegion, World> entry = regions.get(idx);
            inventory.setItem(i, toItem(entry.getKey(), entry.getValue()));
        }

        PlayerGuard pg = PlayerGuard.getInstance();
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "あなたの情報")
                .lore(
                        ChatColor.GRAY + "保護制限: " + ChatColor.WHITE + pg.getProtectionUsed(getPlayer())
                                + "/" + pg.getProtectLimit(getPlayer()),
                        ChatColor.GRAY + "領域数: " + ChatColor.WHITE + regions.size()
                )
                .build());

        if (page > 0) {
            inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "前のページ").build());
        }
        inventory.setItem(SLOT_CLOSE, ItemStackBuilder.of(Material.BARRIER)
                .name(ChatColor.RED + "閉じる").build());
        if (page < maxPage) {
            inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "次のページ").build());
        }
    }

    private org.bukkit.inventory.ItemStack toItem(ProtectedRegion region, World world) {
        UUID me = getPlayer().getUniqueId();
        RegionRoles.Role role = RegionRoles.roleOf(region, me);
        String roleLabel;
        switch (role) {
            case PRIMARY_OWNER: roleLabel = "主オーナー"; break;
            case SUB_OWNER: roleLabel = "subowner"; break;
            default: roleLabel = "owner"; break;
        }

        return ItemStackBuilder.of(Material.MAP)
                .name(ChatColor.WHITE + region.getId() + "/" + world.getName())
                .lore(
                        ChatColor.GRAY + "権限: " + ChatColor.WHITE + roleLabel,
                        ChatColor.GRAY + "範囲: " + ChatColor.WHITE + String.format("(%d, %d, %d) -> (%d, %d, %d)",
                                region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                                region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()),
                        ChatColor.GRAY + "クリックで管理"
                )
                .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_CLOSE) {
            getPlayer().closeInventory();
            return;
        }
        if (slot == SLOT_PREV) {
            page--;
            init();
            return;
        }
        if (slot == SLOT_NEXT) {
            page++;
            init();
            return;
        }

        if (slot < 0 || slot >= PAGE_SIZE) return;
        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= regions.size()) return;
        ProtectedRegion region = regions.get(idx).getKey();
        new RegionHubGUI(getPlayer(), region, this).open();
    }
}

