package com.arata.yukarilauncher.feature.login

import com.arata.yukarilauncher.value.MinecraftAccount

/** ログイン完了リスナー。 */
interface DoneListener {
    /** ログインが完了したときに呼び出される。 @param account ログインアカウント */
    fun onLoginDone(account: MinecraftAccount)
}
