package com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad

import android.view.MotionEvent.AXIS_HAT_X
import android.view.MotionEvent.AXIS_HAT_Y
import android.view.MotionEvent.AXIS_LTRIGGER
import android.view.MotionEvent.AXIS_RTRIGGER
import android.view.MotionEvent.AXIS_RZ
import android.view.MotionEvent.AXIS_X
import android.view.MotionEvent.AXIS_Y
import android.view.MotionEvent.AXIS_Z
import com.arata.yukarilauncher.Tools.currentDisplayMetrics
import com.arata.yukarilauncher.setting.AllSettings
import android.content.Context
import android.graphics.drawable.Animatable
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.math.MathUtils
import com.arata.yukarilauncher.event.single.MCOptionChangeEvent
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.mouse.CursorDrawableUtils
import com.arata.yukarilauncher.ui.view.GrabListener
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge
import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.Settings

open class Gamepad : GrabListener, GamepadHandler {
    private val mSensitivityFactor = (1.4 * (1080f / currentDisplayMetrics.heightPixels)).toDouble()
    private val mPointerImageView: ImageView
    private var mPointerHotspotX = 0f
    private var mPointerHotspotY = 0f
    private var mLastCursorType = Int.MIN_VALUE
    private val mLeftJoystick: GamepadJoystick
    private var mCurrentJoystickDirection = GamepadJoystick.DIRECTION_NONE
    private val mRightJoystick: GamepadJoystick
    private var mLastHorizontalValue = 0.0f
    private var mLastVerticalValue = 0.0f

    companion object {
        private const val MOUSE_MAX_ACCELERATION = 2.0

        fun sendInput(keycodes: ShortArray, isDown: Boolean) {
            for (keycode in keycodes) {
                when (keycode.toInt()) {
                    GamepadMap.MOUSE_SCROLL_DOWN.toInt() -> if (isDown) CallbackBridge.sendScroll(0.0, -1.0)
                    GamepadMap.MOUSE_SCROLL_UP.toInt() -> if (isDown) CallbackBridge.sendScroll(0.0, 1.0)
                    GamepadMap.MOUSE_LEFT.toInt() -> CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), isDown)
                    GamepadMap.MOUSE_MIDDLE.toInt() -> CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt(), isDown)
                    GamepadMap.MOUSE_RIGHT.toInt() -> CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isDown)
                    GamepadMap.UNSPECIFIED.toInt() -> {}
                    else -> {
                        CallbackBridge.sendKeyPress(keycode.toInt(), CallbackBridge.getCurrentMods(), isDown)
                        CallbackBridge.setModifiers(keycode.toInt(), isDown)
                    }
                }
            }
        }

        fun isGamepadEvent(event: MotionEvent): Boolean = GamepadJoystick.isJoystickEvent(event)

        fun isGamepadEvent(event: KeyEvent): Boolean {
            val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (event.device != null && (event.device!!.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
            return isGamepad && GamepadDpad.isDpadEvent(event)
        }

        private fun sendDirectionalKeycode(direction: Int, isDown: Boolean, map: GamepadMap) {
            when (direction) {
                GamepadJoystick.DIRECTION_NORTH -> map.DIRECTION_FORWARD!!.update(isDown)
                GamepadJoystick.DIRECTION_NORTH_EAST -> {
                    map.DIRECTION_FORWARD!!.update(isDown)
                    map.DIRECTION_RIGHT!!.update(isDown)
                }
                GamepadJoystick.DIRECTION_EAST -> map.DIRECTION_RIGHT!!.update(isDown)
                GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                    map.DIRECTION_RIGHT!!.update(isDown)
                    map.DIRECTION_BACKWARD!!.update(isDown)
                }
                GamepadJoystick.DIRECTION_SOUTH -> map.DIRECTION_BACKWARD!!.update(isDown)
                GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                    map.DIRECTION_BACKWARD!!.update(isDown)
                    map.DIRECTION_LEFT!!.update(isDown)
                }
                GamepadJoystick.DIRECTION_WEST -> map.DIRECTION_LEFT!!.update(isDown)
                GamepadJoystick.DIRECTION_NORTH_WEST -> {
                    map.DIRECTION_FORWARD!!.update(isDown)
                    map.DIRECTION_LEFT!!.update(isDown)
                }
            }
        }
    }

    private var mMouseMagnitude = 0.0
    private var mMouseAngle = 0.0
    private var mMouseSensitivity = 19.0
    private var mGameMap: GamepadMap? = null
    private var mMenuMap: GamepadMap? = null
    private var mCurrentMap: GamepadMap? = null
    private var isGrabbing = false
    private val mScreenChoreographer: Choreographer
    private var mLastFrameTime: Long = 0
    private val mMapProvider: GamepadDataProvider

    constructor(contextView: View, inputDevice: InputDevice, mapProvider: GamepadDataProvider, showCursor: Boolean) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        Settings.setDeadzoneScale(AllSettings.deadZoneScale.getValue() / 100f)

        mScreenChoreographer = Choreographer.getInstance()
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                tick(frameTimeNanos)
                mScreenChoreographer.postFrameCallback(this)
            }
        }
        mScreenChoreographer.postFrameCallback(frameCallback)
        mLastFrameTime = System.nanoTime()

        mLeftJoystick = GamepadJoystick(AXIS_X, AXIS_Y, inputDevice)
        mRightJoystick = GamepadJoystick(AXIS_Z, AXIS_RZ, inputDevice)

        val ctx = contextView.context
        mPointerImageView = ImageView(ctx)
        updatePointerDrawable()
        if (mPointerImageView.drawable != null) {
            mPointerImageView.drawable!!.setFilterBitmap(false)
        }

        val size = ((22 * MCOptions.mcScale) / AllStaticSettings.scaleFactor).toInt()
        mPointerImageView.layoutParams = FrameLayout.LayoutParams(size, size)
        updatePointerHotspot(size, size)

        mMapProvider = mapProvider

        CallbackBridge.sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)

        if (showCursor) {
            (contextView.parent as ViewGroup).addView(mPointerImageView)
        }

        placePointerView(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2)

        reloadGamepadMaps()
        mMapProvider.attachGrabListener(this)
    }

    fun reloadGamepadMaps() {
        mGameMap?.resetPressedState()
        mMenuMap?.resetPressedState()
        GamepadMapStore.load()
        mGameMap = mMapProvider.gameMap
        mMenuMap = mMapProvider.menuMap
        mCurrentMap = mGameMap
        val currentGrab = CallbackBridge.isGrabbing()
        isGrabbing = !currentGrab
        onGrabState(currentGrab)
    }

    fun updateJoysticks() {
        updateDirectionalJoystick()
        updateMouseJoystick()
    }

    @Subscribe
    fun event(event: MCOptionChangeEvent) {
        notifyGUISizeChange(MCOptions.mcScale)
    }

    fun notifyGUISizeChange(newSize: Int) {
        val size = ((22 * newSize) / AllStaticSettings.scaleFactor).toInt()
        mPointerImageView.post {
            mPointerImageView.layoutParams = FrameLayout.LayoutParams(size, size)
            updatePointerHotspot(size, size)
        }
    }

    private fun tick(frameTimeNanos: Long) {
        if (mLastCursorType != CallbackBridge.getCurrentCursorType()) {
            mPointerImageView.post { updatePointerDrawable() }
        }
        var newFrameTime = System.nanoTime()
        if (mLastHorizontalValue != 0f || mLastVerticalValue != 0f) {
            var acceleration = Math.pow(mMouseMagnitude, MOUSE_MAX_ACCELERATION)
            if (acceleration > 1) acceleration = 1.0

            var deltaX = (Math.cos(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            var deltaY = (Math.sin(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            newFrameTime = System.nanoTime()
            val deltaTimeScale = (newFrameTime - mLastFrameTime) / 16666666f
            deltaX *= deltaTimeScale
            deltaY *= deltaTimeScale

            CallbackBridge.sendCursorPos(CallbackBridge.mouseX + deltaX, CallbackBridge.mouseY - deltaY)

            if (!isGrabbing) {
                val clampedX = MathUtils.clamp(CallbackBridge.mouseX + deltaX, 0f, CallbackBridge.windowWidth.toFloat())
                val clampedY = MathUtils.clamp(CallbackBridge.mouseY - deltaY, 0f, CallbackBridge.windowHeight.toFloat())
                CallbackBridge.sendCursorPos(clampedX, clampedY)
                placePointerView((clampedX / AllStaticSettings.scaleFactor).toInt(), (clampedY / AllStaticSettings.scaleFactor).toInt())
            }
        }
        mLastFrameTime = newFrameTime
    }

    fun updatePointerDrawable() {
        mLastCursorType = CallbackBridge.getCurrentCursorType()
        mPointerImageView.setImageDrawable(YLTools.customMouse(mPointerImageView.context))
        if (mPointerImageView.drawable != null) {
            mPointerImageView.drawable!!.setVisible(true, true)
        }
        if (mPointerImageView.drawable is Animatable) {
            (mPointerImageView.drawable as Animatable).start()
        }
        if (mPointerImageView.drawable != null) {
            mPointerImageView.drawable!!.setFilterBitmap(false)
            val layoutParams = mPointerImageView.layoutParams
            if (layoutParams != null) {
                updatePointerHotspot(layoutParams.width, layoutParams.height)
            }
        }
    }

    private fun updatePointerHotspot(width: Int, height: Int) {
        if (mPointerImageView.drawable == null) return
        val hotspot = CursorDrawableUtils.getScaledHotspot(mPointerImageView.drawable!!, width, height)
        mPointerHotspotX = hotspot[0].toFloat()
        mPointerHotspotY = hotspot[1].toFloat()
    }

    private fun updateMouseJoystick() {
        val currentJoystick = if (isGrabbing) mRightJoystick else mLeftJoystick
        val horizontalValue = currentJoystick.getHorizontalAxis()
        val verticalValue = currentJoystick.getVerticalAxis()
        if (horizontalValue != mLastHorizontalValue || verticalValue != mLastVerticalValue) {
            mLastHorizontalValue = horizontalValue
            mLastVerticalValue = verticalValue
            mMouseMagnitude = currentJoystick.magnitude
            mMouseAngle = currentJoystick.angleRadian
            tick(System.nanoTime())
            return
        }
        mLastHorizontalValue = horizontalValue
        mLastVerticalValue = verticalValue
        mMouseMagnitude = currentJoystick.magnitude
        mMouseAngle = currentJoystick.angleRadian
    }

    private fun updateDirectionalJoystick() {
        val currentJoystick = if (isGrabbing) mLeftJoystick else mRightJoystick
        val lastJoystickDirection = mCurrentJoystickDirection
        mCurrentJoystickDirection = currentJoystick.heightDirection
        if (mCurrentJoystickDirection == lastJoystickDirection) return
        sendDirectionalKeycode(lastJoystickDirection, false, currentMap)
        sendDirectionalKeycode(mCurrentJoystickDirection, true, currentMap)
    }

    private val currentMap: GamepadMap
        get() = mCurrentMap!!

    override fun onGrabState(isGrabbing: Boolean) {
        val lastGrabbingValue = this.isGrabbing
        this.isGrabbing = isGrabbing
        if (lastGrabbingValue == isGrabbing) return

        mCurrentMap!!.resetPressedState()
        if (isGrabbing) {
            mCurrentMap = mGameMap
            mPointerImageView.visibility = View.INVISIBLE
            mMouseSensitivity = 18.0
            return
        }

        mCurrentMap = mMenuMap
        sendDirectionalKeycode(mCurrentJoystickDirection, false, mGameMap!!)

        CallbackBridge.sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)
        placePointerView(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2)
        mPointerImageView.visibility = View.VISIBLE
        mMouseSensitivity = 19.0 * AllStaticSettings.scaleFactor / mSensitivityFactor
    }

    override fun handleGamepadInput(keycode: Int, value: Float) {
        val isKeyEventDown = value == 1f
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A -> currentMap.BUTTON_A!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_B -> currentMap.BUTTON_B!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_X -> currentMap.BUTTON_X!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_Y -> currentMap.BUTTON_Y!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L1 -> currentMap.SHOULDER_LEFT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R1 -> currentMap.SHOULDER_RIGHT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L2 -> currentMap.TRIGGER_LEFT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R2 -> currentMap.TRIGGER_RIGHT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_THUMBL -> currentMap.THUMBSTICK_LEFT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_THUMBR -> currentMap.THUMBSTICK_RIGHT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_UP -> currentMap.DPAD_UP!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_DOWN -> currentMap.DPAD_DOWN!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_LEFT -> currentMap.DPAD_LEFT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_RIGHT -> currentMap.DPAD_RIGHT!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                currentMap.DPAD_RIGHT!!.update(false)
                currentMap.DPAD_LEFT!!.update(false)
                currentMap.DPAD_UP!!.update(false)
                currentMap.DPAD_DOWN!!.update(false)
            }
            KeyEvent.KEYCODE_BUTTON_START -> currentMap.BUTTON_START!!.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_SELECT -> currentMap.BUTTON_SELECT!!.update(isKeyEventDown)
            AXIS_HAT_X -> {
                currentMap.DPAD_RIGHT!!.update(value > 0.85f)
                currentMap.DPAD_LEFT!!.update(value < -0.85f)
            }
            AXIS_HAT_Y -> {
                currentMap.DPAD_DOWN!!.update(value > 0.85f)
                currentMap.DPAD_UP!!.update(value < -0.85f)
            }
            AXIS_X -> {
                mLeftJoystick.setXAxisValue(value)
                updateJoysticks()
            }
            AXIS_Y -> {
                mLeftJoystick.setYAxisValue(value)
                updateJoysticks()
            }
            AXIS_Z -> {
                mRightJoystick.setXAxisValue(value)
                updateJoysticks()
            }
            AXIS_RZ -> {
                mRightJoystick.setYAxisValue(value)
                updateJoysticks()
            }
            AXIS_RTRIGGER -> currentMap.TRIGGER_RIGHT!!.update(value > 0.5f)
            AXIS_LTRIGGER -> currentMap.TRIGGER_LEFT!!.update(value > 0.5f)
            else -> CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SPACE.toInt(), CallbackBridge.getCurrentMods(), isKeyEventDown)
        }
    }

    private fun placePointerView(x: Int, y: Int) {
        mPointerImageView.x = x - mPointerHotspotX
        mPointerImageView.y = y - mPointerHotspotY
    }
}
