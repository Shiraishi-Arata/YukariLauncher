package com.arata.anim.animations

import android.animation.Animator
import android.view.View

/**
 * すべてのアニメーターの基底となる抽象クラス
 * 各アニメーションはこのクラスを継承し、getAnimators を実装する
 */
abstract class BaseAnimator {
    /**
     * 対象のViewに対するアニメーター配列を生成する
     * @param target アニメーションを適用するView
     * @return アニメーターの配列
     */
    abstract fun getAnimators(target: View): Array<Animator>
}