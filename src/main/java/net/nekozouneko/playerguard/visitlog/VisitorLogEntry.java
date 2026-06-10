package net.nekozouneko.playerguard.visitlog;

import java.util.UUID;

public class VisitorLogEntry {

    private final long at;
    private final VisitorLogType type;
    private final UUID player;
    private final String detail;

    public VisitorLogEntry(long at, VisitorLogType type, UUID player, String detail) {
        this.at = at;
        this.type = type;
        this.player = player;
        this.detail = detail;
    }

    public long getAt() {
        return at;
    }

    public VisitorLogType getType() {
        return type;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getDetail() {
        return detail;
    }

    public String encode() {
        return at + "\t" + type.name() + "\t" + player + "\t" + safe(detail);
    }

    public static VisitorLogEntry decode(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("\t", 4);
        if (parts.length < 4) return null;
        try {
            long at = Long.parseLong(parts[0]);
            VisitorLogType type = VisitorLogType.valueOf(parts[1]);
            UUID player = UUID.fromString(parts[2]);
            String detail = parts[3];
            return new VisitorLogEntry(at, type, player, detail);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace('\t', ' ');
    }
}
