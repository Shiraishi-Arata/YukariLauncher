package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.view.InputDevice
import android.view.MotionEvent
import com.arata.yukarilauncher.utils.MathUtils

/**
 * ゲームパッドのジョイスティック（アナログスティック）を表すクラス。
 * 水平/垂直軸の値から角度、大きさ、方向を計算します。
 */
class GamepadJoystick {
    companion object {
        /** 無方向。 */
        const val DIRECTION_NONE = -1
        /** 東（右）。 */
        const val DIRECTION_EAST = 0
        /** 北東。 */
        const val DIRECTION_NORTH_EAST = 1
        /** 北（上）。 */
        const val DIRECTION_NORTH = 2
        /** 北西。 */
        const val DIRECTION_NORTH_WEST = 3
        /** 西（左）。 */
        const val DIRECTION_WEST = 4
        /** 南西。 */
        const val DIRECTION_SOUTH_WEST = 5
        /** 南（下）。 */
        const val DIRECTION_SOUTH = 6
        /** 南東。 */
        const val DIRECTION_SOUTH_EAST = 7

        /**
         * MotionEventがジョイスティックイベントかどうかを判定します。
         * @param event 判定するMotionEvent
         * @return ジョイスティックイベントの場合はtrue
         */
        fun isJoystickEvent(event: MotionEvent): Boolean {
            return (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                    && event.action == MotionEvent.ACTION_MOVE
        }
    }

    private val mInputDevice: InputDevice
    private val mHorizontalAxis: Int
    private val mVerticalAxis: Int
    private var mVerticalAxisValue = 0f
    private var mHorizontalAxisValue = 0f

    /**
     * @param horizontalAxis 水平軸の定数
     * @param verticalAxis 垂直軸の定数
     * @param device 入力デバイス
     */
    constructor(horizontalAxis: Int, verticalAxis: Int, device: InputDevice) {
        mHorizontalAxis = horizontalAxis
        mVerticalAxis = verticalAxis
        this.mInputDevice = device
    }

    /** @return ジョイスティックの角度（ラジアン） */
    val angleRadian: Double
        get() = -Math.atan2(getVerticalAxis().toDouble(), getHorizontalAxis().toDouble())

    /** @return ジョイスティックの角度（度） */
    val angleDegree: Double
        get() {
            var result = Math.toDegrees(angleRadian)
            if (result < 0) result += 360.0
            return result
        }

    /** @return ジョイスティックの変位の大きさ */
    val magnitude: Double
        get() {
            val x = Math.abs(mHorizontalAxisValue)
            val y = Math.abs(mVerticalAxisValue)
            return MathUtils.dist(0f, 0f, x, y).toDouble()
        }

    /** @return 垂直軸の値 */
    fun getVerticalAxis(): Float = mVerticalAxisValue

    /** @return 水平軸の値 */
    fun getHorizontalAxis(): Float = mHorizontalAxisValue

    /** @return 現在の方向を示す8方向の定数 */
    val heightDirection: Int
        get() {
            if (magnitude == 0.0) return DIRECTION_NONE
            return ((angleDegree + 22.5) / 45).toInt() % 8
        }

    /** @param value X軸の値を設定 */
    fun setXAxisValue(value: Float) {
        this.mHorizontalAxisValue = value
    }

    /** @param value Y軸の値を設定 */
    fun setYAxisValue(value: Float) {
        this.mVerticalAxisValue = value
    }
}