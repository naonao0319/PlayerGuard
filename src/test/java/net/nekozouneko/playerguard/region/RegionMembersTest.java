package net.nekozouneko.playerguard.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionMembersTest {

    private ProtectedRegion newRegion() {
        return new ProtectedCuboidRegion("test",
                BlockVector3.at(0, 0, 0), BlockVector3.at(10, 10, 10));
    }

    @Test
    void addsNewMember() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        assertEquals(RegionMembers.AddResult.ADDED, RegionMembers.add(r, u));
        assertTrue(r.getMembers().contains(u));
    }

    @Test
    void rejectsDuplicateMember() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionMembers.add(r, u);
        assertEquals(RegionMembers.AddResult.ALREADY_MEMBER, RegionMembers.add(r, u));
    }

    @Test
    void rejectsOwnerAsMember() {
        ProtectedRegion r = newRegion();
        UUID owner = UUID.randomUUID();
        r.getOwners().addPlayer(owner);
        assertEquals(RegionMembers.AddResult.IS_OWNER, RegionMembers.add(r, owner));
    }

    @Test
    void rejectsNull() {
        assertEquals(RegionMembers.AddResult.INVALID, RegionMembers.add(newRegion(), null));
    }

    @Test
    void removesExistingMember() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionMembers.add(r, u);
        assertEquals(RegionMembers.RemoveResult.REMOVED, RegionMembers.remove(r, u));
        assertFalse(r.getMembers().contains(u));
    }

    @Test
    void removeClearsRentalEntry() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionRentals.rent(r, u, 3_600_000L, 0);

        assertEquals(RegionMembers.RemoveResult.REMOVED, RegionMembers.remove(r, u));
        assertFalse(r.getMembers().contains(u));
        assertFalse(RegionRentals.isRental(r, u));
    }

    @Test
    void removeMissingMemberReports() {
        ProtectedRegion r = newRegion();
        assertEquals(RegionMembers.RemoveResult.NOT_MEMBER, RegionMembers.remove(r, UUID.randomUUID()));
    }
}
