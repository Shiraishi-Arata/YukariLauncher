package net.kdt.pojavlaunch.progresskeeper;

/**
 * タスク数の変更をリッスンするインターフェース。
 */
public interface TaskCountListener {
    /**
     * タスク数が更新されたときに呼び出されます。
     * @param taskCount 現在のタスク数
     */
    void onUpdateTaskCount(int taskCount);
}
