package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Nullable;

import com.arata.anim.AnimPlayer;
import com.arata.anim.animations.Animations;
import com.arata.yukarilauncher.event.single.MCOptionChangeEvent;
import com.arata.yukarilauncher.event.single.RefreshHotbarEvent;
import com.arata.yukarilauncher.event.value.HotbarChangeEvent;
import com.arata.yukarilauncher.feature.MCOptions;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.setting.AllStaticSettings;
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarType;
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarUtils;

import net.kdt.pojavlaunch.GrabListener;
import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.MathUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.lwjgl.glfw.CallbackBridge;

public class HotbarView extends View implements View.OnLayoutChangeListener, Runnable {
    private final TapDetector mDoubleTapDetector = new TapDetector(2, TapDetector.DETECTION_METHOD_DOWN);
    private View mParentView;
    private static final int[] HOTBAR_KEYS = {
            LwjglGlfwKeycode.GLFW_KEY_1, LwjglGlfwKeycode.GLFW_KEY_2,   LwjglGlfwKeycode.GLFW_KEY_3,
            LwjglGlfwKeycode.GLFW_KEY_4, LwjglGlfwKeycode.GLFW_KEY_5,   LwjglGlfwKeycode.GLFW_KEY_6,
            LwjglGlfwKeycode.GLFW_KEY_7, LwjglGlfwKeycode.GLFW_KEY_8, LwjglGlfwKeycode.GLFW_KEY_9};
    private final DropGesture mDropGesture = new DropGesture(new Handler(Looper.getMainLooper()));
    private final GrabListener mGrabListener = new GrabListener() {
/**
 * 「on Grab State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
        @Override
        public void onGrabState(boolean isGrabbing) {
            mLastIndex = -1;
            mDropGesture.cancel();
        }
    };

    private int mWidth;
    private int mLastIndex = -1;
    private int mGuiScale;

    //调整判定框宽高时，用这个动画播放器播放一个淡化动画，来给用户一个当前判定框范围的反馈
    private final AnimPlayer adjustAnimPlayer = new AnimPlayer();
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public HotbarView(Context context) {
        super(context);
        init();
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public HotbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */

    @SuppressWarnings("unused") // You suggested me this constructor, Android
    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
/**
 * 「init」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    private void init() {
        setAlpha(0);
        setBackgroundColor(Color.parseColor("#80E64242"));
        adjustAnimPlayer.duration(800);
        adjustAnimPlayer.apply(new AnimPlayer.Entry(this, Animations.FadeOut));
    }
/**
 * 「on Attached To Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        ViewParent parent = getParent();
        if(parent == null) return;
        if(parent instanceof View) {
            mParentView = (View) parent;
            mParentView.addOnLayoutChangeListener(this);
        }
        adaptiveReset();
        CallbackBridge.addGrabListener(mGrabListener);
    }
/**
 * 「on Detached From Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CallbackBridge.removeGrabListener(mGrabListener);
        EventBus.getDefault().unregister(this);
    }

    /**
     * Hotbar更新イベントを受信したときに判定領域を更新します。
     * @param event 更新イベント
     */
    @Subscribe
    public void event(RefreshHotbarEvent event) {
        post(this);
    }

    /**
     * options.txtファイルが変更されたときに判定領域を更新します。
     * GUIサイズを確認する必要があります。
     * @param event 更新イベント
     */
    @Subscribe
    public void event(MCOptionChangeEvent event) {
        mGuiScale = MCOptions.INSTANCE.getMcScale();
        post(this);
    }

    /**
     * 判定枠の幅と高さを手動で調整するイベントを監視します。
     * @param event 変更イベント
     */
    @Subscribe
    public void event(HotbarChangeEvent event) {
        manualReset(event.getWidth(), event.getHeight(), true);
    }

    /**
     * マージンレイアウトパラメータを取得します。
     */
    private ViewGroup.MarginLayoutParams getMarginLayoutParams() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams))
            throw new RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams");
        return (ViewGroup.MarginLayoutParams) layoutParams;
    }
/**
 * 「adaptive Reset」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void adaptiveReset() {
        ViewGroup.MarginLayoutParams marginLayoutParams = getMarginLayoutParams();
        int height;
        marginLayoutParams.width = mWidth = mcScale(180);
        marginLayoutParams.height = height = mcScale(20);
        marginLayoutParams.leftMargin = (CallbackBridge.physicalWidth / 2) - (mWidth / 2);
        marginLayoutParams.topMargin = CallbackBridge.physicalHeight - height;
        setLayoutParams(marginLayoutParams);
    }
/**
 * 「manual Reset」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void manualReset(int width, int height, boolean playAnim) {
        ViewGroup.MarginLayoutParams marginLayoutParams = getMarginLayoutParams();
        marginLayoutParams.width = mWidth = width;
        marginLayoutParams.height = height;
        marginLayoutParams.leftMargin = Tools.currentDisplayMetrics.widthPixels / 2 - width / 2;
        marginLayoutParams.topMargin = Tools.currentDisplayMetrics.heightPixels - height;
        setLayoutParams(marginLayoutParams);
        if (playAnim) adjustAnimPlayer.start();
    }
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */

    @SuppressWarnings("ClickableViewAccessibility") // performClick does not report coordinates.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!CallbackBridge.isGrabbing()) return false;
        boolean hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event);

        // Check if we need to cancel the drop event
        int actionMasked = event.getActionMasked();
        if(isLastEventInGesture(actionMasked)) mDropGesture.cancel();
        else mDropGesture.submit();
        // Determine the hotbar slot
        float x = event.getX();
        // Ignore positions equal to mWidth because they would translate into an out-of-bounds hotbar index
        if(x < 0 || x >= mWidth) {
            // If out of bounds, cancel the hotbar gesture to avoid dropping items on last hotbar slots
            mDropGesture.cancel();
            return true;
        }
        int hotbarIndex = (int)MathUtils.map(x, 0, mWidth, 0, HOTBAR_KEYS.length);
        // Check if the slot changed and we need to make a key press
        if(hotbarIndex == mLastIndex) {
            // Only check for doubletapping if the slot has not changed
            if (hasDoubleTapped && !AllStaticSettings.disableDoubleTap) CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F);
            return true;
        }
        mLastIndex = hotbarIndex;
        int hotbarKey = HOTBAR_KEYS[hotbarIndex];
        CallbackBridge.sendKeyPress(hotbarKey);
        // Cancel the event since we changed hotbar slots.
        mDropGesture.cancel();
        // Only resubmit the gesture only if it isn't the last event we will receive.
        if(!isLastEventInGesture(actionMasked)) mDropGesture.submit();
        return true;
    }
/**
 * このオブジェクトが「LastEventInGesture」状態であるかを判定します。
 */
    private boolean isLastEventInGesture(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL;
    }
/**
 * 「mc Scale」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private int mcScale(int input) {
        return (int)((mGuiScale * input)/ AllStaticSettings.scaleFactor);
    }
/**
 * 「run」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    @Override
    public void run() {
        if (getParent() == null) return;

        HotbarType hotbarType = HotbarUtils.getCurrentType();
        if (hotbarType == HotbarType.AUTO) {
            adaptiveReset();
        } else {
            manualReset(AllSettings.getHotbarWidth().getValue().getValue(), AllSettings.getHotbarHeight().getValue().getValue(), false);
        }
    }
/**
 * 「on Layout Change」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // We need to check whether dimensions match or not because here we are looking specifically for changes of dimensions
        // and Android keeps calling this without dimensions actually changing for some reason.
        if(v.equals(mParentView) && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            // Need to post this, because it is not correct to resize the view
            // during a layout pass.
            post(this::adaptiveReset);
        }
    }
}
