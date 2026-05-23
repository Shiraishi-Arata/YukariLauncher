package net.kdt.pojavlaunch.customcontrols.gamepad;

import android.view.InputDevice;
import android.view.MotionEvent;

import net.kdt.pojavlaunch.utils.MathUtils;

public class GamepadJoystick {

    // 方向
    public static final int DIRECTION_NONE = -1; // 中央にあるGamepadJoystick

    public static final int DIRECTION_EAST = 0;
    public static final int DIRECTION_NORTH_EAST = 1;
    public static final int DIRECTION_NORTH = 2;
    public static final int DIRECTION_NORTH_WEST = 3;
    public static final int DIRECTION_WEST = 4;
    public static final int DIRECTION_SOUTH_WEST = 5;
    public static final int DIRECTION_SOUTH = 6;
    public static final int DIRECTION_SOUTH_EAST = 7;

    private final InputDevice mInputDevice;

    private final int mHorizontalAxis;
    private final int mVerticalAxis;
    private float mVerticalAxisValue = 0;
    private float mHorizontalAxisValue = 0;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public GamepadJoystick(int horizontalAxis, int verticalAxis, InputDevice device){
        mHorizontalAxis = horizontalAxis;
        mVerticalAxis = verticalAxis;
        this.mInputDevice = device;
    }
/**
 * 「AngleRadian」の値を取得します。
 */
    public double getAngleRadian(){
        // -PIからPIまで
        // TODO misuse of the deadzone here !
        return -Math.atan2(getVerticalAxis(), getHorizontalAxis());
    }
/**
 * 「AngleDegree」の値を取得します。
 */
    public double getAngleDegree(){
        // 0から360度まで
        double result = Math.toDegrees(getAngleRadian());
        if(result < 0) result += 360;

        return result;
    }
/**
 * 「Magnitude」の値を取得します。
 */
    public double getMagnitude(){
        float x = Math.abs(mHorizontalAxisValue);
        float y = Math.abs(mVerticalAxisValue);

        return MathUtils.dist(0,0, x, y);
    }
/**
 * 「VerticalAxis」の値を取得します。
 */
    public float getVerticalAxis(){
        return mVerticalAxisValue;
    }
/**
 * 「HorizontalAxis」の値を取得します。
 */
    public float getHorizontalAxis(){
        return mHorizontalAxisValue;
    }
/**
 * このオブジェクトが「JoystickEvent」状態であるかを判定します。
 */
    public static boolean isJoystickEvent(MotionEvent event){
        return (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && event.getAction() == MotionEvent.ACTION_MOVE;
    }
/**
 * 「HeightDirection」の値を取得します。
 */
    public int getHeightDirection(){
        if(getMagnitude() == 0) return DIRECTION_NONE;
        return ((int) ((getAngleDegree()+22.5)/45)) % 8;
    }
/**
 * 「XAxisValue」の値を設定します。
 */


    /* Setters */
    public void setXAxisValue(float value){
        this.mHorizontalAxisValue = value;
    }
/**
 * 「YAxisValue」の値を設定します。
 */
    public void setYAxisValue(float value){
        this.mVerticalAxisValue = value;
    }
}
