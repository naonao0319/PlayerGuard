package net.nekozouneko.playerguard.scheduler;

/** キャンセル可能なスケジュール済みタスクのハンドル。 */
public interface PGTask {
    void cancel();
}
