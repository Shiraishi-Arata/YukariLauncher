package com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlDrawerData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup
import java.util.ArrayList

/**
 * コントロールドロワーのビュークラス。
 * 複数のサブボタンを格納し、指定された方向に展開表示します。
 */
@SuppressLint("ViewConstructor")
class ControlDrawer : ControlButton {
    /** このドロワーに含まれるサブボタンのリスト。 */
    val buttons: ArrayList<ControlSubButton>
    /** このドロワーのデータ。 */
    lateinit var drawerData: ControlDrawerData
    /** 親レイアウトへの参照。 */
    val parentLayout: ControlLayout
    /** サブボタンを表示するかどうか。 */
    var areButtonsVisible: Boolean
    /** ドロワーが完全に初期化されたかどうか（[drawerData]を含む）。 */
    private var mDrawerInitialized = false

    constructor(layout: ControlLayout, drawerData: ControlDrawerData) : super(layout, drawerData.properties) {
        buttons = ArrayList(drawerData.buttonProperties.size)
        parentLayout = layout
        this.drawerData = drawerData
        mDrawerInitialized = true
        areButtonsVisible = layout.isModifiable
    }

    /**
     * プロパティデータからサブボタンを追加します。
     * @param properties 追加するサブボタンのプロパティ
     */
    fun addButton(properties: ControlData) {
        addButton(ControlSubButton(parentLayout, properties, this))
    }

    /**
     * サブボタンを追加します。
     * @param button 追加するサブボタン
     */
    fun addButton(button: ControlSubButton) {
        buttons.add(button)
        syncButtons()
        setControlButtonVisibility(button, areButtonsVisible)
    }

    /**
     * サブボタンの表示状態を設定します。
     * @param button 対象のサブボタン
     * @param isVisible 表示する場合はtrue
     */
    private fun setControlButtonVisibility(button: ControlButton, isVisible: Boolean) {
        button.controlView.visibility = if (isVisible) VISIBLE else GONE
    }

    /** サブボタンの表示/非表示を切り替えます。 */
    private fun switchButtonVisibility() {
        areButtonsVisible = !areButtonsVisible
        val visibility = if (areButtonsVisible) VISIBLE else GONE
        for (button in buttons) {
            button.controlView.visibility = visibility
        }
    }

    /** サブボタンをドロワーの方向に合わせて整列します。 */
    private fun alignButtons() {
        if (!::drawerData.isInitialized) return
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        val margin = ControlInterface.getMarginDistance().toInt()

        for (i in buttons.indices) {
            when (drawerData.orientation) {
                ControlDrawerData.Orientation.RIGHT -> {
                    buttons[i].setDynamicX(generateDynamicX(x + (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }
                ControlDrawerData.Orientation.LEFT -> {
                    buttons[i].setDynamicX(generateDynamicX(x - (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }
                ControlDrawerData.Orientation.UP -> {
                    buttons[i].setDynamicY(generateDynamicY(y - (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }
                ControlDrawerData.Orientation.DOWN -> {
                    buttons[i].setDynamicY(generateDynamicY(y + (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }
                else -> {}
            }
            buttons[i].updateProperties()
        }
    }

    /** サブボタンのサイズをドロワー本体に合わせます。 */
    private fun resizeButtons() {
        if (!::drawerData.isInitialized) return
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        for (subButton in buttons) {
            subButton.mProperties.setWidth(mProperties.getWidth())
            subButton.mProperties.setHeight(mProperties.getHeight())
            subButton.updateProperties()
        }
    }

    /** サブボタンの整列とリサイズを同期します。 */
    fun syncButtons() {
        alignButtons()
        resizeButtons()
    }

    /**
     * 指定されたボタンがこのドロワーの子かどうか判定します。
     * @param button 判定するボタン
     * @return 子の場合はtrue
     */
    fun containsChild(button: ControlInterface): Boolean {
        for (childButton in buttons) {
            if (childButton === button) return true
        }
        return false
    }

    override fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        val data = super.preProcessProperties(properties, layout)
        data.isHideable = true
        return data
    }

    override fun setVisible(isVisible: Boolean) {
        val visibility = if (isVisible) VISIBLE else GONE
        this.visibility = visibility
        if (visibility == GONE || areButtonsVisible) {
            for (button in buttons) {
                button.controlView.visibility = if (isVisible) VISIBLE else if (!mProperties.isHideable && visibility == GONE) VISIBLE else View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mControlLayout.isModifiable) {
            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> switchButtonVisibility()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun setX(x: Float) {
        super.setX(x)
        alignButtons()
    }

    override fun setY(y: Float) {
        super.setY(y)
        alignButtons()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        super.setLayoutParams(params)
        syncButtons()
    }

    override fun canSnap(button: ControlInterface): Boolean {
        return super.canSnap(button) && !containsChild(button)
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadValues(drawerData)
    }

    override fun cloneButton() {
        val cloneData = ControlDrawerData(drawerData)
        cloneData.properties.dynamicX = "0.5 * \${screen_width}"
        cloneData.properties.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addDrawer(cloneData)
    }

    override fun removeButton() {
        for (subButton in buttons) {
            parentLayout.removeView(subButton)
        }
        parentLayout.layout!!.mDrawerDataList!!.remove(drawerData)
        parentLayout.removeView(this)
    }
}
