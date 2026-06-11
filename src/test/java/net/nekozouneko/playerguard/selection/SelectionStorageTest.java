package net.nekozouneko.playerguard.selection;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SelectionStorageTest {

    private CuboidRegion region(int x, int y, int z) {
        return new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(x, y, z));
    }

    @Test
    void putThenGetReturnsStoredSelection() {
        SelectionStorage storage = new SelectionStorage();
        UUID id = UUID.randomUUID();
        storage.putSelection(id, region(1, 1, 1));
        assertNotNull(storage.getSelection(id));
    }

    @Test
    void getSelectionReturnsNullWhenAbsent() {
        SelectionStorage storage = new SelectionStorage();
        assertNull(storage.getSelection(UUID.randomUUID()));
    }

    @Test
    void getSelectionsReturnsIndependentCopy() {
        SelectionStorage storage = new SelectionStorage();
        UUID id = UUID.randomUUID();
        storage.putSelection(id, region(1, 1, 1));

        Map<UUID, CuboidRegion> snapshot = storage.getSelections();
        snapshot.clear();

        assertNotNull(storage.getSelection(id), "外部マップの変更は内部に影響しない");
    }

    @Test
    void clearByUuidRemovesOnlyThatEntry() {
        SelectionStorage storage = new SelectionStorage();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        storage.putSelection(a, region(1, 1, 1));
        storage.putSelection(b, region(2, 2, 2));

        storage.clear(a);

        assertNull(storage.getSelection(a));
        assertNotNull(storage.getSelection(b));
    }

    @Test
    void clearRemovesAll() {
        SelectionStorage storage = new SelectionStorage();
        storage.putSelection(UUID.randomUUID(), region(1, 1, 1));
        storage.clear();
        assertTrue(storage.getSelections().isEmpty());
    }
}
