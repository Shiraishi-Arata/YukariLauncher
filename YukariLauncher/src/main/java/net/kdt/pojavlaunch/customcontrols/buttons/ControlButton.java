package net.kdt.pojavlaunch.customcontrols.buttons;

import static net.kdt.pojavlaunch.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN;
import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;
import static org.lwjgl.glfw.CallbackBridge.sendMouseButton;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import com.arata.yukarilauncher.ui.activity.MainActivity;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

import org.lwjgl.glfw.CallbackBridge;

@SuppressLint({"ViewConstructor", "AppCompatCustomView"})
public class ControlButton extends TextView implements ControlInterface {
    private final Paint mRectPaint = new Paint();
    protected ControlData mProperties;
    private final ControlLayout mControlLayout;

    /* Cache value from the ControlData radius for drawing purposes */
    private float mComputedRadius;

    protected boolean mIsToggled = false;
    protected boolean mIsPointerOutOfBounds = false;
    private final Handler mRepeatHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRepeatRunnable = new Runnable() {
/**
 * 「run」メソッド。
 * このクラスに定義された機能メソッドです。
 */
        @Override
        public void run() {
            if (!mProperties.repeatedlyEnabled) return;
            sendKeyPressesWithoutActivation(true);
            sendKeyPressesWithoutActivation(false);
            mRepeatHandler.postDelayed(this, getRepeatIntervalMs());
        }
    };
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public ControlButton(ControlLayout layout, ControlData properties) {
        super(layout.getContext());
        mControlLayout = layout;
        setGravity(Gravity.CENTER);
        setAllCaps(AllSettings.getButtonAllCaps().getValue());
        setTextColor(Color.WHITE);
        setPadding(4, 4, 4, 4);
        setTextSize(14); // Nullify the default size setting
        setOutlineProvider(null); // Disable shadow casting, removing one drawing pass

        //setOnLongClickListener(this);

        //ボタン作成時、幅/高さはスケーリングに合わせてまだ処理されていません。
        setProperties(preProcessProperties(properties, layout));

        injectBehaviors();
    }
/**
 * 「ControlView」の値を取得します。
 */
    @Override
    public View getControlView() {return this;}
/**
 * 「Properties」の値を取得します。
 */
    public ControlData getProperties() {
        return mProperties;
    }
/**
 * 「Properties」の値を設定します。
 */
    public void setProperties(ControlData properties, boolean changePos) {
        mProperties = properties;
        ControlInterface.super.setProperties(properties, changePos);
        mComputedRadius = ControlInterface.super.computeCornerRadius(mProperties.cornerRadius);

        if (mProperties.isToggle) {
            //トグルレイヤー用
            final TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);
            mRectPaint.setColor(value.data);
            mRectPaint.setAlpha(128);
        } else {
            mRectPaint.setColor(Color.WHITE);
            mRectPaint.setAlpha(60);
        }

        setText(properties.name);
    }
/**
 * Viewの描画処理を行います。
 * このメソッドはシステムによって自動的に呼び出されます。
 */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsToggled || (!mProperties.isToggle && isActivated()))
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), mComputedRadius, mComputedRadius, mRectPaint);
    }
/**
 * 「on Detached From Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    protected void onDetachedFromWindow() {
        stopButtonRepeat();
        super.onDetachedFromWindow();
    }
/**
 * 「load Edit Values」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void loadEditValues(EditControlPopup editControlPopup){
        editControlPopup.loadValues(getProperties());
    }

    /**
     * ControlButtonの別のインスタンスを親レイアウトに追加します。
     */
    public void cloneButton(){
        ControlData cloneData = new ControlData(getProperties());
        cloneData.dynamicX = "0.5 * ${screen_width}";
        cloneData.dynamicY = "0.5 * ${screen_height}";
        ((ControlLayout) getParent()).addControlButton(cloneData);
    }

    /**
     * レイアウトからこのボタンの痕跡をすべて削除します。
     */
    public void removeButton() {
        getControlLayoutParent().getLayout().mControlDataList.remove(getProperties());
        getControlLayoutParent().removeView(this);
    }
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                //マウスアクションとして扱われるようにイベントを送信
                if(getProperties().passThruEnabled && CallbackBridge.isGrabbing()){
                    View gameSurface = getControlLayoutParent().getGameSurface();
                    if(gameSurface != null) gameSurface.dispatchTouchEvent(event);
                }

                //範囲外の場合
                if(event.getX() < getControlView().getLeft() || event.getX() > getControlView().getRight() ||
                        event.getY() < getControlView().getTop()  || event.getY() > getControlView().getBottom()){
                    if(getProperties().isSwipeable && !mIsPointerOutOfBounds){
                        //キーを削除
                        if(!triggerToggle()) {
                            sendKeyPresses(false);
                        }
                    }
                    mIsPointerOutOfBounds = true;
                    getControlLayoutParent().onTouch(this, event);
                    break;
                }

                //現在範囲内にある場合
                if(mIsPointerOutOfBounds) {
                    getControlLayoutParent().onTouch(this, event);
                    //ボタンを再押下
                    if(getProperties().isSwipeable && !getProperties().isToggle){
                        sendKeyPresses(true);
                    }
                }
                mIsPointerOutOfBounds = false;
                break;

            case MotionEvent.ACTION_DOWN: // 0
            case MotionEvent.ACTION_POINTER_DOWN: // 5
                if(!getProperties().isToggle){
                    if (getProperties().repeatedlyEnabled) {
                        setActivated(true);
                        sendKeyPressesWithoutActivation(true);
                        sendKeyPressesWithoutActivation(false);
                        startButtonRepeat();
                    } else {
                        sendKeyPresses(true);
                    }
                }
                break;

            case MotionEvent.ACTION_UP: // 1
            case MotionEvent.ACTION_CANCEL: // 3
            case MotionEvent.ACTION_POINTER_UP: // 6
                if(getProperties().passThruEnabled){
                    View gameSurface = getControlLayoutParent().getGameSurface();
                    if(gameSurface != null) gameSurface.dispatchTouchEvent(event);
                }
                if(mIsPointerOutOfBounds) getControlLayoutParent().onTouch(this, event);
                mIsPointerOutOfBounds = false;

                if(!triggerToggle()) {
                    if (getProperties().repeatedlyEnabled) {
                        stopButtonRepeat();
                        setActivated(false);
                    } else {
                        sendKeyPresses(false);
                    }
                }
                break;

            default:
                return false;
        }

        return super.onTouchEvent(event);
    }
/**
 * 「trigger Toggle」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */



    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean triggerToggle(){
        //returns true a the toggle system is triggered
        if(mProperties.isToggle){
            mIsToggled = !mIsToggled;
            invalidate();
            sendKeyPresses(mIsToggled);
            return true;
        }
        return false;
    }
/**
 * 「send Key Presses」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void sendKeyPresses(boolean isDown){
        setActivated(isDown);
        sendKeyPressesWithoutActivation(isDown);
    }
/**
 * 「send Key Presses Without Activation」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void sendKeyPressesWithoutActivation(boolean isDown){
        for(int keycode : mProperties.keycodes){
            if(keycode >= GLFW_KEY_UNKNOWN){
                sendKeyPress(keycode, CallbackBridge.getCurrentMods(), isDown);
                CallbackBridge.setModifiers(keycode, isDown);
            }else{
                sendSpecialKey(keycode, isDown);
            }
        }
    }
/**
 * 「RepeatIntervalMs」の値を取得します。
 */

    private int getRepeatIntervalMs() {
        int cps = Math.max(1, mProperties.repeatCps);
        return Math.max(1, 1000 / cps);
    }
/**
 * 「start Button Repeat」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void startButtonRepeat() {
        stopButtonRepeat();
        mRepeatHandler.postDelayed(mRepeatRunnable, Math.max(0, mProperties.repeatLongPressDelayMs));
    }
/**
 * 「stop Button Repeat」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void stopButtonRepeat() {
        mRepeatHandler.removeCallbacks(mRepeatRunnable);
    }
/**
 * 「send Special Key」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    private void sendSpecialKey(int keycode, boolean isDown){
        switch (keycode) {
            case ControlData.SPECIALBTN_KEYBOARD:
                if(isDown) MainActivity.switchKeyboardState();
                break;

            case ControlData.SPECIALBTN_TOGGLECTRL:
                if(isDown) getControlLayoutParent().toggleControlVisible();
                break;

            case ControlData.SPECIALBTN_VIRTUALMOUSE:
                if(isDown) MainActivity.toggleMouse(getContext());
                break;

            case ControlData.SPECIALBTN_MOUSEPRI:
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSEMID:
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSESEC:
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown);
                break;

            case ControlData.SPECIALBTN_SCROLLDOWN:
                if (isDown) CallbackBridge.sendScroll(0, 1d);
                break;

            case ControlData.SPECIALBTN_SCROLLUP:
                if (isDown) CallbackBridge.sendScroll(0, -1d);
                break;
            case ControlData.SPECIALBTN_MENU:
                mControlLayout.notifyAppMenu();
                break;
        }
    }
/**
 * 「OverlappingRendering」を持っているかを確認します。
 */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
