package net.nekozouneko.playerguard.selection;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.regions.CuboidRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionStorage {

    private final Map<UUID, CuboidRegion> selection = new ConcurrentHashMap<>();

    public void clear() {
        selection.clear();
    }

    public void clear(UUID uuid) {
        selection.remove(uuid);
    }

    public void putSelection(UUID uuid, CuboidRegion cr) {
        Preconditions.checkArgument(cr != null);
        selection.put(uuid, cr.clone());
    }

    public CuboidRegion getSelection(UUID uuid) {
        return selection.get(uuid);
    }

    public Map<UUID, CuboidRegion> getSelections() {
        return new HashMap<>(selection);
    }
}
