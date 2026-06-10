package net.nekozouneko.playerguard.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionRolesTest {

    private static final long HOUR = 3_600_000L;

    private ProtectedRegion newRegion() {
        return new ProtectedCuboidRegion("test",
                BlockVector3.at(0, 0, 0), BlockVector3.at(10, 10, 10));
    }

    @Test
    void distinguishesPrimaryAndCoOwnerByFlag() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        r.getOwners().addPlayer(b);
        r.setFlag(PGCustomFlags.PRIMARY_OWNER, a.toString());

        assertEquals(RegionRoles.Role.PRIMARY_OWNER, RegionRoles.roleOf(r, a));
        assertEquals(RegionRoles.Role.SUB_OWNER, RegionRoles.roleOf(r, b));
        assertTrue(RegionRoles.isPrimaryOwner(r, a));
        assertFalse(RegionRoles.isPrimaryOwner(r, b));
    }

    @Test
    void detectsBuilderAndNone() {
        ProtectedRegion r = newRegion();
        UUID m = UUID.randomUUID();
        r.getMembers().addPlayer(m);
        assertEquals(RegionRoles.Role.BUILDER, RegionRoles.roleOf(r, m));
        assertEquals(RegionRoles.Role.NONE, RegionRoles.roleOf(r, UUID.randomUUID()));
    }

    @Test
    void lazyMigrationSetsSoleOwnerAsPrimary() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        r.getOwners().addPlayer(a);

        assertEquals(a, RegionRoles.getPrimaryOwner(r));
        assertEquals(a.toString(), r.getFlag(PGCustomFlags.PRIMARY_OWNER));
    }

    @Test
    void legacyMultiOwnerTreatsAllAsPrimary() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        r.getOwners().addPlayer(b);

        assertNull(RegionRoles.getPrimaryOwner(r));
        assertEquals(RegionRoles.Role.PRIMARY_OWNER, RegionRoles.roleOf(r, a));
        assertEquals(RegionRoles.Role.PRIMARY_OWNER, RegionRoles.roleOf(r, b));
        assertNull(r.getFlag(PGCustomFlags.PRIMARY_OWNER));
    }

    @Test
    void staleFlagRemigrates() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        r.setFlag(PGCustomFlags.PRIMARY_OWNER, UUID.randomUUID().toString());

        assertEquals(a, RegionRoles.getPrimaryOwner(r));
        assertEquals(a.toString(), r.getFlag(PGCustomFlags.PRIMARY_OWNER));
    }

    @Test
    void promotesBuilderToCoOwner() {
        ProtectedRegion r = newRegion();
        UUID owner = UUID.randomUUID();
        UUID m = UUID.randomUUID();
        r.getOwners().addPlayer(owner);
        r.getMembers().addPlayer(m);

        assertEquals(RegionRoles.PromoteResult.PROMOTED, RegionRoles.promote(r, m));
        assertTrue(r.getOwners().contains(m));
        assertFalse(r.getMembers().contains(m));
        assertEquals(RegionRoles.Role.SUB_OWNER, RegionRoles.roleOf(r, m));
    }

    @Test
    void promoteRejectsRentalBuilder() {
        ProtectedRegion r = newRegion();
        UUID m = UUID.randomUUID();
        RegionRentals.rent(r, m, HOUR, 0);
        assertEquals(RegionRoles.PromoteResult.IS_RENTAL, RegionRoles.promote(r, m));
    }

    @Test
    void promoteRejectsNonBuilder() {
        assertEquals(RegionRoles.PromoteResult.NOT_BUILDER,
                RegionRoles.promote(newRegion(), UUID.randomUUID()));
    }

    @Test
    void demotesCoOwnerToBuilder() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        r.getOwners().addPlayer(b);
        r.setFlag(PGCustomFlags.PRIMARY_OWNER, a.toString());

        assertEquals(RegionRoles.DemoteResult.DEMOTED, RegionRoles.demote(r, b));
        assertFalse(r.getOwners().contains(b));
        assertTrue(r.getMembers().contains(b));
    }

    @Test
    void demoteRejectsPrimaryOwner() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        assertEquals(RegionRoles.DemoteResult.NOT_CO_OWNER, RegionRoles.demote(r, a));
    }

    @Test
    void removesCoOwnerFromOwnersDomain() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        r.getOwners().addPlayer(b);
        r.setFlag(PGCustomFlags.PRIMARY_OWNER, a.toString());

        assertEquals(RegionRoles.RemoveRoleResult.REMOVED, RegionRoles.removeMember(r, b));
        assertFalse(r.getOwners().contains(b));
    }

    @Test
    void removeMemberRejectsPrimaryOwner() {
        ProtectedRegion r = newRegion();
        UUID a = UUID.randomUUID();
        r.getOwners().addPlayer(a);
        assertEquals(RegionRoles.RemoveRoleResult.NOT_REMOVABLE, RegionRoles.removeMember(r, a));
        assertTrue(r.getOwners().contains(a));
    }

    @Test
    void removeMemberRemovesBuilder() {
        ProtectedRegion r = newRegion();
        UUID m = UUID.randomUUID();
        r.getMembers().addPlayer(m);
        assertEquals(RegionRoles.RemoveRoleResult.REMOVED, RegionRoles.removeMember(r, m));
        assertFalse(r.getMembers().contains(m));
    }
}
