package com.arata.yukarilauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import com.arata.yukarilauncher.event.single.RefreshHotbarEvent
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.feature.awt.EfficientAndroidLWJGLKeycode
import com.arata.yukarilauncher.feature.graphics.GameGraphicsApiHelper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.ui.activity.BaseActivity
import com.arata.yukarilauncher.ui.activity.MainActivity.Companion.touchCharInput
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad.DefaultDataProvider
import com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad.Gamepad
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.AbstractTouchpad
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.AndroidPointerCapture
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.InGUIEventProcessor
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.InGameEventProcessor
import com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse.TouchEventProcessor
import com.arata.yukarilauncher.ui.view.GrabListener
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.utils.runtime.JREUtils
import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.sendMouseButton
import org.lwjgl.glfw.CallbackBridge.windowHeight
import org.lwjgl.glfw.CallbackBridge.windowWidth
import java.util.Locale
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView

class MinecraftGLSurface : View, GrabListener {
    companion object {
        private var sCurrentVersionName: String? = null

        fun setCurrentVersionName(versionName: String?) {
            sCurrentVersionName = versionName
        }

        fun sendMouseButtonUnconverted(button: Int, status: Boolean): Boolean {
            val glfwButton = when (button) {
                MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt()
                MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt()
                MotionEvent.BUTTON_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt()
                else -> -256
            }
            if (glfwButton == -256) return false
            sendMouseButton(glfwButton, status)
            return true
        }
    }

    private var mGamepad: Gamepad? = null
    private val mInputManager = RemapperManager(context, RemapperView.Builder(null)
        .remapA(true).remapB(true).remapX(true).remapY(true)
        .remapLeftJoystick(true).remapRightJoystick(true)
        .remapStart(true).remapSelect(true)
        .remapLeftShoulder(true).remapRightShoulder(true)
        .remapLeftTrigger(true).remapRightTrigger(true)
        .remapDpad(true))

    private val mSensitivityFactor = 1.4 * (1080f / Tools.getDisplayMetrics(context as BaseActivity).heightPixels)

    var mSurfaceReadyListener: SurfaceReadyListener? = null
    val mSurfaceReadyListenerLock = Any()
    var mSurface: View? = null

    private val mIngameProcessor = InGameEventProcessor(mSensitivityFactor)
    private val mInGUIProcessor = InGUIEventProcessor()
    private var mCurrentTouchProcessor: TouchEventProcessor = mInGUIProcessor
    private var mPointerCapture: AndroidPointerCapture? = null
    private var mLastGrabState = false
    private var mOnRenderingStartedListener: OnRenderingStartedListener? = null
    private var mIsRenderingStarted = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        isFocusable = true
    }

    private fun setUpPointerCapture(touchpad: AbstractTouchpad?) {
        mPointerCapture?.detach()
        mPointerCapture = AndroidPointerCapture(touchpad!!, this)
    }

    fun start(isAlreadyRunning: Boolean, touchpad: AbstractTouchpad?) {
        setUpPointerCapture(touchpad)
        mInGUIProcessor.setAbstractTouchpad(touchpad)
        if (AllSettings.alternateSurface.getValue()) {
            val surfaceView = SurfaceView(context)
            mSurface = surfaceView
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                private var isCalled = isAlreadyRunning
                override fun surfaceCreated(@NonNull holder: SurfaceHolder) {
                    if (isCalled) {
                        JREUtils.setupBridgeWindow(surfaceView.holder.surface)
                        return
                    }
                    isCalled = true
                    realStart(surfaceView.holder.surface)
                }

                override fun surfaceChanged(@NonNull holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    refreshSize()
                }

                override fun surfaceDestroyed(@NonNull holder: SurfaceHolder) {}
            })
            (parent as ViewGroup).addView(surfaceView)
        } else {
            val textureView = TextureView(context)
            textureView.isOpaque = true
            textureView.alpha = 1.0f
            mSurface = textureView
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                private var isCalled = isAlreadyRunning
                override fun onSurfaceTextureAvailable(@NonNull surface: SurfaceTexture, width: Int, height: Int) {
                    val tSurface = Surface(surface)
                    if (isCalled) {
                        JREUtils.setupBridgeWindow(tSurface)
                        return
                    }
                    isCalled = true
                    realStart(tSurface)
                }

                override fun onSurfaceTextureSizeChanged(@NonNull surface: SurfaceTexture, width: Int, height: Int) {
                    refreshSize()
                }

                override fun onSurfaceTextureDestroyed(@NonNull surface: SurfaceTexture): Boolean = true

                override fun onSurfaceTextureUpdated(@NonNull surface: SurfaceTexture) {
                    if (!mIsRenderingStarted) {
                        mIsRenderingStarted = true
                        mOnRenderingStartedListener?.isStarted()
                    }
                }
            }
            (parent as ViewGroup).addView(textureView)
        }
    }

    @Suppress("accessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if ((parent as ControlLayout).isModifiable) return false
        for (i in 0 until e.pointerCount) {
            val toolType = e.getToolType(i)
            when {
                toolType == MotionEvent.TOOL_TYPE_MOUSE -> {
                    mPointerCapture?.handleAutomaticCapture()
                    return true
                }
                toolType != MotionEvent.TOOL_TYPE_STYLUS -> continue
            }
            if (CallbackBridge.isGrabbing()) return false
            CallbackBridge.sendCursorPos(e.getX(i) * AllStaticSettings.scaleFactor, e.getY(i) * AllStaticSettings.scaleFactor)
            return true
        }
        return mCurrentTouchProcessor.processTouchEvent(e)
    }

    private fun createGamepad(contextView: View, inputDevice: InputDevice) {
        mGamepad = Gamepad(contextView, inputDevice, DefaultDataProvider, true)
    }

    fun updateMouseDrawable() {
        mGamepad?.updatePointerDrawable()
    }

    @SuppressLint("NewApi")
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var mouseCursorIndex = -1
        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepad == null) createGamepad(this, event.device)
            mInputManager.handleMotionEventInput(context, event, mGamepad)
            return true
        }
        for (i in 0 until event.pointerCount) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS) continue
            mouseCursorIndex = i
            break
        }
        if (mouseCursorIndex == -1) return false
        updateGrabState(CallbackBridge.isGrabbing())
        return when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE -> {
                CallbackBridge.mouseX = event.getX(mouseCursorIndex) * AllStaticSettings.scaleFactor
                CallbackBridge.mouseY = event.getY(mouseCursorIndex) * AllStaticSettings.scaleFactor
                CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                true
            }
            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble())
                true
            }
            MotionEvent.ACTION_BUTTON_PRESS -> sendMouseButtonUnconverted(event.actionButton, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> sendMouseButtonUnconverted(event.actionButton, false)
            else -> false
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        mCurrentTouchProcessor.dispatchTouchEvent(event, this)
        return super.dispatchTouchEvent(event)
    }

    fun processKeyEvent(event: KeyEvent): Boolean {
        val eventKeycode = event.keyCode
        if (eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return true
        val action = event.action
        if (action == KeyEvent.ACTION_MULTIPLE) return true
        if (action == KeyEvent.ACTION_UP && event.flags and KeyEvent.FLAG_CANCELED != 0) return true
        if (event.flags and KeyEvent.FLAG_SOFT_KEYBOARD == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (eventKeycode == KeyEvent.KEYCODE_ENTER) return true
            touchCharInput?.dispatchKeyEvent(event)
            return true
        }
        if (event.device != null
            && (event.source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE
                    || event.source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE)) {
            if (eventKeycode == KeyEvent.KEYCODE_BACK) {
                sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), event.action == KeyEvent.ACTION_DOWN)
                return true
            }
        }
        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepad == null) createGamepad(this, event.device)
            mInputManager.handleKeyEventInput(context, event, mGamepad)
            return true
        }
        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(eventKeycode)
        if (EfficientAndroidLWJGLKeycode.containsIndex(index)) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }
        return event.flags and KeyEvent.FLAG_FALLBACK == KeyEvent.FLAG_FALLBACK
    }

    fun refreshSize() {
        val newWidth = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.widthPixels, AllStaticSettings.scaleFactor)
        val newHeight = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.heightPixels, AllStaticSettings.scaleFactor)
        if (newHeight < 1 || newWidth < 1) {
            Logging.e("MGLSurface", String.format(Locale.getDefault(), "Impossible resolution : %dx%d", newWidth, newHeight))
            return
        }
        windowWidth = newWidth
        windowHeight = newHeight
        if (mSurface == null) {
            Logging.w("MGLSurface", "Attempt to refresh size on null surface")
            return
        }
        if (AllSettings.alternateSurface.getValue()) {
            val view = mSurface as SurfaceView
            if (view.holder != null) {
                view.holder.setFixedSize(windowWidth, windowHeight)
            }
        } else {
            val view = mSurface as TextureView
            if (view.surfaceTexture != null) {
                view.surfaceTexture?.setDefaultBufferSize(windowWidth, windowHeight)
            }
        }
        CallbackBridge.sendUpdateWindowSize(windowWidth, windowHeight)
        EventBus.getDefault().post(RefreshHotbarEvent())
    }

    private fun realStart(surface: Surface) {
        refreshSize()
        MCOptions.set("fullscreen", "false")
        MCOptions.set("overrideWidth", windowWidth.toString())
        MCOptions.set("overrideHeight", windowHeight.toString())
        GameGraphicsApiHelper.applyPreferredGraphicsBackend(context, sCurrentVersionName)
        MCOptions.save()
        MCOptions.mcScale
        JREUtils.setupBridgeWindow(surface)
        Thread({
            try {
                synchronized(mSurfaceReadyListenerLock) {
                    if (mSurfaceReadyListener == null) (mSurfaceReadyListenerLock as java.lang.Object).wait()
                }
                mSurfaceReadyListener?.isReady()
            } catch (e: Throwable) {
                Tools.showError(context, e, true)
            }
        }, "JVM Main thread").start()
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    private fun pickEventProcessor(isGrabbing: Boolean): TouchEventProcessor {
        if (AllStaticSettings.forceGuiInput) {
            return mInGUIProcessor
        }
        return if (isGrabbing) mIngameProcessor else mInGUIProcessor
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        val desiredProcessor = pickEventProcessor(isGrabbing)
        if (mLastGrabState != isGrabbing || mCurrentTouchProcessor !== desiredProcessor) {
            mCurrentTouchProcessor.cancelPendingActions()
            mCurrentTouchProcessor = desiredProcessor
            mLastGrabState = isGrabbing
        }
    }

    fun refreshTouchProcessor() {
        post { updateGrabState(CallbackBridge.isGrabbing()) }
    }

    interface SurfaceReadyListener {
        fun isReady()
    }

    fun setSurfaceReadyListener(listener: SurfaceReadyListener?) {
        synchronized(mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener
            (mSurfaceReadyListenerLock as java.lang.Object).notifyAll()
        }
    }

    interface OnRenderingStartedListener {
        fun isStarted()
    }

    fun setOnRenderingStartedListener(listener: OnRenderingStartedListener?) {
        mOnRenderingStartedListener = listener
    }
}
