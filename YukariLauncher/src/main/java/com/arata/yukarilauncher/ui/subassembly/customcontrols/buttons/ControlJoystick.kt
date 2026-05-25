package com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.View
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlJoystickData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad.GamepadJoystick
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup
import org.lwjgl.glfw.CallbackBridge
import io.github.controlwear.virtual.joystick.android.JoystickView

@SuppressLint("ViewConstructor")
class ControlJoystick : JoystickView, ControlInterface {
    companion object {
        const val DIRECTION_FORWARD_LOCK = 8
    }

    private val mDirectionForwardLock = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt())
    private val mDirectionForward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_W.toInt())
    private val mDirectionRight = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_D.toInt())
    private val mDirectionBackward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_S.toInt())
    private val mDirectionLeft = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_A.toInt())
    private var mControlData: ControlJoystickData? = null
    private var mLastDirectionInt = GamepadJoystick.DIRECTION_NONE
    private var mCurrentDirectionInt = GamepadJoystick.DIRECTION_NONE

    constructor(parent: ControlLayout, data: ControlJoystickData) : super(parent.context) {
        init(data, parent)
    }

    private fun sendInput(keys: IntArray, isDown: Boolean) {
        for (key in keys) {
            CallbackBridge.sendKeyPress(key, CallbackBridge.getCurrentMods(), isDown)
        }
    }

    private fun init(data: ControlJoystickData, layout: ControlLayout) {
        mControlData = data
        setProperties(preProcessProperties(data, layout))
        setDeadzone(35)
        setFixedCenter(data.absolute)
        setAutoReCenterButton(true)

        injectBehaviors()

        setOnMoveListener(object : OnMoveListener {
            override fun onMove(angle: Int, strength: Int) {
                mLastDirectionInt = mCurrentDirectionInt
                mCurrentDirectionInt = getDirectionInt(angle, strength)

                if (mLastDirectionInt != mCurrentDirectionInt) {
                    sendDirectionalKeycode(mLastDirectionInt, false)
                    sendDirectionalKeycode(mCurrentDirectionInt, true)
                }
            }

            override fun onForwardLock(isLocked: Boolean) {
                sendInput(mDirectionForwardLock, isLocked)
            }
        })
    }

    override val controlView: View
        get() = this

    override val properties: ControlData
        get() = mControlData!!

    override fun setProperties(properties: ControlData, changePos: Boolean) {
        mControlData = properties as ControlJoystickData
        mControlData!!.isHideable = true
        super<ControlInterface>.setProperties(properties, changePos)
        postDelayed({
            setForwardLockDistance(if (mControlData!!.forwardLock) Tools.dpToPx(60f).toInt() else 0)
            setFixedCenter(mControlData!!.absolute)
        }, 10)
    }

    override fun removeButton() {
        getControlLayoutParent().layout!!.mJoystickDataList!!.remove(properties)
        getControlLayoutParent().removeView(this)
    }

    override fun cloneButton() {
        val data = ControlJoystickData(mControlData!!)
        getControlLayoutParent().addJoystickButton(data)
    }

    override fun setBackground() {
        setBorderWidth((Tools.dpToPx(properties.strokeWidth * (getControlLayoutParent().layoutScale / 100f))).toInt())
        setBorderColor(properties.strokeColor)
        setBackgroundColor(properties.bgColor)
    }

    override fun sendKeyPresses(isDown: Boolean) {}

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadJoystickValues(mControlData!!)
    }

    private fun getDirectionInt(angle: Int, intensity: Int): Int {
        if (intensity == 0) return GamepadJoystick.DIRECTION_NONE
        return ((angle + 22.5) / 45 % 8).toInt()
    }

    private fun sendDirectionalKeycode(direction: Int, isDown: Boolean) {
        when (direction) {
            GamepadJoystick.DIRECTION_NORTH -> sendInput(mDirectionForward, isDown)
            GamepadJoystick.DIRECTION_NORTH_EAST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionRight, isDown)
            }
            GamepadJoystick.DIRECTION_EAST -> sendInput(mDirectionRight, isDown)
            GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                sendInput(mDirectionRight, isDown)
                sendInput(mDirectionBackward, isDown)
            }
            GamepadJoystick.DIRECTION_SOUTH -> sendInput(mDirectionBackward, isDown)
            GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                sendInput(mDirectionBackward, isDown)
                sendInput(mDirectionLeft, isDown)
            }
            GamepadJoystick.DIRECTION_WEST -> sendInput(mDirectionLeft, isDown)
            GamepadJoystick.DIRECTION_NORTH_WEST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionLeft, isDown)
            }
            DIRECTION_FORWARD_LOCK -> sendInput(mDirectionForwardLock, isDown)
        }
    }
}
