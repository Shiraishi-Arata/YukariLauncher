package net.kdt.pojavlaunch.progresskeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 進行状況を管理し、リスナーに通知するクラス。
 * 複数のタスクの進行状況を追跡し、タスク数の変更をリスナーに通知します。
 */
public class ProgressKeeper {
    private static final HashMap<String, List<ProgressListener>> sProgressListeners = new HashMap<>();
    private static final HashMap<String, ProgressState> sProgressStates = new HashMap<>();
    private static final List<TaskCountListener> sTaskCountListeners = new ArrayList<>();

    /**
     * 進行状況を送信します。進行状況が-1、residが-1の場合は終了を示します。
     */
    public static synchronized void submitProgress(String progressRecord, int progress, int resid, Object... va) {
        ProgressState progressState = sProgressStates.get(progressRecord);
        boolean shouldCallStarted = progressState == null;
        boolean shouldCallEnded = resid == -1 && progress == -1;
        if(shouldCallEnded) {
            shouldCallStarted = false;
            sProgressStates.remove(progressRecord);
            updateTaskCount();
        }else if(shouldCallStarted){
            sProgressStates.put(progressRecord, (progressState = new ProgressState()));
            updateTaskCount();
        }
        if(progressState != null) {
            progressState.progress = progress;
            progressState.resid = resid;
            progressState.varArg = va;
        }

        List<ProgressListener> progressListeners = sProgressListeners.get(progressRecord);
        if(progressListeners != null)
            for(ProgressListener listener : progressListeners) {
                    if(shouldCallStarted) listener.onProgressStarted();
                    else if(shouldCallEnded) listener.onProgressEnded();
                    else listener.onProgressUpdated(progress, resid, va);
            }
    }

    /**
     * タスク数を更新し、リスナーに通知します。
     */
    private static synchronized void updateTaskCount() {
        int count = sProgressStates.size();
        for(TaskCountListener listener : sTaskCountListeners) {
            listener.onUpdateTaskCount(count);
        }
    }

    /**
     * 特定の進行状況レコードにリスナーを追加します。
     */
    public static synchronized void addListener(String progressRecord, ProgressListener listener) {
        ProgressState state = sProgressStates.get(progressRecord);
        if(state != null && (state.resid != -1 || state.progress != -1)) {
            listener.onProgressStarted();
            listener.onProgressUpdated(state.progress, state.resid, state.varArg);
        }else{
            listener.onProgressEnded();
        }
        List<ProgressListener> listenerWeakReferenceList = sProgressListeners.computeIfAbsent(progressRecord, k -> new ArrayList<>());
        listenerWeakReferenceList.add(listener);
    }

    /**
     * 特定の進行状況レコードからリスナーを削除します。
     */
    public static synchronized void removeListener(String progressRecord, ProgressListener listener) {
        List<ProgressListener> listenerWeakReferenceList = sProgressListeners.get(progressRecord);
        if(listenerWeakReferenceList != null) listenerWeakReferenceList.remove(listener);
    }

    /**
     * タスク数変更リスナーを追加します。
     */
    public static synchronized void addTaskCountListener(TaskCountListener listener) {
        listener.onUpdateTaskCount(sProgressStates.size());
        if(!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener);
    }

    /**
     * タスク数変更リスナーを追加します（初回更新の制御付き）。
     */
    public static synchronized void addTaskCountListener(TaskCountListener listener, boolean runUpdate) {
        if(runUpdate) listener.onUpdateTaskCount(sProgressStates.size());
        if(!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener);
    }

    /**
     * タスク数変更リスナーを削除します。
     */
    public static synchronized void removeTaskCountListener(TaskCountListener listener) {
        sTaskCountListeners.remove(listener);
    }

    /**
     * @return 現在のタスクセット内に指定されたキーが存在するかどうか
     */
    public static boolean containsProgress(String progressKey) {
        return sProgressStates.containsKey(progressKey);
    }

    /**
     * すべてのタスクが完了するまで待機し、その後Runnableを実行します。
     * 保留中のプロセスがない場合も同様に実行します。
     * Runnableは最後にタスク数を更新したスレッドから実行されるため、長時間の処理は避けてください。
     * @param runnable タスクがなくなった時に実行するRunnable
     */
    public static void waitUntilDone(final Runnable runnable) {
        if(getTaskCount() == 0) {
            runnable.run();
            return;
        }
        TaskCountListener listener = new TaskCountListener() {
            @Override
            public void onUpdateTaskCount(int taskCount) {
                if(taskCount == 0) {
                    runnable.run();
                    removeTaskCountListener(this);
                }
            }
        };
        addTaskCountListener(listener);
    }

    /**
     * @return 現在のタスク数
     */
    public static synchronized int getTaskCount() {
        return sProgressStates.size();
    }

    /**
     * @return 実行中のタスクがあるかどうか
     */
    public static boolean hasOngoingTasks() {
        return getTaskCount() > 0;
    }

    /**
     * 進行状況レコードを保持する内部クラス。
     */
    public static class ProgressRecord {
        public final String key;
        public final int progress;
        public final int resid;
        public final Object[] varArg;

        public ProgressRecord(String key, int progress, int resid, Object[] varArg) {
            this.key = key;
            this.progress = progress;
            this.resid = resid;
            this.varArg = varArg;
        }
    }

    /**
     * @return 現在のすべての進行状況レコードのリスト
     */
    public static synchronized List<ProgressRecord> getProgressRecords() {
        if (sProgressStates.isEmpty()) return Collections.emptyList();
        List<ProgressRecord> records = new ArrayList<>();
        for (Map.Entry<String, ProgressState> entry : sProgressStates.entrySet()) {
            ProgressState state = entry.getValue();
            records.add(
                    new ProgressRecord(
                            entry.getKey(),
                            state.progress,
                            state.resid,
                            state.varArg == null ? new Object[0] : state.varArg.clone()
                    )
            );
        }
        return records;
    }
}
