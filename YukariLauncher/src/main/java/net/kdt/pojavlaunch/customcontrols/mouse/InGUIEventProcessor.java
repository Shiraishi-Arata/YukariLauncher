package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;

import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.setting.AllStaticSettings;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;

import org.lwjgl.glfw.CallbackBridge;

public class InGUIEventProcessor implements TouchEventProcessor {
    public static final float FINGER_SCROLL_THRESHOLD = Tools.dpToPx(6);
    public static final float FINGER_STILL_THRESHOLD = Tools.dpToPx(5);

    private final PointerTracker mTracker = new PointerTracker();
    private final TapDetector mSingleTapDetector;
    private AbstractTouchpad mTouchpad;
    private boolean mIsMouseDown = false;
    private float mStartX, mStartY;
    private final Scroller mScroller = new Scroller(FINGER_SCROLL_THRESHOLD);
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public InGUIEventProcessor() {
        mSingleTapDetector = new TapDetector(1, TapDetector.DETECTION_METHOD_BOTH);
    }
/**
 * 「process Touch Event」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public boolean processTouchEvent(MotionEvent motionEvent) {
        boolean singleTap = mSingleTapDetector.onTouchEvent(motionEvent);

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTracker.startTracking(motionEvent);
                if(!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.getX(), motionEvent.getY());

                    // disabled gestures means no scrolling possible, send gesture early
                    if (AllSettings.getDisableGestures().getValue()) enableMouse();
/**
 * 「GestureStart」の値を設定します。
 */
                    else setGestureStart(motionEvent);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pointerCount = motionEvent.getPointerCount();
                int pointerIndex = mTracker.trackEvent(motionEvent);
                if(pointerCount == 1 || AllSettings.getDisableGestures().getValue()) {
                    if(touchpadDisplayed()) {
                        mTouchpad.applyMotionVector(mTracker.getMotionVector());
                    } else {
                        float mainPointerX = motionEvent.getX(pointerIndex);
                        float mainPointerY = motionEvent.getY(pointerIndex);
                        sendTouchCoordinates(mainPointerX, mainPointerY);

                        if(!mIsMouseDown) {
                            if(!hasGestureStarted()) setGestureStart(motionEvent);
                            if(!LeftClickGesture.isFingerStill(mStartX, mStartY, FINGER_STILL_THRESHOLD))
                                enableMouse();
                        }

                    }
                } else mScroller.performScroll(mTracker.getMotionVector());
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mScroller.resetScrollOvershoot();
                mTracker.cancelTracking();

                // Handle single tap on gestures
                if((!AllSettings.getDisableGestures().getValue() || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, CallbackBridge.mouseX, CallbackBridge.mouseY);
                }

                if(mIsMouseDown) disableMouse();
                resetGesture();
        }


        return true;
    }
/**
 * 「touchpad Displayed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private boolean touchpadDisplayed() {
        return mTouchpad != null && mTouchpad.getDisplayState();
    }
/**
 * 「AbstractTouchpad」の値を設定します。
 */
    public void setAbstractTouchpad(AbstractTouchpad touchpad) {
        mTouchpad = touchpad;
    }
/**
 * 「send Touch Coordinates」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void sendTouchCoordinates(float x, float y) {
        CallbackBridge.sendCursorPos( x * AllStaticSettings.scaleFactor, y * AllStaticSettings.scaleFactor);
    }
/**
 * 「enable Mouse」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void enableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true);
        mIsMouseDown = true;
    }
/**
 * 「disable Mouse」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void disableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false);
        mIsMouseDown = false;
    }
/**
 * 「GestureStart」の値を設定します。
 */
    private void setGestureStart(MotionEvent event) {
        mStartX = event.getX() * AllStaticSettings.scaleFactor;
        mStartY = event.getY() * AllStaticSettings.scaleFactor;
    }
/**
 * 「reset Gesture」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void resetGesture() {
        mStartX = mStartY = -1;
    }
/**
 * 「GestureStarted」を持っているかを確認します。
 */
    private boolean hasGestureStarted() {
        return mStartX != -1 || mStartY != -1;
    }
/**
 * 「cancel Pending Actions」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void cancelPendingActions() {
        mScroller.resetScrollOvershoot();
        disableMouse();
    }
}
