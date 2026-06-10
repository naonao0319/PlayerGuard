package net.nekozouneko.playerguard.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.UUID;

/**
 * 領域メンバーの追加/削除のコアロジック。
 * Bukkit に依存せず UUID と {@link ProtectedRegion} のみで動くため単体テスト可能。
 */
public final class RegionMembers {

    public enum AddResult { ADDED, ALREADY_MEMBER, IS_OWNER, INVALID }
    public enum RemoveResult { REMOVED, NOT_MEMBER }

    private RegionMembers() {}

    public static AddResult add(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return AddResult.INVALID;
        if (region.getOwners().contains(target)) return AddResult.IS_OWNER;
        if (region.getMembers().contains(target)) return AddResult.ALREADY_MEMBER;
        region.getMembers().addPlayer(target);
        return AddResult.ADDED;
    }

    public static RemoveResult remove(ProtectedRegion region, UUID target) {
        if (region == null || target == null || !region.getMembers().contains(target)) {
            return RemoveResult.NOT_MEMBER;
        }
        region.getMembers().removePlayer(target);
        RegionRentals.removeEntry(region, target);
        return RemoveResult.REMOVED;
    }
}
