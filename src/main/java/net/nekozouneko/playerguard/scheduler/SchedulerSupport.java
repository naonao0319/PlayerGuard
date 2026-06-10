package net.nekozouneko.playerguard.scheduler;

/**
 * 実行中サーバーが Folia 由来のリージョンスケジューラ API を提供するかを判定するヘルパ。
 * paper-api の {@code EntityScheduler} はサーバー実装が Paper / Folia の場合のみ
 * ランタイムに存在し、純粋な Spigot サーバーには存在しない。
 */
public final class SchedulerSupport {

    private SchedulerSupport() {}

    private static final boolean FOLIA_API_PRESENT =
            isClassPresent("io.papermc.paper.threadedregions.scheduler.EntityScheduler");

    public static boolean isFoliaApiPresent() {
        return FOLIA_API_PRESENT;
    }

    static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
