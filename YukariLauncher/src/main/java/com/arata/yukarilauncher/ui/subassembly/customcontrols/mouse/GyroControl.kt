package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.ui.view.GrabListener
import org.lwjgl.glfw.CallbackBridge
import java.util.Arrays

/**
 * ジャイロセンサーによるマウス制御クラス。
 * デバイスの回転ベクトルセンサーを使用して、物理的な動きをマウス操作に変換します。
 */
class GyroControl(activity: Activity) : SensorEventListener, GrabListener {
    companion object {
        private const val SINGLE_AXIS_LOW_PASS_THRESHOLD = 1.13f
        private const val MULTI_AXIS_LOW_PASS_THRESHOLD = 1.3f
        private const val ROTATION_VECTOR_WARMUP_PERIOD = 2
    }

    private val mWindowManager: WindowManager = activity.windowManager
    private var mSurfaceRotation = -10
    private val mSensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val mCorrectionListener = OrientationCorrectionListener(activity)
    private var mShouldHandleEvents = false
    private var mWarmup = 0
    private var xFactor = 1f
    private var yFactor = 1f
    private var mSwapXY = false

    private val mPreviousRotation = FloatArray(16)
    private val mCurrentRotation = FloatArray(16)
    private val mAngleDifference = FloatArray(3)

    private val mAngleBuffer = Array(
        if (AllSettings.gyroSmoothing.getValue()) 2 else 1
    ) { FloatArray(3) }
    private var xTotal = 0f
    private var yTotal = 0f
    private var xAverage = 0f
    private var yAverage = 0f
    private var mHistoryIndex = -1
    private var mStoredX = 0f
    private var mStoredY = 0f

    init {
        updateOrientation()
    }

    /** ジャイロ制御を有効にします。 */
    fun enable() {
        if (mSensor == null) return
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mSensorManager.registerListener(this, mSensor, (1000 * AllSettings.gyroSampleRate.getValue()))
        mCorrectionListener.enable()
        mShouldHandleEvents = CallbackBridge.isGrabbing()
        CallbackBridge.addGrabListener(this)
    }

    /** ジャイロ制御を無効にします。 */
    fun disable() {
        if (mSensor == null) return
        mSensorManager.unregisterListener(this)
        mCorrectionListener.disable()
        mStoredX = 0f
        mStoredY = 0f
        resetDamper()
        CallbackBridge.removeGrabListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (!mShouldHandleEvents) return
        System.arraycopy(mCurrentRotation, 0, mPreviousRotation, 0, 16)
        SensorManager.getRotationMatrixFromVector(mCurrentRotation, sensorEvent.values)

        if (mWarmup > 0) {
            mWarmup--
            return
        }
        SensorManager.getAngleChange(mAngleDifference, mCurrentRotation, mPreviousRotation)
        damperValue(mAngleDifference)
        mStoredX += xAverage * 10 * AllStaticSettings.gyroSensitivity
        mStoredY += yAverage * 10 * AllStaticSettings.gyroSensitivity

        var updatePosition = false
        val absX = Math.abs(mStoredX)
        val absY = Math.abs(mStoredY)

        if (absX + absY > MULTI_AXIS_LOW_PASS_THRESHOLD) {
            CallbackBridge.mouseX -= (if (mSwapXY) mStoredY else mStoredX) * xFactor
            CallbackBridge.mouseY += (if (mSwapXY) mStoredX else mStoredY) * yFactor
            mStoredX = 0f
            mStoredY = 0f
            updatePosition = true
        } else {
            if (Math.abs(mStoredX) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                CallbackBridge.mouseX -= (if (mSwapXY) mStoredY else mStoredX) * xFactor
                mStoredX = 0f
                updatePosition = true
            }
            if (Math.abs(mStoredY) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                CallbackBridge.mouseY += (if (mSwapXY) mStoredX else mStoredY) * yFactor
                mStoredY = 0f
                updatePosition = true
            }
        }

        if (updatePosition) {
            CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }
    }

    /** 画面の向きに合わせて座標変換を更新します。 */
    fun updateOrientation() {
        val rotation = mWindowManager.defaultDisplay.rotation
        mSurfaceRotation = rotation
        when (rotation) {
            Surface.ROTATION_0 -> {
                mSwapXY = true
                xFactor = 1f
                yFactor = 1f
            }
            Surface.ROTATION_90 -> {
                mSwapXY = false
                xFactor = -1f
                yFactor = 1f
            }
            Surface.ROTATION_180 -> {
                mSwapXY = true
                xFactor = -1f
                yFactor = -1f
            }
            Surface.ROTATION_270 -> {
                mSwapXY = false
                xFactor = 1f
                yFactor = -1f
            }
        }

        if (AllStaticSettings.gyroInvertX) xFactor *= -1f
        if (AllStaticSettings.gyroInvertY) yFactor *= -1f
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}

    override fun onGrabState(isGrabbing: Boolean) {
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mShouldHandleEvents = isGrabbing
    }

    /**
     * 角度差をダンパー（平滑化）処理します。
     * @param newAngleDifference 新しい角度差データ
     */
    private fun damperValue(newAngleDifference: FloatArray) {
        mHistoryIndex++
        if (mHistoryIndex >= mAngleBuffer.size) mHistoryIndex = 0

        xTotal -= mAngleBuffer[mHistoryIndex][1]
        yTotal -= mAngleBuffer[mHistoryIndex][2]

        System.arraycopy(newAngleDifference, 0, mAngleBuffer[mHistoryIndex], 0, 3)

        xTotal += mAngleBuffer[mHistoryIndex][1]
        yTotal += mAngleBuffer[mHistoryIndex][2]

        xAverage = xTotal / mAngleBuffer.size
        yAverage = yTotal / mAngleBuffer.size
    }

    /** ダンパーの状態をリセットします。 */
    private fun resetDamper() {
        mHistoryIndex = -1
        xTotal = 0f
        yTotal = 0f
        xAverage = 0f
        yAverage = 0f
        for (oldAngle in mAngleBuffer) {
            Arrays.fill(oldAngle, 0f)
        }
    }

    /** デバイスの向き変化を検出して座標軸を補正する内部クラス。 */
    inner class OrientationCorrectionListener(context: Context) : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(i: Int) {
            if (!mShouldHandleEvents) return
            if (i == ORIENTATION_UNKNOWN) return

            when (mSurfaceRotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    mSwapXY = false
                    if (225 < i && i < 315) {
                        xFactor = -1f
                        yFactor = 1f
                    } else if (45 < i && i < 135) {
                        xFactor = 1f
                        yFactor = -1f
                    }
                }
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    mSwapXY = true
                    if ((315 < i && i <= 360) || i < 45) {
                        xFactor = 1f
                        yFactor = 1f
                    } else if (135 < i && i < 225) {
                        xFactor = -1f
                        yFactor = -1f
                    }
                }
            }

            if (AllStaticSettings.gyroInvertX) xFactor *= -1f
            if (AllStaticSettings.gyroInvertY) yFactor *= -1f
        }
    }
}