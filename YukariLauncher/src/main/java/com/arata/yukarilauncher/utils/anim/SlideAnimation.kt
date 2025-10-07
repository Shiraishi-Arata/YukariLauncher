package com.arata.yukarilauncher.utils.anim

import com.arata.anim.AnimPlayer

interface SlideAnimation {
    fun slideIn(animPlayer: AnimPlayer)
    fun slideOut(animPlayer: AnimPlayer)
}