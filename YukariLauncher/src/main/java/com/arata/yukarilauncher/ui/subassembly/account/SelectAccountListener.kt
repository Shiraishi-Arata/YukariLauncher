package com.arata.yukarilauncher.ui.subassembly.account

import com.arata.yukarilauncher.value.MinecraftAccount

/**
 * アカウント選択時のコールバックリスナー
 */
interface SelectAccountListener {
    /**
     * アカウントが選択された
     */
    fun onSelect(account: MinecraftAccount)
}