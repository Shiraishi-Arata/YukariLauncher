package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.view.AnimButton
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/** 選択されたコントロールを複製するアクションボタン。 */
@SuppressLint("AppCompatCustomView")
class CloneButton : AnimButton, ActionButtonInterface {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }

    override fun init() {
        setOnClickListener(this)
        setText(R.string.generic_clone)
    }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun shouldBeVisible(): Boolean = mCurrentlySelectedButton != null

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton == null) return
        mCurrentlySelectedButton!!.cloneButton()
        mCurrentlySelectedButton!!.getControlLayoutParent().removeEditWindow()
    }
}
