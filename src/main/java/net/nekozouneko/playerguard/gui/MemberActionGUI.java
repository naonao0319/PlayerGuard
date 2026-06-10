package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.region.RegionRentals;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.region.RegionRoles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class MemberActionGUI extends AbstractGUI {

    private static final int SLOT_HEAD = 0;
    private static final int SLOT_PROMOTE = 2;
    private static final int SLOT_DEMOTE = 3;
    private static final int SLOT_CANCEL_RENTAL = 4;
    private static final int SLOT_REMOVE = 6;
    private static final int SLOT_BACK = 8;

    private final ProtectedRegion region;
    private final UUID target;

    public MemberActionGUI(Player player, AbstractGUI parent, ProtectedRegion region, UUID target) {
        super(player, parent);
        this.region = region;
        this.target = target;
    }

    private String targetName() {
        String name = Bukkit.getOfflinePlayer(target).getName();
        return name != null ? name : target.toString();
    }

    private boolean canPromote(Role viewer, Role targetRole, boolean rental) {
        return viewer == Role.PRIMARY_OWNER && targetRole == Role.BUILDER && !rental;
    }

    private boolean canDemote(Role viewer, Role targetRole) {
        return viewer == Role.PRIMARY_OWNER && targetRole == Role.CO_OWNER;
    }

    private boolean canCancelRental(Role viewer, boolean rental) {
        return rental && (viewer == Role.PRIMARY_OWNER || viewer == Role.CO_OWNER);
    }

    private boolean canRemove(Role viewer, Role targetRole, boolean rental) {
        if (rental) return false;
        if (viewer == Role.PRIMARY_OWNER)
            return targetRole == Role.CO_OWNER || targetRole == Role.BUILDER;
        return viewer == Role.CO_OWNER && targetRole == Role.BUILDER;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 9, "■ メンバー操作");
        inventory.clear();

        Role viewer = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        Role targetRole = RegionRoles.roleOf(region, target);
        boolean rental = RegionRentals.isRental(region, target);

        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(ChatColor.WHITE + targetName())
                .lore(ChatColor.GRAY + roleLabel(targetRole, rental))
                .build();
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            head.setItemMeta(sm);
        }
        inventory.setItem(SLOT_HEAD, head);

        if (canPromote(viewer, targetRole, rental)) {
            inventory.setItem(SLOT_PROMOTE, ItemStackBuilder.of(Material.EMERALD)
                    .name(ChatColor.GREEN + "co-ownerに昇格").build());
        }
        if (canDemote(viewer, targetRole)) {
            inventory.setItem(SLOT_DEMOTE, ItemStackBuilder.of(Material.GUNPOWDER)
                    .name(ChatColor.YELLOW + "builderに降格").build());
        }
        if (canCancelRental(viewer, rental)) {
            Long expiry = RegionRentals.getExpiry(region, target);
            inventory.setItem(SLOT_CANCEL_RENTAL, ItemStackBuilder.of(Material.CLOCK)
                    .name(ChatColor.YELLOW + "貸出を解約")
                    .lore(ChatColor.GRAY + "残り: " + (expiry != null
                            ? RegionRentals.formatRemaining(expiry - System.currentTimeMillis()) : "不明"))
                    .build());
        }
        if (canRemove(viewer, targetRole, rental)) {
            inventory.setItem(SLOT_REMOVE, ItemStackBuilder.of(Material.RED_DYE)
                    .name(ChatColor.RED + "メンバーから削除").build());
        }
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "戻る").build());
    }

    private String roleLabel(Role role, boolean rental) {
        if (rental) return "貸出中builder";
        switch (role) {
            case PRIMARY_OWNER: return "主オーナー";
            case CO_OWNER: return "共同オーナー";
            case BUILDER: return "builder";
            default: return "非メンバー";
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        Role viewer = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        Role targetRole = RegionRoles.roleOf(region, target);
        boolean rental = RegionRentals.isRental(region, target);

        switch (e.getRawSlot()) {
            case SLOT_PROMOTE:
                if (canPromote(viewer, targetRole, rental)
                        && RegionRoles.promote(region, target) == RegionRoles.PromoteResult.PROMOTED) {
                    getPlayer().sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN
                            + "%sをco-ownerに昇格しました。", targetName()));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_DEMOTE:
                if (canDemote(viewer, targetRole)
                        && RegionRoles.demote(region, target) == RegionRoles.DemoteResult.DEMOTED) {
                    getPlayer().sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN
                            + "%sをbuilderに降格しました。", targetName()));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_CANCEL_RENTAL:
                if (canCancelRental(viewer, rental)
                        && RegionRoles.removeMember(region, target) == RegionRoles.RemoveRoleResult.REMOVED) {
                    getPlayer().sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN
                            + "%sへの貸出を解約しました。", targetName()));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_REMOVE:
                if (canRemove(viewer, targetRole, rental)
                        && RegionRoles.removeMember(region, target) == RegionRoles.RemoveRoleResult.REMOVED) {
                    getPlayer().sendMessage(String.format(ChatColor.DARK_GREEN + "■ " + ChatColor.GREEN
                            + "%sをメンバーから削除しました。", targetName()));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_BACK:
                back();
                break;
            default:
                break;
        }
    }

    private void clickFeedbackAndBack() {
        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 1);
        back();
    }
}
