package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.command.sub.playerguard.TransferCommand;
import net.nekozouneko.playerguard.region.RegionMembers;
import net.nekozouneko.playerguard.region.RegionRentals;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.region.RegionRoles.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberGUI extends AbstractGUI {

    private static final int SIZE = 54;
    private static final int SLOT_ADD = 46;
    private static final int SLOT_RENT = 48;
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

        long now = System.currentTimeMillis();
        int slot = 0;
        for (UUID uuid : region.getOwners().getUniqueIds()) {
            if (RegionRoles.roleOf(region, uuid) != Role.SUB_OWNER) continue;
            if (slot >= SIZE - 9) break;
            inventory.setItem(slot, memberHead(uuid, ChatColor.GOLD, "subowner", null));
            memberSlots.add(uuid);
            slot++;
        }
        for (UUID uuid : region.getMembers().getUniqueIds()) {
            if (slot >= SIZE - 9) break;
            Long expiry = RegionRentals.getExpiry(region, uuid);
            String rentalLore = expiry != null
                    ? "貸出中: あと" + RegionRentals.formatRemaining(expiry - now) : null;
            inventory.setItem(slot, memberHead(uuid, ChatColor.WHITE, "builder", rentalLore));
            memberSlots.add(uuid);
            slot++;
        }

        inventory.setItem(SLOT_ADD, ItemStackBuilder.of(Material.LIME_DYE)
                .name(ChatColor.GREEN + "メンバーを追加").build());
        inventory.setItem(SLOT_RENT, ItemStackBuilder.of(Material.CLOCK)
                .name(ChatColor.YELLOW + "建築権を貸し出す").build());
        Role viewerRole = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        if (viewerRole == Role.PRIMARY_OWNER || (viewerRole == Role.SUB_OWNER && PGConfig.allowSubownerTransfer())) {
            inventory.setItem(SLOT_TRANSFER, ItemStackBuilder.of(Material.GOLDEN_APPLE)
                    .name(ChatColor.GOLD + "領域を譲渡").build());
        }
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "戻る").build());
    }

    private ItemStack memberHead(UUID uuid, ChatColor nameColor, String roleLabel, String extraLore) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + roleLabel);
        if (extraLore != null) lore.add(ChatColor.YELLOW + extraLore);
        lore.add(ChatColor.GRAY + "クリックで操作");
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(nameColor + (op.getName() != null ? op.getName() : uuid.toString()))
                .lore(lore.toArray(new String[0]))
                .build();
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            head.setItemMeta(sm);
        }
        return head;
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
        if (slot == SLOT_RENT) {
            openRentSelector();
            return;
        }
        if (slot == SLOT_TRANSFER) {
            Role viewerRole = RegionRoles.roleOf(region, getPlayer().getUniqueId());
            if (viewerRole == Role.PRIMARY_OWNER || (viewerRole == Role.SUB_OWNER && PGConfig.allowSubownerTransfer())) {
                openTransferSelector();
            }
            return;
        }
        if (slot < memberSlots.size()) {
            new MemberActionGUI(getPlayer(), this, region, memberSlots.get(slot)).open();
        }
    }

    private List<OfflinePlayer> nonMemberCandidates() {
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID u = online.getUniqueId();
            if (u.equals(getPlayer().getUniqueId())) continue;
            if (region.getOwners().contains(u)) continue;
            if (region.getMembers().contains(u)) continue;
            candidates.add(online);
        }
        return candidates;
    }

    private void openAddSelector() {
        List<OfflinePlayer> candidates = nonMemberCandidates();
        new PlayerSelectGUI(getPlayer(), this, "■ 追加するプレイヤー", candidates, uuid -> {
            RegionMembers.add(region, uuid);
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
            open();
        }).open();
    }

    private void openRentSelector() {
        List<OfflinePlayer> candidates = nonMemberCandidates();
        new PlayerSelectGUI(getPlayer(), this, "■ 貸出先プレイヤー", candidates, uuid -> {
            Player t = Bukkit.getPlayer(uuid);
            String name = t != null ? t.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            new RentalDurationGUI(getPlayer(), this, region, uuid,
                    name != null ? name : uuid.toString()).open();
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
