package com.arata.anim.animations.bounce

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import com.arata.anim.animations.BaseAnimator

/**
 * 拡大しながら出現するバウンスアニメーション
 * 透明度を0→0.8→1に変化させ、スケールを0.7→1.03→0.97→1と拡大する
 */
class BounceEnlargeAnimator: BaseAnimator() {
    /** バウンス拡大アニメーターを生成する */
    override fun getAnimators(target: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(target, "alpha", 0f, 0.8f, 1f, 1f),
            ObjectAnimator.ofFloat(target, "scaleX", 0.7f, 1.03f, 0.97f, 1f),
            ObjectAnimator.ofFloat(target, "scaleY", 0.7f, 1.03f, 0.97f, 1f))
    }
}