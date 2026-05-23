package com.arata.yukarilauncher.ui.layout

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.arata.yukarilauncher.R

/**
 * アニメーション効果付きのRelativeLayout。
 * タップ時のスケールアニメーションとリップルエフェクトを提供する。
 */
@SuppressLint("Recycle")
class AnimRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    /**
     * 初期化時にスケールアニメーションとリップルエフェクトを設定する。
     */
    init {
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
        if (context.obtainStyledAttributes(attrs, R.styleable.AnimRelativeLayout).getBoolean(R.styleable.AnimRelativeLayout_ripple_for_relative, false)) {
            setRipple()
        }
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
            background,
            null
        )

        background = rippleDrawable
    }
}
