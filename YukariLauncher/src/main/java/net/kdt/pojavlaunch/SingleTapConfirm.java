package net.kdt.pojavlaunch;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * シングルタップを検出するジェスチャーリスナー。
 */
public class SingleTapConfirm extends SimpleOnGestureListener {
    /**
     * シングルタップアップイベントを検出します。
     */
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return true;
    }
}
