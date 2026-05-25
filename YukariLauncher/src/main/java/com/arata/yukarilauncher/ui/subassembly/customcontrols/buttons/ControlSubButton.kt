package com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlDrawerData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlLayout
import com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview.EditControlPopup

/**
 * ドロワー内に配置されるサブボタンのビュークラス。
 * 親ドロワーの向きに応じてサイズと動作が制約されます。
 */
@SuppressLint("ViewConstructor")
class ControlSubButton : ControlButton {
    /** このサブボタンが所属する親ドロワー。 */
    lateinit var parentDrawer: ControlDrawer

    constructor(layout: ControlLayout, properties: ControlData, parentDrawer: ControlDrawer) : super(layout, properties) {
        this.parentDrawer = parentDrawer
        filterProperties()
    }

    /** 親ドロワーの向きに応じてプロパティをフィルタリングします。 */
    private fun filterProperties() {
        if (parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            mProperties.setHeight(parentDrawer.properties.getHeight())
            mProperties.setWidth(parentDrawer.properties.getWidth())
        }
        setProperties(mProperties, false)
    }

    override fun setVisible(isVisible: Boolean) {}

    override fun onGrabState(isGrabbing: Boolean) {}

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (::parentDrawer.isInitialized && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            params.width = parentDrawer.mProperties.getWidth().toInt()
            params.height = parentDrawer.mProperties.getHeight().toInt()
        }
        super.setLayoutParams(params)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mControlLayout.isModifiable || parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE) {
            return super.onTouchEvent(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            onLongClick(this)
        }
        return true
    }

    override fun cloneButton() {
        val cloneData = ControlData(properties)
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addSubButton(parentDrawer, cloneData)
    }

    override fun removeButton() {
        parentDrawer.drawerData.buttonProperties.remove(properties)
        parentDrawer.drawerData.buttonProperties.remove(properties)
        parentDrawer.buttons.remove(this)
        parentDrawer.syncButtons()
        super.removeButton()
    }

    override fun snapAndAlign(x: Float, y: Float) {
        if (parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE)
            super.snapAndAlign(x, y)
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadSubButtonValues(properties, parentDrawer.drawerData.orientation)
    }
}
