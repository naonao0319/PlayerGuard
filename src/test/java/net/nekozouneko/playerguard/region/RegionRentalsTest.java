package net.nekozouneko.playerguard.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionRentalsTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;

    private ProtectedRegion newRegion() {
        return new ProtectedCuboidRegion("test",
                BlockVector3.at(0, 0, 0), BlockVector3.at(10, 10, 10));
    }

    @Test
    void rentsToNonMember() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        assertEquals(RegionRentals.RentResult.RENTED, RegionRentals.rent(r, u, HOUR, 0));
        assertTrue(r.getMembers().contains(u));
        assertEquals(Long.valueOf(HOUR), RegionRentals.getExpiry(r, u));
        assertTrue(RegionRentals.isRental(r, u));
    }

    @Test
    void extendsExistingRental() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionRentals.rent(r, u, HOUR, 0);
        assertEquals(RegionRentals.RentResult.EXTENDED, RegionRentals.rent(r, u, 2 * HOUR, 0));
        assertEquals(Long.valueOf(2 * HOUR), RegionRentals.getExpiry(r, u));
    }

    @Test
    void rejectsNormalBuilder() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        r.getMembers().addPlayer(u);
        assertEquals(RegionRentals.RentResult.ALREADY_MEMBER, RegionRentals.rent(r, u, HOUR, 0));
        assertFalse(RegionRentals.isRental(r, u));
    }

    @Test
    void rejectsOwner() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        r.getOwners().addPlayer(u);
        assertEquals(RegionRentals.RentResult.ALREADY_MEMBER, RegionRentals.rent(r, u, HOUR, 0));
    }

    @Test
    void rejectsInvalidArgs() {
        ProtectedRegion r = newRegion();
        assertEquals(RegionRentals.RentResult.INVALID, RegionRentals.rent(r, null, HOUR, 0));
        assertEquals(RegionRentals.RentResult.INVALID, RegionRentals.rent(null, UUID.randomUUID(), HOUR, 0));
        assertEquals(RegionRentals.RentResult.INVALID, RegionRentals.rent(r, UUID.randomUUID(), 0, 0));
    }

    @Test
    void purgeRemovesExpiredOnly() {
        ProtectedRegion r = newRegion();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        RegionRentals.rent(r, u1, HOUR, 0);
        RegionRentals.rent(r, u2, 3 * HOUR, 0);

        List<UUID> removed = RegionRentals.purgeExpired(r, 2 * HOUR);

        assertEquals(1, removed.size());
        assertEquals(u1, removed.get(0));
        assertFalse(r.getMembers().contains(u1));
        assertTrue(r.getMembers().contains(u2));
        assertTrue(RegionRentals.isRental(r, u2));
        assertFalse(RegionRentals.isRental(r, u1));
    }

    @Test
    void purgeDropsMalformedEntries() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        Set<String> set = new HashSet<>();
        set.add("garbage-entry");
        set.add(u + ":" + HOUR);
        r.setFlag(PGCustomFlags.RENTALS, set);

        List<UUID> removed = RegionRentals.purgeExpired(r, 0);

        assertTrue(removed.isEmpty());
        Set<String> after = r.getFlag(PGCustomFlags.RENTALS);
        assertNotNull(after);
        assertEquals(1, after.size());
        assertTrue(after.contains(u + ":" + HOUR));
    }

    @Test
    void purgeClearsFlagWhenAllExpired() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionRentals.rent(r, u, HOUR, 0);
        RegionRentals.purgeExpired(r, 2 * HOUR);
        assertNull(r.getFlag(PGCustomFlags.RENTALS));
    }

    @Test
    void formatsRemainingTime() {
        assertEquals("1分未満", RegionRentals.formatRemaining(30_000L));
        assertEquals("5分", RegionRentals.formatRemaining(5 * 60_000L));
        assertEquals("2時間5分", RegionRentals.formatRemaining(2 * HOUR + 5 * 60_000L));
        assertEquals("3日4時間", RegionRentals.formatRemaining(3 * DAY + 4 * HOUR + 5 * 60_000L));
    }
}
