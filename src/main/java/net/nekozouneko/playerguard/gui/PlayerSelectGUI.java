package net.nekozouneko.playerguard.gui;

import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 渡されたプレイヤー候補をヘッドで一覧し、クリックで onSelect を呼ぶ汎用GUI。
 */
public class PlayerSelectGUI extends AbstractGUI {

    private final String title;
    private final List<OfflinePlayer> candidates;
    private final Consumer<UUID> onSelect;
    private final List<UUID> slotOwners = new ArrayList<>();

    public PlayerSelectGUI(Player player, AbstractGUI parent,
                           String title, List<OfflinePlayer> candidates, Consumer<UUID> onSelect) {
        super(player, parent);
        this.title = title;
        this.candidates = candidates;
        this.onSelect = onSelect;
    }

    @Override
    public void init() {
        int size = Math.min(54, ((Math.max(candidates.size(), 1) + 8) / 9) * 9 + 9);
        if (inventory == null)
            inventory = Bukkit.createInventory(this, size, title);
        inventory.clear();
        slotOwners.clear();

        int slot = 0;
        for (OfflinePlayer op : candidates) {
            if (slot >= size - 9) break; // 最下段はボタン用に空ける
            ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                    .name(ChatColor.WHITE + (op.getName() != null ? op.getName() : op.getUniqueId().toString()))
                    .build();
            if (head.getItemMeta() instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(op);
                head.setItemMeta(sm);
            }
            inventory.setItem(slot, head);
            slotOwners.add(op.getUniqueId());
            slot++;
        }

        inventory.setItem(size - 1, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "戻る")
                .build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        if (slot == inventory.getSize() - 1) {
            back();
            return;
        }
        if (slot < slotOwners.size()) {
            UUID selected = slotOwners.get(slot);
            onSelect.accept(selected);
        }
    }
}
