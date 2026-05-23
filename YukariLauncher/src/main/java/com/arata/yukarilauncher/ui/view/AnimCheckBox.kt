package com.arata.yukarilauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.arata.yukarilauncher.R

/**
 * アニメーション効果付きのCheckBox。
 * タップ時のスケールアニメーションを提供する。
 */
class AnimCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.checkboxStyle
) : AppCompatCheckBox(context, attrs, defStyleAttr) {
    /**
     * 初期化時にスケールアニメーションを設定する。
     */
    init {
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
    }

    /**
     * レイアウト完了後にピボットを中心に設定する。
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post {
            pivotX = width / 2f
            pivotY = height / 2f
        }
    }
}
