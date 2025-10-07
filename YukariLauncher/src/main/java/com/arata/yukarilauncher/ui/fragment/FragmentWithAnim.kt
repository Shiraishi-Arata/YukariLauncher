package com.arata.yukarilauncher.ui.fragment

import com.arata.anim.AnimPlayer
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.anim.SlideAnimation

abstract class FragmentWithAnim : BaseFragment, SlideAnimation {
    private var animPlayer: AnimPlayer = AnimPlayer()

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    override fun onStart() {
        super.onStart()
        slideIn()
    }

    fun slideIn() {
        playAnimation { slideIn(it) }
    }

    fun slideOut() {
        playAnimation { slideOut(it) }
    }

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
