package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.commons.spigot.persistence.EnumDataType;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.GuardFlags;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Arrays;
import java.util.List;

public class MenuGUI extends AbstractGUI{

    private final ProtectedRegion region;

    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region) {
        this(instance, player, region, null);
    }

    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region, AbstractGUI parent) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        GuardFlags[] values = GuardFlags.values();

        int maxRow = 1;
        for (GuardFlags gf : values) {
            maxRow = Math.max(maxRow, gf.getGuiRow());
        }
        int size = Math.min(54, maxRow * 9);

        if (inventory == null)
            inventory = Bukkit.createInventory(this, size, "■ 権限を管理");
        inventory.clear();

        NamespacedKey key = new NamespacedKey(PlayerGuard.getInstance(), "flag");

        // 各フラグを guiRow で指定された行に左詰めで配置する
        int[] column = new int[maxRow + 1];
        for (GuardFlags gf : values) {
            int slot = (gf.getGuiRow() - 1) * 9 + column[gf.getGuiRow()]++;
            if (slot < 0 || slot >= size) continue;

            ItemStack item = ItemStackBuilder.of(gf.getIcon())
                    .name(ChatColor.WHITE + gf.getDisplayName())
                    .lore(ChatColor.GRAY + "状態："+stateToJapanese(GuardFlags.getState(region, gf)))
                    .persistentData(key, new EnumDataType<>(GuardFlags.class), gf)
                    .build();
            inventory.setItem(slot, item);
        }

        if (getParent() != null) {
            inventory.setItem(size - 1, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "戻る")
                    .build());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;

        if (getParent() != null && e.getCurrentItem().getType() == Material.ARROW) {
            back();
            return;
        }

        NamespacedKey key = new NamespacedKey(PlayerGuard.getInstance(), "flag");
        PersistentDataContainer c = e.getCurrentItem().getItemMeta().getPersistentDataContainer();

        GuardFlags flag = c.get(key, new EnumDataType<>(GuardFlags.class));
        if (flag == null)
            return;

        if (PGConfig.isFlagDisabled(flag)) {
            GuardFlags.initRegionFlag(region, flag);

            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10, 0);
            init();
            return;
        }

        GuardFlags.State state = GuardFlags.getState(region, flag);

        List<GuardFlags.State> states = Arrays.asList(GuardFlags.State.values());

        int index = states.indexOf(state) + 1;
        if (index >= states.size()) {
            index = 0;
        }

        GuardFlags.State nextState = states.get(index);
        if (nextState == GuardFlags.State.SOME_CHANGED) {
            nextState = GuardFlags.State.ALLOW;
        }

        if (nextState == GuardFlags.State.UNSET) {
            for (StateFlag sff : flag.getFlags()) {
                region.setFlag(sff, null);
                region.setFlag(sff.getRegionGroupFlag(), RegionGroup.NONE);
            }
        }
        else {
            boolean b = state == GuardFlags.State.ALLOW;
            for (StateFlag sff : flag.getFlags()) {
                region.setFlag(sff, PGUtil.boolToState(!b));
                region.setFlag(sff.getRegionGroupFlag(), flag.regionGroup());
            }
        }

        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 2);

        init();
    }

    private String stateToJapanese(GuardFlags.State state) {
        switch (state) {
            case ALLOW:
                return "許可";
            case DENY:
                return "拒否";
            case SOME_CHANGED:
                return "管理者により変更されています";
            case UNSET:
                return "設定解除";
        }

        return "不明";
    }
}
