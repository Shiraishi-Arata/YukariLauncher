package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;

public class PointerTracker {
    private boolean mColdStart = true;
    private int mTrackedPointerId;
    private int mPointerCount;
    private float mLastX, mLastY;
    private final float[] mMotionVector = new float[2];
/**
 * 「start Tracking」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void startTracking(MotionEvent motionEvent) {
        mColdStart = false;
        mTrackedPointerId = motionEvent.getPointerId(0);
        mPointerCount = motionEvent.getPointerCount();
        mLastX = motionEvent.getX();
        mLastY = motionEvent.getY();
    }
/**
 * 「cancel Tracking」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void cancelTracking() {
        mColdStart = true;
    }
/**
 * 「track Event」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public int trackEvent(MotionEvent motionEvent) {
        int trackedPointerIndex = motionEvent.findPointerIndex(mTrackedPointerId);
        int pointerCount = motionEvent.getPointerCount();
        if(trackedPointerIndex == -1 || mPointerCount != pointerCount || mColdStart) {
            startTracking(motionEvent);
            trackedPointerIndex = 0;
        }
        float trackedX = motionEvent.getX(trackedPointerIndex);
        float trackedY = motionEvent.getY(trackedPointerIndex);
        mMotionVector[0] = trackedX - mLastX;
        mMotionVector[1] = trackedY - mLastY;
        mLastX = trackedX;
        mLastY = trackedY;
        return trackedPointerIndex;
    }
/**
 * 「MotionVector」の値を取得します。
 */
    public float[] getMotionVector() {
        return mMotionVector;
    }
}
