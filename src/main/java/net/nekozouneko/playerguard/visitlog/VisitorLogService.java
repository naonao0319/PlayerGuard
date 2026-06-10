package net.nekozouneko.playerguard.visitlog;

import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VisitorLogService {

    private static final int MAX_ENTRIES_PER_REGION = 100;

    private final File file;
    private final Map<String, Deque<String>> logsByRegionKey = new HashMap<>();
    private final Map<UUID, String> lastVisitorRegionKeyByPlayer = new HashMap<>();
    private boolean dirty;

    public VisitorLogService(PlayerGuard plugin) {
        this.file = new File(plugin.getDataFolder(), "visitor-log.yml");
        load();
    }

    public synchronized void trackRegion(UUID player, String worldName, String regionId, boolean isVisitor) {
        String next = (isVisitor && regionId != null) ? worldName + ":" + regionId : null;
        String prev = lastVisitorRegionKeyByPlayer.put(player, next);
        if (Objects.equals(prev, next)) return;

        if (prev != null) {
            String[] p = splitKey(prev);
            append(p[0], p[1], new VisitorLogEntry(System.currentTimeMillis(),
                    VisitorLogType.EXIT, player, ""));
        }
        if (next != null) {
            append(worldName, regionId, new VisitorLogEntry(System.currentTimeMillis(),
                    VisitorLogType.ENTER, player, ""));
        }
    }

    public synchronized void forget(UUID player) {
        lastVisitorRegionKeyByPlayer.remove(player);
    }

    public synchronized void logAction(String worldName, String regionId, VisitorLogType type, UUID player, String detail) {
        if (worldName == null || regionId == null || type == null || player == null) return;
        append(worldName, regionId, new VisitorLogEntry(System.currentTimeMillis(), type, player, detail));
    }

    public synchronized List<VisitorLogEntry> getEntries(String worldName, String regionId) {
        if (worldName == null || regionId == null) return Collections.emptyList();
        Deque<String> deque = logsByRegionKey.get(worldName + ":" + regionId);
        if (deque == null || deque.isEmpty()) return Collections.emptyList();
        List<VisitorLogEntry> list = new ArrayList<>();
        for (String raw : deque) {
            VisitorLogEntry e = VisitorLogEntry.decode(raw);
            if (e != null) list.add(e);
        }
        Collections.reverse(list);
        return list;
    }

    public synchronized void clearByRegionId(String regionId) {
        if (regionId == null) return;
        Iterator<Map.Entry<String, Deque<String>>> it = logsByRegionKey.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, Deque<String>> e = it.next();
            String[] k = splitKey(e.getKey());
            if (regionId.equals(k[1])) {
                it.remove();
                changed = true;
            }
        }
        if (changed) dirty = true;
    }

    public synchronized boolean flushIfDirty() {
        if (!dirty) return false;
        save();
        return true;
    }

    public synchronized void save() {
        YamlConfiguration yc = new YamlConfiguration();
        for (Map.Entry<String, Deque<String>> e : logsByRegionKey.entrySet()) {
            String[] k = splitKey(e.getKey());
            yc.set("worlds." + k[0] + "." + k[1], new ArrayList<>(e.getValue()));
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yc.save(file);
            dirty = false;
        } catch (IOException ignored) {}
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worlds = yc.getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String world : worlds.getKeys(false)) {
            ConfigurationSection regions = worlds.getConfigurationSection(world);
            if (regions == null) continue;
            for (String regionId : regions.getKeys(false)) {
                List<String> list = yc.getStringList("worlds." + world + "." + regionId);
                if (list == null || list.isEmpty()) continue;
                Deque<String> dq = new ArrayDeque<>();
                for (String raw : list) {
                    if (raw == null) continue;
                    dq.addLast(raw);
                }
                if (!dq.isEmpty()) logsByRegionKey.put(world + ":" + regionId, dq);
            }
        }
    }

    private void append(String worldName, String regionId, VisitorLogEntry entry) {
        String key = worldName + ":" + regionId;
        Deque<String> dq = logsByRegionKey.get(key);
        if (dq == null) {
            dq = new ArrayDeque<>();
            logsByRegionKey.put(key, dq);
        }
        dq.addLast(entry.encode());
        while (dq.size() > MAX_ENTRIES_PER_REGION) dq.removeFirst();
        dirty = true;
    }

    private static String[] splitKey(String key) {
        int idx = key.indexOf(':');
        if (idx <= 0) return new String[] { "", key };
        return new String[] { key.substring(0, idx), key.substring(idx + 1) };
    }
}
