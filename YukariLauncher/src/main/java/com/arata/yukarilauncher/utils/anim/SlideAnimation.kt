package com.arata.yukarilauncher.utils.anim

import com.arata.anim.AnimPlayer

/**
 * スライドアニメーションのインターフェース
 * ビューのスライドイン・スライドアウトを定義する
 */
interface SlideAnimation {
    /**
     * スライドインアニメーションを実行する
     */
    fun slideIn(animPlayer: AnimPlayer)
    /**
     * スライドアウトアニメーションを実行する
     */
    fun slideOut(animPlayer: AnimPlayer)
}
