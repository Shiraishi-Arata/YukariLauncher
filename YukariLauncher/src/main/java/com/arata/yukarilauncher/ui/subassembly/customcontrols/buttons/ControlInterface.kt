package com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons

import android.view.View.GONE
import android.view.View.VISIBLE
import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.core.math.MathUtils
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.view.GrabListener
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup
import org.lwjgl.glfw.CallbackBridge

/**
 * コントロールボタン/ドロワー/ジョイスティックの共通インターフェース。
 * プロパティの設定、位置計算、スナップ動作、グラブリスナーなどの共通機能を定義します。
 */
interface ControlInterface : View.OnLongClickListener, GrabListener {
    /** このコントロールのビューインスタンス。 */
    val controlView: View
    /** このコントロールのプロパティデータ。 */
    val properties: ControlData

    /**
     * プロパティを設定します（位置変更あり）。
     * @param properties 設定するプロパティ
     */
    fun setProperties(properties: ControlData) {
        setProperties(properties, true)
    }

    /** このコントロールを削除します。 */
    fun removeButton()
    /** このコントロールを複製します。 */
    fun cloneButton()

    /**
     * コントロールの表示状態を設定します。
     * @param isVisible 表示する場合はtrue
     */
    fun setVisible(isVisible: Boolean) {
        if (properties.isHideable)
            controlView.visibility = if (isVisible) VISIBLE else GONE
    }

    /**
     * キープレスを送信します。
     * @param isDown 押下状態
     */
    fun sendKeyPresses(isDown: Boolean)

    /**
     * 編集ポップアップに値を読み込みます。
     * @param editControlPopup 編集ポップアップ
     */
    fun loadEditValues(editControlPopup: EditControlPopup)

    override fun onGrabState(isGrabbing: Boolean) {
        val parent = getControlLayoutParent()
        if (parent.isModifiable) return
        setVisible(((properties.displayInGame && isGrabbing) || (properties.displayInMenu && !isGrabbing)) && parent.areControlVisible())
    }

    /** @return 親のControlLayout */
    fun getControlLayoutParent(): ControlLayout {
        return controlView.parent as ControlLayout
    }

    /**
     * プロパティのプリプロセスを行います（スケール調整など）。
     * @param properties プリプロセスするプロパティ
     * @param layout 親レイアウト
     * @return 処理後のControlData
     */
    fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        properties.setWidth(properties.getWidth() / layout.layoutScale * AllSettings.buttonScale.getValue())
        properties.setHeight(properties.getHeight() / layout.layoutScale * AllSettings.buttonScale.getValue())
        properties.isHideable = !properties.containsKeycode(ControlData.SPECIALBTN_TOGGLECTRL) && !properties.containsKeycode(ControlData.SPECIALBTN_VIRTUALMOUSE)
        return properties
    }

    /** プロパティを再適用して更新します。 */
    fun updateProperties() {
        setProperties(properties)
    }

    /**
     * プロパティを設定します。
     * @param properties 設定するプロパティ
     * @param changePos 位置を変更するかどうか
     */
    @CallSuper
    fun setProperties(properties: ControlData, changePos: Boolean) {
        if (changePos) {
            controlView.x = properties.insertDynamicPos(this.properties.dynamicX!!)
            controlView.y = properties.insertDynamicPos(this.properties.dynamicY!!)
        }

        var params = controlView.layoutParams
        if (params == null)
            params = FrameLayout.LayoutParams(properties.getWidth().toInt(), properties.getHeight().toInt())
        params!!.width = properties.getWidth().toInt()
        params.height = properties.getHeight().toInt()
        controlView.layoutParams = params
    }

    /** 背景の描画設定を適用します。 */
    fun setBackground() {
        val gd = if (controlView.background is GradientDrawable)
            controlView.background as GradientDrawable
        else
            GradientDrawable()
        gd.setColor(properties.bgColor)
        gd.setStroke((Tools.dpToPx(properties.strokeWidth * (getControlLayoutParent().layoutScale / 100f))).toInt(), properties.strokeColor)
        gd.setCornerRadius(computeCornerRadius(properties.cornerRadius))
        controlView.background = gd
    }

    /**
     * 動的なX座標を設定します。
     * @param dynamicX X座標の数式
     */
    fun setDynamicX(dynamicX: String) {
        properties.dynamicX = dynamicX
        controlView.x = properties.insertDynamicPos(dynamicX)
    }

    /**
     * 動的なY座標を設定します。
     * @param dynamicY Y座標の数式
     */
    fun setDynamicY(dynamicY: String) {
        properties.dynamicY = dynamicY
        controlView.y = properties.insertDynamicPos(dynamicY)
    }

    /**
     * 指定されたX座標から動的位置の数式を生成します。
     * @param x 実際のX座標
     * @return 動的位置の数式
     */
    fun generateDynamicX(x: Float): String {
        return if (x + (properties.getWidth() / 2f) > CallbackBridge.physicalWidth / 2f) {
            "${(x + properties.getWidth()) / CallbackBridge.physicalWidth} * \${screen_width} - \${width}"
        } else {
            "${x / CallbackBridge.physicalWidth} * \${screen_width}"
        }
    }

    /**
     * 指定されたY座標から動的位置の数式を生成します。
     * @param y 実際のY座標
     * @return 動的位置の数式
     */
    fun generateDynamicY(y: Float): String {
        return if (y + (properties.getHeight() / 2f) > CallbackBridge.physicalHeight / 2f) {
            "${(y + properties.getHeight()) / CallbackBridge.physicalHeight} * \${screen_height} - \${height}"
        } else {
            "${y / CallbackBridge.physicalHeight} * \${screen_height}"
        }
    }

    /** 現在の座標から動的位置を再生成して更新します。 */
    fun regenerateDynamicCoordinates() {
        properties.dynamicX = generateDynamicX(controlView.x)
        properties.dynamicY = generateDynamicY(controlView.y)
        updateProperties()
    }

    /**
     * サイズ計算式に実際の値を適用します。
     * @param equation サイズ計算式
     * @param button 対象のボタン
     * @return 適用後の数式
     */
    fun applySize(equation: String, button: ControlInterface): String {
        return equation
            .replace("\${right}", "(\${screen_width} - \${width})")
            .replace("\${bottom}", "(\${screen_height} - \${height})")
            .replace("\${height}", "(px(${Tools.pxToDp(button.properties.getHeight())}) / ${AllSettings.buttonScale.getValue()} * \${preferred_scale})")
            .replace("\${width}", "(px(${Tools.pxToDp(button.properties.getWidth())}) / ${AllSettings.buttonScale.getValue()} * \${preferred_scale})")
    }

    /**
     * 角丸の半径をパーセントからピクセルに変換します。
     * @param radiusInPercent パーセントで指定された角丸半径
     * @return ピクセル単位の角丸半径
     */
    fun computeCornerRadius(radiusInPercent: Float): Float {
        val minSize = Math.min(properties.getWidth(), properties.getHeight())
        return (minSize / 2) * (radiusInPercent / 100)
    }

    /**
     * 他のボタンにスナップ可能か判定します。
     * @param button スナップ先の候補ボタン
     * @return スナップ可能な場合はtrue
     */
    fun canSnap(button: ControlInterface): Boolean {
        if (!AllSettings.buttonSnapping.getValue()) return false
        val MIN_DISTANCE = getSnapDistance()
        if (button === this) return false
        if (button.controlView.visibility == GONE) return false
        return !(com.arata.yukarilauncher.utils.MathUtils.dist(
            button.controlView.x + button.controlView.width / 2f,
            button.controlView.y + button.controlView.height / 2f,
            controlView.x + controlView.width / 2f,
            controlView.y + controlView.height / 2f) > Math.max(
            button.controlView.width / 2f + controlView.width / 2f,
            button.controlView.height / 2f + controlView.height / 2f) + MIN_DISTANCE)
    }

    /**
     * スナップ処理を行いながら指定された座標にボタンを配置します。
     * @param x 新しいX座標
     * @param y 新しいY座標
     */
    fun snapAndAlign(x: Float, y: Float) {
        val MIN_DISTANCE = getSnapDistance()
        var dynamicX = generateDynamicX(x)
        var dynamicY = generateDynamicY(y)

        controlView.x = x
        controlView.y = y

        for (button in (controlView.parent as ControlLayout).getButtonChildren()) {
            if (!canSnap(button)) continue

            val button_top = button.controlView.y
            val button_bottom = button_top + button.controlView.height
            val button_left = button.controlView.x
            val button_right = button_left + button.controlView.width

            val top = controlView.y
            val bottom = controlView.y + controlView.height
            val left = controlView.x
            val right = controlView.x + controlView.width

            if (Math.abs(top - button_bottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.properties.dynamicY!!, button) + applySize(" + \${height}", button) + " + \${margin}"
            } else if (Math.abs(button_top - bottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.properties.dynamicY!!, button) + " - \${height} - \${margin}"
            }
            if (dynamicY != generateDynamicY(controlView.y)) {
                if (Math.abs(button_left - left) < MIN_DISTANCE) {
                    dynamicX = applySize(button.properties.dynamicX!!, button)
                } else if (Math.abs(button_right - right) < MIN_DISTANCE) {
                    dynamicX = applySize(button.properties.dynamicX!!, button) + applySize(" + \${width}", button) + " - \${width}"
                }
            }

            if (Math.abs(button_left - right) < MIN_DISTANCE) {
                dynamicX = applySize(button.properties.dynamicX!!, button) + " - \${width} - \${margin}"
            } else if (Math.abs(left - button_right) < MIN_DISTANCE) {
                dynamicX = applySize(button.properties.dynamicX!!, button) + applySize(" + \${width}", button) + " + \${margin}"
            }
            if (dynamicX != generateDynamicX(controlView.x)) {
                if (Math.abs(button_top - top) < MIN_DISTANCE) {
                    dynamicY = applySize(button.properties.dynamicY!!, button)
                } else if (Math.abs(button_bottom - bottom) < MIN_DISTANCE) {
                    dynamicY = applySize(button.properties.dynamicY!!, button) + applySize(" + \${height}", button) + " - \${height}"
                }
            }
        }

        setDynamicX(dynamicX)
        setDynamicY(dynamicY)
    }

    /** 全てのビヘイビアを注入します（プロパティ、タッチ、レイアウト、グラブリスナー）。 */
    fun injectBehaviors() {
        injectProperties()
        injectTouchEventBehavior()
        injectLayoutParamBehavior()
        injectGrabListenerBehavior()
    }

    /** グラブリスナーの挙動を注入します。 */
    fun injectGrabListenerBehavior() {
        if (controlView == null) {
            Logging.e(ControlInterface::class.java.toString(), "Failed to inject grab listener behavior !")
            return
        }

        controlView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                CallbackBridge.addGrabListener(this@ControlInterface)
            }

            override fun onViewDetachedFromWindow(v: View) {
                controlView.removeOnAttachStateChangeListener(this)
                CallbackBridge.removeGrabListener(this@ControlInterface)
            }
        })
    }

    /** プロパティの初期設定（Z位置など）を適用します。 */
    fun injectProperties() {
        controlView.post { controlView.translationZ = 10f }
    }

    /** タッチイベントの挙動を注入します（編集モードでのドラッグ、スナップなど）。 */
    fun injectTouchEventBehavior() {
        controlView.setOnTouchListener(object : View.OnTouchListener {
            private var mCanTriggerLongClick = true
            private var downX = 0f
            private var downY = 0f
            private var downRawX = 0f
            private var downRawY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (!getControlLayoutParent().isModifiable) {
                    view.onTouchEvent(event)
                    return true
                }

                if (event.actionMasked == MotionEvent.ACTION_UP && mCanTriggerLongClick) {
                    onLongClick(view)
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mCanTriggerLongClick = true
                        downRawX = event.rawX
                        downRawY = event.rawY
                        downX = downRawX - view.x
                        downY = downRawY - view.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (Math.abs(event.rawX - downRawX) > 8 || Math.abs(event.rawY - downRawY) > 8)
                            mCanTriggerLongClick = false
                        getControlLayoutParent().adaptPanelPosition()
                        snapAndAlign(
                            MathUtils.clamp(event.rawX - downX, 0f, (CallbackBridge.physicalWidth - view.width).toFloat()),
                            MathUtils.clamp(event.rawY - downY, 0f, (CallbackBridge.physicalHeight - view.height).toFloat())
                        )
                    }
                }
                return true
            }
        })
    }

    /** レイアウト変更時の挙動を注入します。 */
    fun injectLayoutParamBehavior() {
        controlView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            properties.setWidth((right - left).toFloat())
            properties.setHeight((bottom - top).toFloat())
            setBackground()
            controlView.x = controlView.x
            controlView.y = controlView.y
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (getControlLayoutParent().isModifiable) {
            getControlLayoutParent().editControlButton(this)
            getControlLayoutParent().mActionRow?.setFollowedButton(this)
        }
        return true
    }

    companion object {
        /** @return スナップ判定の距離（dp→px） */
        fun getSnapDistance(): Float {
            return Tools.dpToPx(AllSettings.buttonSnappingDistance.getValue().toFloat())
        }

        /** @return マージン距離（2dp→px） */
        fun getMarginDistance(): Float {
            return Tools.dpToPx(2f)
        }
    }
}