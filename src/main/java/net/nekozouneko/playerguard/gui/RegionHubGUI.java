package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

/**
 * /pg で開く領域管理ハブ。フラグ設定/メンバー管理/領域情報への入口。
 */
public class RegionHubGUI extends AbstractGUI {

    private static final int SLOT_FLAGS = 2;
    private static final int SLOT_MEMBERS = 4;
    private static final int SLOT_INFO = 6;
    private static final int SLOT_VISITOR_LOG = 7;
    private static final int SLOT_CLOSE = 8;

    private final ProtectedRegion region;

    public RegionHubGUI(Player player, ProtectedRegion region) {
        super(player, null);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 9, "■ 領域の管理");
        inventory.clear();

        UUID primary = RegionRoles.getPrimaryOwner(region);
        String primaryName = primary != null ? Bukkit.getOfflinePlayer(primary).getName() : null;
        String primaryLabel;
        if (primary == null) primaryLabel = "(未設定)";
        else primaryLabel = primaryName != null ? primaryName : primary.toString();

        inventory.setItem(SLOT_FLAGS, ItemStackBuilder.of(Material.REDSTONE_TORCH)
                .name(ChatColor.WHITE + "フラグ設定").build());
        inventory.setItem(SLOT_MEMBERS, ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(ChatColor.WHITE + "メンバー管理").build());
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.WHITE + "領域情報")
                .lore(
                        ChatColor.GRAY + "ID: " + ChatColor.WHITE + region.getId(),
                        ChatColor.GRAY + "範囲: " + ChatColor.WHITE + String.format("(%d, %d, %d) -> (%d, %d, %d)",
                                region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                                region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()),
                        ChatColor.GRAY + "主オーナー: " + ChatColor.WHITE + primaryLabel,
                        ChatColor.GRAY + "オーナー数: " + ChatColor.WHITE + region.getOwners().size(),
                        ChatColor.GRAY + "メンバー数: " + ChatColor.WHITE + region.getMembers().size()
                ).build());
        inventory.setItem(SLOT_VISITOR_LOG, ItemStackBuilder.of(Material.BOOK)
                .name(ChatColor.WHITE + "訪問者ログ").build());
        inventory.setItem(SLOT_CLOSE, ItemStackBuilder.of(Material.BARRIER)
                .name(ChatColor.RED + "閉じる").build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        switch (e.getRawSlot()) {
            case SLOT_FLAGS:
                new MenuGUI(PlayerGuard.getInstance(), getPlayer(), region, this).open();
                break;
            case SLOT_MEMBERS:
                new MemberGUI(getPlayer(), this, region).open();
                break;
            case SLOT_VISITOR_LOG:
                new VisitorLogGUI(getPlayer(), this, region).open();
                break;
            case SLOT_CLOSE:
                getPlayer().closeInventory();
                break;
            default:
                break;
        }
    }
}
