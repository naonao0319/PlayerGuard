package net.nekozouneko.playerguard.task;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.region.RegionRentals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class RentalExpiryTask extends BukkitRunnable {

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (RegionManager rm : rc.getLoaded()) {
            for (ProtectedRegion region : new ArrayList<>(rm.getRegions().values())) {
                Set<String> rentals = region.getFlag(PGCustomFlags.RENTALS);
                if (rentals == null || rentals.isEmpty()) continue;
                for (UUID borrower : RegionRentals.purgeExpired(region, now)) {
                    notifyExpiry(region, borrower);
                }
            }
        }
    }

    private void notifyExpiry(ProtectedRegion region, UUID borrower) {
        String borrowerName = Bukkit.getOfflinePlayer(borrower).getName();
        if (borrowerName == null) borrowerName = borrower.toString();

        Player b = Bukkit.getPlayer(borrower);
        if (b != null) {
            b.sendMessage(String.format(ChatColor.GOLD + "■ " + ChatColor.YELLOW
                    + "「%s」の貸出期限が切れました。", region.getId()));
        }
        for (UUID owner : region.getOwners().getUniqueIds()) {
            Player o = Bukkit.getPlayer(owner);
            if (o != null) {
                o.sendMessage(String.format(ChatColor.GOLD + "■ " + ChatColor.YELLOW
                        + "「%s」の%sへの貸出期限が切れたため、自動解除しました。", region.getId(), borrowerName));
            }
        }
    }
}
