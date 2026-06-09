package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.command.sub.playerguard.TransferCommand;
import net.nekozouneko.playerguard.region.RegionMembers;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 領域メンバーの一覧・削除・追加・譲渡を行うGUI。
 */
public class MemberGUI extends AbstractGUI {

    private static final int SIZE = 54;
    private static final int SLOT_ADD = 48;
    private static final int SLOT_TRANSFER = 50;
    private static final int SLOT_BACK = 53;

    private final ProtectedRegion region;
    private final List<UUID> memberSlots = new ArrayList<>();

    public MemberGUI(Player player, AbstractGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE, "■ メンバー管理");
        inventory.clear();
        memberSlots.clear();

        int slot = 0;
        for (UUID uuid : region.getMembers().getUniqueIds()) {
            if (slot >= SIZE - 9) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                    .name(ChatColor.WHITE + (op.getName() != null ? op.getName() : uuid.toString()))
                    .lore(ChatColor.GRAY + "クリックで削除")
                    .build();
            if (head.getItemMeta() instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(op);
                head.setItemMeta(sm);
            }
            inventory.setItem(slot, head);
            memberSlots.add(uuid);
            slot++;
        }

        inventory.setItem(SLOT_ADD, ItemStackBuilder.of(Material.LIME_DYE)
                .name(ChatColor.GREEN + "メンバーを追加").build());
        inventory.setItem(SLOT_TRANSFER, ItemStackBuilder.of(Material.GOLDEN_APPLE)
                .name(ChatColor.GOLD + "領域を譲渡").build());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "戻る").build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        if (slot == SLOT_BACK) {
            back();
            return;
        }
        if (slot == SLOT_ADD) {
            openAddSelector();
            return;
        }
        if (slot == SLOT_TRANSFER) {
            openTransferSelector();
            return;
        }
        if (slot < memberSlots.size()) {
            UUID target = memberSlots.get(slot);
            RegionMembers.remove(region, target);
            getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 1);
            init();
        }
    }

    private void openAddSelector() {
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID u = online.getUniqueId();
            if (u.equals(getPlayer().getUniqueId())) continue;
            if (region.getOwners().contains(u)) continue;
            if (region.getMembers().contains(u)) continue;
            candidates.add(online);
        }
        new PlayerSelectGUI(getPlayer(), this, "■ 追加するプレイヤー", candidates, uuid -> {
            RegionMembers.add(region, uuid);
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
            open();
        }).open();
    }

    private void openTransferSelector() {
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(getPlayer().getUniqueId())) continue;
            candidates.add(online);
        }
        new PlayerSelectGUI(getPlayer(), this, "■ 譲渡先プレイヤー", candidates, uuid -> {
            Player to = Bukkit.getPlayer(uuid);
            if (to != null) {
                TransferCommand.requestTransfer(getPlayer(), region, to);
            } else {
                getPlayer().sendMessage(ChatColor.DARK_RED + "■ " + ChatColor.RED + "そのプレイヤーはオフラインになりました。");
            }
            getPlayer().closeInventory();
        }).open();
    }
}
