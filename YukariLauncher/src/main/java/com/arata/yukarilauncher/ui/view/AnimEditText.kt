package com.arata.yukarilauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim

/**
 * アニメーション効果付きのEditText。
 * タップ時のスケールアニメーションとエラー時のシェイクアニメーションを提供する。
 */
class AnimEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
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

    /**
     * エラーメッセージ設定時にシェイクアニメーションを実行する。
     * @param error エラーメッセージ
     */
    override fun setError(error: CharSequence?) {
        super.setError(error)
        error?.let { setErrorAnim() }
    }

    /**
     * エラーメッセージとアイコン設定時にシェイクアニメーションを実行する。
     * @param error エラーメッセージ
     * @param icon エラーアイコン
     */
    override fun setError(error: CharSequence?, icon: Drawable?) {
        super.setError(error, icon)
        error?.let { setErrorAnim() }
    }

    /**
     * シェイクアニメーションを実行する。
     */
    private fun setErrorAnim() {
        setViewAnim(this, Animations.Shake)
    }
}