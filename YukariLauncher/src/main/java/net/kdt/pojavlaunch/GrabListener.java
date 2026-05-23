package net.kdt.pojavlaunch;

/**
 * グラブ（カーソルキャプチャ）状態の変更を監視するためのリスナーインターフェース。
 */
public interface GrabListener {
    /**
     * グラブ状態が変更されたときに呼び出されます。
     * @param isGrabbing 現在グラブ中かどうか
     */
    void onGrabState(boolean isGrabbing);
}
