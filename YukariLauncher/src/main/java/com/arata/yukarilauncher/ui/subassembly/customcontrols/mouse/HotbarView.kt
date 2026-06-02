package com.arata.yukarilauncher.ui.subassembly.customcontrols.mouse

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.event.single.MCOptionChangeEvent
import com.arata.yukarilauncher.event.single.RefreshHotbarEvent
import com.arata.yukarilauncher.event.value.HotbarChangeEvent
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarType
import com.arata.yukarilauncher.ui.subassembly.hotbar.HotbarUtils
import com.arata.yukarilauncher.ui.view.GrabListener
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.MathUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge

/**
 * Minecraftホットバー操作用のタッチビュー。
 * ホットバーの各スロットをタップしてアイテム選択やドロップ操作を行います。
 */
class HotbarView : View, View.OnLayoutChangeListener, Runnable {
    companion object {
        private val HOTBAR_KEYS = intArrayOf(
            LwjglGlfwKeycode.GLFW_KEY_1.toInt(), LwjglGlfwKeycode.GLFW_KEY_2.toInt(), LwjglGlfwKeycode.GLFW_KEY_3.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_4.toInt(), LwjglGlfwKeycode.GLFW_KEY_5.toInt(), LwjglGlfwKeycode.GLFW_KEY_6.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_7.toInt(), LwjglGlfwKeycode.GLFW_KEY_8.toInt(), LwjglGlfwKeycode.GLFW_KEY_9.toInt()
        )
    }

    private val mDoubleTapDetector = TapDetector(2, TapDetector.DETECTION_METHOD_DOWN)
    private var mParentView: View? = null
    private val mDropGesture = DropGesture(Handler(Looper.getMainLooper()))
    private val mGrabListener = GrabListener { isGrabbing ->
        mLastIndex = -1
        mDropGesture.cancel()
    }

    private var mWidth = 0
    private var mLastIndex = -1
    private var mGuiScale = 0
    private val adjustAnimPlayer = AnimPlayer()

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) { init() }

    private fun init() {
        alpha = 0f
        setBackgroundColor(Color.parseColor("#80E64242"))
        adjustAnimPlayer.duration(800)
        adjustAnimPlayer.apply(AnimPlayer.Entry(this, Animations.FadeOut))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
        val parent = parent
        if (parent is View) {
            mParentView = parent
            mParentView!!.addOnLayoutChangeListener(this)
        }
        adaptiveReset()
        CallbackBridge.addGrabListener(mGrabListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CallbackBridge.removeGrabListener(mGrabListener)
        EventBus.getDefault().unregister(this)
    }

    /** @param event ホットバー更新イベント */
    @Subscribe
    fun event(event: RefreshHotbarEvent) {
        post(this)
    }

    /** @param event MCオプション変更イベント */
    @Subscribe
    fun event(event: MCOptionChangeEvent) {
        mGuiScale = MCOptions.mcScale
        post(this)
    }

    /** @param event ホットバーサイズ変更イベント */
    @Subscribe
    fun event(event: HotbarChangeEvent) {
        manualReset(event.width, event.height, true)
    }

    /** @return MarginLayoutParamsを取得 */
    private fun getMarginLayoutParams(): ViewGroup.MarginLayoutParams {
        val layoutParams = layoutParams
        if (layoutParams !is ViewGroup.MarginLayoutParams)
            throw RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams")
        return layoutParams
    }

    /** 画面サイズに合わせてホットバーの位置とサイズを自動調整します。 */
    private fun adaptiveReset() {
        val marginLayoutParams = getMarginLayoutParams()
        val height: Int
        marginLayoutParams.width = mcScale(180).also { mWidth = it }
        marginLayoutParams.height = mcScale(20).also { height = it }
        marginLayoutParams.leftMargin = CallbackBridge.physicalWidth / 2 - mWidth / 2
        marginLayoutParams.topMargin = CallbackBridge.physicalHeight - height
        layoutParams = marginLayoutParams
    }

    /**
     * 指定されたサイズでホットバーを再設定します。
     * @param width 幅
     * @param height 高さ
     * @param playAnim アニメーションを再生するかどうか
     */
    private fun manualReset(width: Int, height: Int, playAnim: Boolean) {
        val marginLayoutParams = getMarginLayoutParams()
        marginLayoutParams.width = width.also { mWidth = it }
        marginLayoutParams.height = height
        marginLayoutParams.leftMargin = Tools.currentDisplayMetrics.widthPixels / 2 - width / 2
        marginLayoutParams.topMargin = Tools.currentDisplayMetrics.heightPixels - height
        layoutParams = marginLayoutParams
        if (playAnim) adjustAnimPlayer.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!CallbackBridge.isGrabbing()) return false
        val hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event)

        val actionMasked = event.actionMasked
        if (isLastEventInGesture(actionMasked)) mDropGesture.cancel()
        else mDropGesture.submit()

        val x = event.x
        if (x < 0 || x >= mWidth) {
            mDropGesture.cancel()
            return true
        }
        val hotbarIndex = MathUtils.map(x, 0f, mWidth.toFloat(), 0f, HOTBAR_KEYS.size.toFloat()).toInt()

        if (hotbarIndex == mLastIndex) {
            if (hasDoubleTapped && !AllStaticSettings.disableDoubleTap)
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F.toInt())
            return true
        }

        mLastIndex = hotbarIndex
        val hotbarKey = HOTBAR_KEYS[hotbarIndex]
        CallbackBridge.sendKeyPress(hotbarKey)
        mDropGesture.cancel()
        if (!isLastEventInGesture(actionMasked)) mDropGesture.submit()
        return true
    }

    /** @param actionMasked イベントのアクション */
    private fun isLastEventInGesture(actionMasked: Int): Boolean {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL
    }

    /** @param input 入力値（MCスケール適用後を返す） */
    private fun mcScale(input: Int): Int {
        return (mGuiScale * input / AllStaticSettings.scaleFactor).toInt()
    }

    override fun run() {
        if (parent == null) return

        val hotbarType = HotbarUtils.getCurrentType()
        if (hotbarType == HotbarType.AUTO) {
            adaptiveReset()
        } else {
            manualReset(AllSettings.hotbarWidth.value.getValue(), AllSettings.hotbarHeight.value.getValue(), false)
        }
    }

    override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
        if (v == mParentView && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            post { adaptiveReset() }
        }
    }
}