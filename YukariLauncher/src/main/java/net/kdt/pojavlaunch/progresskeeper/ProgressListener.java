package net.kdt.pojavlaunch.progresskeeper;

/**
 * 進行状況の変更をリッスンするインターフェース。
 */
public interface ProgressListener {
    /** 進行状況が開始されたときに呼び出されます。 */
    void onProgressStarted();
    /** 進行状況が更新されたときに呼び出されます。 */
    void onProgressUpdated(int progress, int resid, Object... va);
    /** 進行状況が終了したときに呼び出されます。 */
    void onProgressEnded();
}
