package com.arata.anim

/**
 * アニメーション完了時に呼び出されるコールバックインターフェース
 * 関数型インターフェースとして定義され、ラムダ式での使用が可能
 */
fun interface AnimCallback {
    /** コールバックを実行する */
    fun call()
}