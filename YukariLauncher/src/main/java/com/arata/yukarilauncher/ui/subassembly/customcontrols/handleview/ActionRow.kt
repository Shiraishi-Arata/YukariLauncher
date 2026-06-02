package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.arata.yukarilauncher.Tools
import androidx.core.math.MathUtils
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/**
 * 編集モード時に選択されたボタンの近くに表示されるアクションボタン行。
 * 削除、複製、サブボタン追加の各ボタンを横並びに配置します。
 */
class ActionRow : LinearLayout {
    companion object {
        /** 左側に配置。 */
        const val SIDE_LEFT = 0x0
        /** 上側に配置。 */
        const val SIDE_TOP = 0x1
        /** 右側に配置。 */
        const val SIDE_RIGHT = 0x2
        /** 下側に配置。 */
        const val SIDE_BOTTOM = 0x3
        /** 自動配置。 */
        const val SIDE_AUTO = 0x4
    }

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }

    val mFollowedViewListener = ViewTreeObserver.OnPreDrawListener {
        if (mFollowedView == null || !mFollowedView!!.isShown) {
            hide()
            return@OnPreDrawListener true
        }
        setNewPosition()
        true
    }

    private val actionButtons = arrayOfNulls<ActionButtonInterface>(3)
    private var mFollowedView: View? = null
    private val mSide = SIDE_AUTO

    private fun init() {
        translationZ = 11f
        visibility = GONE
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelOffset(R.dimen._40sdp)
        )

        actionButtons[0] = DeleteButton(context)
        actionButtons[1] = CloneButton(context)
        actionButtons[2] = AddSubButton(context)

        for (buttonInterface in actionButtons) {
            val button = buttonInterface as View
            addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        elevation = 5f
    }

    /**
     * フォローするボタンを設定します。
     * @param controlInterface フォローするコントロールインターフェース
     */
    fun setFollowedButton(controlInterface: ControlInterface?) {
        mFollowedView?.let {
            it.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)
        }

        for (buttonInterface in actionButtons) {
            buttonInterface!!.setFollowedView(controlInterface)
            (buttonInterface as View).visibility = if (buttonInterface.shouldBeVisible()) VISIBLE else GONE
        }

        visibility = VISIBLE
        mFollowedView = controlInterface as? View
        mFollowedView?.let {
            it.viewTreeObserver.addOnPreDrawListener(mFollowedViewListener)
        }
    }

    /**
     * @param side 配置する側
     * @return X座標
     */
    private fun getXPosition(side: Int): Float {
        return when (side) {
            SIDE_LEFT -> mFollowedView!!.x - width
            SIDE_RIGHT -> mFollowedView!!.x + mFollowedView!!.width
            else -> mFollowedView!!.x + mFollowedView!!.width / 2f - width / 2f
        }
    }

    /**
     * @param side 配置する側
     * @return Y座標
     */
    private fun getYPosition(side: Int): Float {
        return when (side) {
            SIDE_TOP -> mFollowedView!!.y - height
            SIDE_BOTTOM -> mFollowedView!!.y + mFollowedView!!.height
            else -> mFollowedView!!.y + mFollowedView!!.height / 2f - height / 2f
        }
    }

    /** 現在の位置を再計算して設定します。 */
    private fun setNewPosition() {
        if (mFollowedView == null) return
        val side = pickSide()
        x = MathUtils.clamp(getXPosition(side), 0f, (Tools.currentDisplayMetrics.widthPixels - width).toFloat())
        y = getYPosition(side)
    }

    /** @return 最適な配置側を決定 */
    private fun pickSide(): Int {
        if (mFollowedView == null) return mSide
        if (mSide != SIDE_AUTO) return mSide

        val parent = mFollowedView!!.parent as? ViewGroup ?: return mSide

        var side = SIDE_TOP
        val futurePos = getYPosition(side)
        if (futurePos + height > parent.height + height / 2f) {
            side = SIDE_TOP
        } else if (futurePos < -height / 2f) {
            side = SIDE_BOTTOM
        }
        return side
    }

    /** アクション行を非表示にします。 */
    fun hide() {
        mFollowedView?.let {
            it.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)
        }
        visibility = GONE
    }
}