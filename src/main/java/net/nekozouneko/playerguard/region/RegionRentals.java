package net.nekozouneko.playerguard.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 期限付きbuilder貸出のコアロジック。
 * エントリ書式は "&lt;uuid&gt;:&lt;期限epochミリ秒&gt;"。壊れたエントリは読み飛ばし、purge/再書き込み時に破棄する。
 * Bukkit に依存せず単体テスト可能。
 */
public final class RegionRentals {

    public enum RentResult { RENTED, EXTENDED, ALREADY_MEMBER, INVALID }

    private RegionRentals() {}

    /** 貸出エントリ1件。パース不能なら parse は null を返す。 */
    private static final class Entry {
        final UUID uuid;
        final long expiry;

        Entry(UUID uuid, long expiry) {
            this.uuid = uuid;
            this.expiry = expiry;
        }

        static Entry parse(String raw) {
            if (raw == null) return null;
            int idx = raw.lastIndexOf(':');
            if (idx <= 0 || idx == raw.length() - 1) return null;
            try {
                return new Entry(UUID.fromString(raw.substring(0, idx)),
                        Long.parseLong(raw.substring(idx + 1)));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * 非メンバーへ期限付きで建築権を貸し出す。既に貸出中なら期限を上書き(延長/短縮)する。
     * 通常builder・オーナーには貸し出せない。
     */
    public static RentResult rent(ProtectedRegion region, UUID target, long durationMillis, long now) {
        if (region == null || target == null || durationMillis <= 0) return RentResult.INVALID;
        if (region.getOwners().contains(target)) return RentResult.ALREADY_MEMBER;

        boolean extending = getExpiry(region, target) != null;
        if (!extending && region.getMembers().contains(target)) return RentResult.ALREADY_MEMBER;

        Set<String> entries = new HashSet<>();
        Set<String> current = region.getFlag(PGCustomFlags.RENTALS);
        if (current != null) {
            for (String raw : current) {
                Entry e = Entry.parse(raw);
                if (e == null || e.uuid.equals(target)) continue;
                entries.add(raw);
            }
        }
        entries.add(target + ":" + (now + durationMillis));
        region.setFlag(PGCustomFlags.RENTALS, entries);
        region.getMembers().addPlayer(target);
        return extending ? RentResult.EXTENDED : RentResult.RENTED;
    }

    /** 貸出期限(epochミリ秒)。貸出中でなければ null。 */
    public static Long getExpiry(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return null;
        Set<String> current = region.getFlag(PGCustomFlags.RENTALS);
        if (current == null) return null;
        for (String raw : current) {
            Entry e = Entry.parse(raw);
            if (e != null && e.uuid.equals(target)) return e.expiry;
        }
        return null;
    }

    public static boolean isRental(ProtectedRegion region, UUID target) {
        return getExpiry(region, target) != null;
    }

    /**
     * 期限切れエントリを削除し、該当プレイヤーをmembersドメインからも除去する。
     * 壊れたエントリはこのタイミングで破棄する。除去したUUIDのリストを返す(通知用)。
     */
    public static List<UUID> purgeExpired(ProtectedRegion region, long now) {
        if (region == null) return Collections.emptyList();
        Set<String> current = region.getFlag(PGCustomFlags.RENTALS);
        if (current == null || current.isEmpty()) return Collections.emptyList();

        List<UUID> removed = new ArrayList<>();
        Set<String> keep = new HashSet<>();
        for (String raw : current) {
            Entry e = Entry.parse(raw);
            if (e == null) continue;
            if (e.expiry <= now) {
                region.getMembers().removePlayer(e.uuid);
                removed.add(e.uuid);
            } else {
                keep.add(raw);
            }
        }
        if (!removed.isEmpty() || keep.size() != current.size()) {
            region.setFlag(PGCustomFlags.RENTALS, keep.isEmpty() ? null : keep);
        }
        return removed;
    }

    /** 貸出エントリのみ削除する(メンバー除去はしない)。{@link RegionMembers#remove} から呼ばれる。 */
    static void removeEntry(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return;
        Set<String> current = region.getFlag(PGCustomFlags.RENTALS);
        if (current == null) return;
        Set<String> keep = new HashSet<>();
        for (String raw : current) {
            Entry e = Entry.parse(raw);
            if (e == null || e.uuid.equals(target)) continue;
            keep.add(raw);
        }
        region.setFlag(PGCustomFlags.RENTALS, keep.isEmpty() ? null : keep);
    }

    /** 残り時間の表示用文字列(例: "3日4時間" / "2時間5分" / "5分" / "1分未満")。 */
    public static String formatRemaining(long remainingMillis) {
        if (remainingMillis < 60_000L) return "1分未満";
        long totalMinutes = remainingMillis / 60_000L;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;
        if (days > 0) return days + "日" + hours + "時間";
        if (hours > 0) return hours + "時間" + minutes + "分";
        return minutes + "分";
    }
}
