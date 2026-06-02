package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.view.AnimButton
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlData
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlDrawer
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/** ドロワーにサブボタンを追加するアクションボタン。 */
@SuppressLint("AppCompatCustomView")
class AddSubButton : AnimButton, ActionButtonInterface {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }

    override fun init() {
        setOnClickListener(this)
        setText(R.string.customctrl_addsubbutton)
    }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton is ControlDrawer
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton is ControlDrawer) {
            (mCurrentlySelectedButton as ControlDrawer).parentLayout.addSubButton(
                mCurrentlySelectedButton as ControlDrawer,
                ControlData(context.getString(R.string.controls_add_control_button))
            )
        }
    }
}