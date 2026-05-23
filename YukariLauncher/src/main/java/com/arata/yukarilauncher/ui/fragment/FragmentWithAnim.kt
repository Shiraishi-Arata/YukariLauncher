package com.arata.yukarilauncher.ui.fragment

import com.arata.anim.AnimPlayer
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.anim.SlideAnimation

/**
 * アニメーション付きフラグメントの基底クラス
 */
abstract class FragmentWithAnim : BaseFragment, SlideAnimation {
    private var animPlayer: AnimPlayer = AnimPlayer()

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    /**
     * フラグメント開始時にスライドインアニメーションを再生します。
     */
    override fun onStart() {
        super.onStart()
        slideIn()
    }

    /**
     * スライドインアニメーションを再生する
     */
    fun slideIn() {
        playAnimation { slideIn(it) }
    }

    /**
     * スライドアウトアニメーションを再生する
     */
    fun slideOut() {
        playAnimation { slideOut(it) }
    }

    /**
     * アニメーション設定が有効な場合のみ再生する
     */
    private fun playAnimation(animationAction: (AnimPlayer) -> Unit) {
        if (AllSettings.animation.getValue()) {
            animPlayer.clearEntries()
            animPlayer.apply {
                animationAction(this)
                start()
            }
        }
    }
}
