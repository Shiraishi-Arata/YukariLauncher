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
 * Class dealing with the virtual mouse
 */
public class Touchpad extends View implements GrabListener, AbstractTouchpad {
    /* Whether the Touchpad should be displayed */
    private boolean mDisplayState;
    /* Mouse pointer icon used by the touchpad */
    private Drawable mMousePointerDrawable;
    private int mLastCursorType = Integer.MIN_VALUE;
    private float mMouseX, mMouseY;

    public Touchpad(@NonNull Context context) {
        this(context, null);
    }

    public Touchpad(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /** Enable the touchpad */
    private void _enable(){
        setVisibility(VISIBLE);
        placeMouseAt(currentDisplayMetrics.widthPixels / 2f, currentDisplayMetrics.heightPixels / 2f);
    }

    /** Disable the touchpad and hides the mouse */
    private void _disable(){
        setVisibility(GONE);
    }

    /** @return The new state, enabled or disabled */
    public boolean switchState(){
        mDisplayState = !mDisplayState;
        if(!CallbackBridge.isGrabbing()) {
            if(mDisplayState) _enable();
            else _disable();
        }
        return mDisplayState;
    }

    public void placeMouseAt(float x, float y) {
        mMouseX = x;
        mMouseY = y;
        updateMousePosition();
    }

    private void sendMousePosition() {
        CallbackBridge.sendCursorPos((mMouseX * AllStaticSettings.scaleFactor), (mMouseY * AllStaticSettings.scaleFactor));
    }

    private void updateMousePosition() {
        sendMousePosition();
        // I wanted to implement a dirty rect for this, but it is ignored since API level 21
        // (which is our min API)
        // Let's hope the "internally calculated area" is good enough.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cursorType = CallbackBridge.getCurrentCursorType();
        if (cursorType != mLastCursorType) {
            updateMouseDrawable();
        }
        canvas.translate(mMouseX, mMouseY);
        mMousePointerDrawable.draw(canvas);
    }

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

    public void updateMouseScale() {
        Dimension mousescale = ImageUtils.resizeWithRatio(mMousePointerDrawable.getIntrinsicWidth(), mMousePointerDrawable.getIntrinsicHeight(),
                AllSettings.getMouseScale().getValue());
        int scaledWidth = (int) (mousescale.width * 0.5);
        int scaledHeight = (int) (mousescale.height * 0.5);
        int[] hotspot = CursorDrawableUtils.getScaledHotspot(mMousePointerDrawable, scaledWidth, scaledHeight);
        mMousePointerDrawable.setBounds(-hotspot[0], -hotspot[1], scaledWidth - hotspot[0], scaledHeight - hotspot[1]);
    }

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

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mMousePointerDrawable || super.verifyDrawable(who);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (drawable == mMousePointerDrawable) {
            invalidate();
            return;
        }
        super.invalidateDrawable(drawable);
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        post(()->updateGrabState(isGrabbing));
    }
    private void updateGrabState(boolean isGrabbing) {
        if(!isGrabbing) {
            if(mDisplayState && getVisibility() != VISIBLE) _enable();
            if(!mDisplayState && getVisibility() == VISIBLE) _disable();
        }else{
            if(getVisibility() != View.GONE) _disable();
        }
    }

    @Override
    public boolean getDisplayState() {
        return mDisplayState;
    }

    @Override
    public void applyMotionVector(float x, float y) {
        mMouseX = Math.max(0, Math.min(currentDisplayMetrics.widthPixels, mMouseX + x * (AllSettings.getMouseSpeed().getValue() / 100f)));
        mMouseY = Math.max(0, Math.min(currentDisplayMetrics.heightPixels, mMouseY + y * (AllSettings.getMouseSpeed().getValue() / 100f)));
        updateMousePosition();
    }

    @Override
    public void enable(boolean supposed) {
        if(mDisplayState) return;
        mDisplayState = true;
        if(supposed && CallbackBridge.isGrabbing()) return;
        _enable();
    }

    @Override
    public void disable() {
        if(!mDisplayState) return;
        mDisplayState = false;
        _disable();
    }
}
