package net.kdt.pojavlaunch.customcontrols.mouse;

import static org.lwjgl.glfw.CallbackBridge.sendMouseButton;

import android.os.Handler;

import com.arata.yukarilauncher.setting.AllStaticSettings;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.MathUtils;

import org.lwjgl.glfw.CallbackBridge;

public class LeftClickGesture extends ValidatorGesture {
    public static final int FINGER_STILL_THRESHOLD = (int) Tools.dpToPx(9);
    private float mGestureStartX, mGestureStartY, mGestureEndX, mGestureEndY;
    private boolean mMouseActivated;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public LeftClickGesture(Handler handler) {
        super(handler);
    }
/**
 * 「input Event」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public final void inputEvent() {
        if(submit()) {
            mGestureStartX = mGestureEndX = CallbackBridge.mouseX;
            mGestureStartY = mGestureEndY = CallbackBridge.mouseY;
        }
    }
/**
 * 「GestureDelay」の値を取得します。
 */
    @Override
    protected int getGestureDelay() {
        return AllStaticSettings.timeLongPressTrigger;
    }
/**
 * 「check And Trigger」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public boolean checkAndTrigger() {
        boolean fingerStill = LeftClickGesture.isFingerStill(mGestureStartX, mGestureStartY, mGestureEndX, mGestureEndY, FINGER_STILL_THRESHOLD);
        // If the finger is still, fire the gesture.
        if(fingerStill) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true);
            mMouseActivated = true;
        }
        // Otherwise, don't click but still keep it active
        return true;
    }
/**
 * 「on Gesture Cancelled」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onGestureCancelled(boolean isSwitching) {
        if(mMouseActivated) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false);
            mMouseActivated = false;
        }
    }
/**
 * 「Motion」の値を設定します。
 */
    public void setMotion(float deltaX, float deltaY) {
        mGestureEndX += deltaX;
        mGestureEndY += deltaY;
    }

    /**
     * CallbackBridgeのmouseX/mouseYと比較して指が静止しているか確認します。
     * @param startX ジェスチャーの開始X座標
     * @param startY ジェスチャーの開始Y座標
     * @return 指の位置が「静止」とみなされるかどうか
     */
    public static boolean isFingerStill(float startX, float startY, float threshold) {
        return MathUtils.dist(
                CallbackBridge.mouseX,
                CallbackBridge.mouseY,
                startX,
                startY
        ) <= threshold;
    }
/**
 * このオブジェクトが「FingerStill」状態であるかを判定します。
 */
    public static boolean isFingerStill(float startX, float startY, float endX, float endY, float threshold) {
        return MathUtils.dist(
                endX,
                endY,
                startX,
                startY
        ) <= threshold;
    }
}
