package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;
import android.view.View;

public interface TouchEventProcessor {
/**
 * 「process Touch Event」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    boolean processTouchEvent(MotionEvent motionEvent);
/**
 * 「cancel Pending Actions」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    void cancelPendingActions();
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */
    default void dispatchTouchEvent(MotionEvent event, View view) {}
}
