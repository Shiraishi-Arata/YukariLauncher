package com.arata.yukarilauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools

/**
 * アニメーション効果付きのButton。
 * タップ時のスケールアニメーション、リップルエフェクト、影を提供する。
 */
open class AnimButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {
    /**
     * 初期化時に大文字禁止、リップルエフェクト、スケールアニメーション、影を設定する。
     */
    init {
        isAllCaps = false
        setRipple()
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
        translationZ = Tools.dpToPx(4f)
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

    /**
     * リップルエフェクト（波紋効果）を背景に設定する。
     */
    private fun setRipple() {
        val rippleDrawable = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background_ripple_effect)),
            ResourcesCompat.getDrawable(resources, R.drawable.button_background, context.theme),
            null
        )

        background = rippleDrawable
    }
}
