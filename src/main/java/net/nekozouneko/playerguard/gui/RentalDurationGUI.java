package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.region.RegionRentals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class RentalDurationGUI extends AbstractGUI {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;

    private static final long[] DURATIONS = { HOUR, DAY, 7 * DAY, 30 * DAY };
    private static final String[] LABELS = { "1時間", "1日", "7日", "30日" };
    private static final int[] SLOTS = { 1, 3, 5, 7 };
    private static final int SLOT_BACK = 8;

    private final ProtectedRegion region;
    private final UUID target;
    private final String targetName;

    public RentalDurationGUI(Player player, AbstractGUI parent,
                             ProtectedRegion region, UUID target, String targetName) {
        super(player, parent);
        this.region = region;
        this.target = target;
        this.targetName = targetName;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 9, "■ 貸出期間: " + targetName);
        inventory.clear();

        for (int i = 0; i < DURATIONS.length; i++) {
            inventory.setItem(SLOTS[i], ItemStackBuilder.of(Material.CLOCK)
                    .name(ChatColor.WHITE + LABELS[i])
                    .lore(ChatColor.GRAY + "クリックで" + LABELS[i] + "貸し出す")
                    .build());
        }
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "戻る").build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_BACK) {
            back();
            return;
        }
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                rent(DURATIONS[i], LABELS[i]);
                return;
            }
        }
    }

    private void rent(long duration, String label) {
        RegionRentals.RentResult result =
                RegionRentals.rent(region, target, duration, System.currentTimeMillis());
        switch (result) {
            case RENTED:
            case EXTENDED:
                getPlayer().sendMessage(PGMessages.success(
                        "%s に領域 %s の建築権を %s 貸し出しました。",
                        PGMessages.highlight(targetName),
                        PGMessages.highlight(region.getId()),
                        PGMessages.highlight(label)
                ));
                getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
                Player t = Bukkit.getPlayer(target);
                if (t != null) {
                    t.sendMessage(PGMessages.info(
                            "%s から領域 %s の建築権を %s 貸与されました。",
                            PGMessages.highlight(getPlayer().getName()),
                            PGMessages.highlight(region.getId()),
                            PGMessages.highlight(label)
                    ));
                }
                break;
            case ALREADY_MEMBER:
                getPlayer().sendMessage(PGMessages.warn("そのプレイヤーはすでにメンバーです。"));
                break;
            default:
                getPlayer().sendMessage(PGMessages.error("建築権の貸出に失敗しました。"));
                break;
        }
        back();
    }
}
