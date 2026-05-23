package net.kdt.pojavlaunch.customcontrols.mouse;

import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.setting.AllStaticSettings;
import com.arata.yukarilauncher.utils.YLTools;
import com.arata.yukarilauncher.utils.image.Dimension;
import com.arata.yukarilauncher.utils.image.ImageUtils;
import com.arata.yukarilauncher.utils.mouse.CursorDrawableUtils;

import net.kdt.pojavlaunch.GrabListener;

import org.lwjgl.glfw.CallbackBridge;

/**
 * 仮想マウスを扱うクラス
 */
public class Touchpad extends View implements GrabListener, AbstractTouchpad {
    /* Whether the Touchpad should be displayed */
    private boolean mDisplayState;
    /* Mouse pointer icon used by the touchpad */
    private Drawable mMousePointerDrawable;
    private int mLastCursorType = Integer.MIN_VALUE;
    private float mMouseX, mMouseY;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public Touchpad(@NonNull Context context) {
        this(context, null);
    }
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public Touchpad(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * タッチパッドを有効にします。
     */
    private void _enable(){
        setVisibility(VISIBLE);
        placeMouseAt(currentDisplayMetrics.widthPixels / 2f, currentDisplayMetrics.heightPixels / 2f);
    }

    /**
     * タッチパッドを無効にしてマウスを非表示にします。
     */
    private void _disable(){
        setVisibility(GONE);
    }

    /** @return 新しい状態（有効または無効） */
    public boolean switchState(){
        mDisplayState = !mDisplayState;
        if(!CallbackBridge.isGrabbing()) {
            if(mDisplayState) _enable();
/**
 * 「_disable」メソッド。
 * このクラスに定義された機能メソッドです。
 */
            else _disable();
        }
        return mDisplayState;
    }
/**
 * 「place Mouse At」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void placeMouseAt(float x, float y) {
        mMouseX = x;
        mMouseY = y;
        updateMousePosition();
    }
/**
 * 「send Mouse Position」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void sendMousePosition() {
        CallbackBridge.sendCursorPos((mMouseX * AllStaticSettings.scaleFactor), (mMouseY * AllStaticSettings.scaleFactor));
    }
/**
 * 「update Mouse Position」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void updateMousePosition() {
        sendMousePosition();
        // I wanted to implement a dirty rect for this, but it is ignored since API level 21
        // (which is our min API)
        // Let's hope the "internally calculated area" is good enough.
        invalidate();
    }
/**
 * Viewの描画処理を行います。
 * このメソッドはシステムによって自動的に呼び出されます。
 */
    @Override
    protected void onDraw(Canvas canvas) {
        int cursorType = CallbackBridge.getCurrentCursorType();
        if (cursorType != mLastCursorType) {
            updateMouseDrawable();
        }
        canvas.translate(mMouseX, mMouseY);
        mMousePointerDrawable.draw(canvas);
    }
/**
 * 「init」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    private void init(){
        // Setup mouse pointer
        updateMouseDrawable();
        // For some reason it's annotated as Nullable even though it doesn't seem to actually
        // ever return null
        assert mMousePointerDrawable != null;

        updateMouseScale();

        setFocusable(false);
        setDefaultFocusHighlightEnabled(false);

        // When the game is grabbing, we should not display the mouse
        disable();
        mDisplayState = false;
    }
/**
 * 「update Mouse Scale」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void updateMouseScale() {
        Dimension mousescale = ImageUtils.resizeWithRatio(mMousePointerDrawable.getIntrinsicWidth(), mMousePointerDrawable.getIntrinsicHeight(),
                AllSettings.getMouseScale().getValue());
        int scaledWidth = (int) (mousescale.width * 0.5);
        int scaledHeight = (int) (mousescale.height * 0.5);
        int[] hotspot = CursorDrawableUtils.getScaledHotspot(mMousePointerDrawable, scaledWidth, scaledHeight);
        mMousePointerDrawable.setBounds(-hotspot[0], -hotspot[1], scaledWidth - hotspot[0], scaledHeight - hotspot[1]);
    }
/**
 * 「update Mouse Drawable」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void updateMouseDrawable() {
        mLastCursorType = CallbackBridge.getCurrentCursorType();
        mMousePointerDrawable = YLTools.customMouse(getContext());
        mMousePointerDrawable.setCallback(this);
        mMousePointerDrawable.setVisible(true, true);
        if (mMousePointerDrawable instanceof Animatable) {
            ((Animatable) mMousePointerDrawable).start();
        }
        updateMouseScale();
    }
/**
 * 「verify Drawable」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mMousePointerDrawable || super.verifyDrawable(who);
    }
/**
 * 「invalidate Drawable」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (drawable == mMousePointerDrawable) {
            invalidate();
            return;
        }
        super.invalidateDrawable(drawable);
    }
/**
 * 「on Grab State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onGrabState(boolean isGrabbing) {
        post(()->updateGrabState(isGrabbing));
    }
/**
 * 「update Grab State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void updateGrabState(boolean isGrabbing) {
        if(!isGrabbing) {
            if(mDisplayState && getVisibility() != VISIBLE) _enable();
            if(!mDisplayState && getVisibility() == VISIBLE) _disable();
        }else{
            if(getVisibility() != View.GONE) _disable();
        }
    }
/**
 * 「DisplayState」の値を取得します。
 */
    @Override
    public boolean getDisplayState() {
        return mDisplayState;
    }
/**
 * 「apply Motion Vector」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void applyMotionVector(float x, float y) {
        mMouseX = Math.max(0, Math.min(currentDisplayMetrics.widthPixels, mMouseX + x * (AllSettings.getMouseSpeed().getValue() / 100f)));
        mMouseY = Math.max(0, Math.min(currentDisplayMetrics.heightPixels, mMouseY + y * (AllSettings.getMouseSpeed().getValue() / 100f)));
        updateMousePosition();
    }
/**
 * 「enable」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    @Override
    public void enable(boolean supposed) {
        if(mDisplayState) return;
        mDisplayState = true;
        if(supposed && CallbackBridge.isGrabbing()) return;
        _enable();
    }
/**
 * 「disable」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    @Override
    public void disable() {
        if(!mDisplayState) return;
        mDisplayState = false;
        _disable();
    }
}
