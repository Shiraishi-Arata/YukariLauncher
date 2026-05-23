package com.arata.anim.animations.bounce

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import com.arata.anim.animations.BaseAnimator

/**
 * 縮小しながら消えるバウンスアニメーション
 * 透明度を1→0に変化させ、スケールを1→1.03→0.6→0と縮小する
 */
class BounceShrinkAnimator: BaseAnimator() {
    /** バウンス縮小アニメーターを生成する */
    override fun getAnimators(target: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(target, "alpha", 1f, 0.85f, 0.5f, 0f),
            ObjectAnimator.ofFloat(target, "scaleX", 1f, 1.03f, 0.6f, 0f),
            ObjectAnimator.ofFloat(target, "scaleY", 1f, 1.03f, 0.6f, 0f))
    }
}
