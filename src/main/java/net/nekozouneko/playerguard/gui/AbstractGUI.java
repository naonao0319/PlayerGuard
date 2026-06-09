package net.nekozouneko.playerguard.gui;

import lombok.Getter;
import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class AbstractGUI implements Listener, InventoryHolder {

    @Getter
    private final Player player;
    /** 戻り先GUI。null ならルート(戻る=閉じる)。 */
    @Getter
    private final AbstractGUI parent;
    protected Inventory inventory;
    private boolean registered = false;

    public AbstractGUI(Player player) {
        this(player, null);
    }

    public AbstractGUI(Player player, AbstractGUI parent) {
        this.player = player;
        this.parent = parent;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void init();

    public void open() {
        init();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, PlayerGuard.getInstance());
            registered = true;
        }
        player.openInventory(inventory);
    }

    /** 親があれば親を開き直し、無ければインベントリを閉じる。 */
    public void back() {
        if (parent != null) {
            parent.open();
        } else {
            player.closeInventory();
        }
    }

    protected void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    /**
     * このGUIが閉じられたら自身のリスナーを解除する。
     * 別GUIへ遷移する際も openInventory により本イベントが発火し、
     * 古いGUIのリスナーだけが解除される。
     */
    @EventHandler
    public void onAbstractClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this) return;
        unregister();
    }
}
