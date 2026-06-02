package com.arata.yukarilauncher.feature.login

/** ログインエラーリスナー。 */
interface ErrorListener {
    /** ログインエラー時に呼び出される。 @param errorMessage エラー内容 */
    fun onLoginError(errorMessage: Throwable)
}