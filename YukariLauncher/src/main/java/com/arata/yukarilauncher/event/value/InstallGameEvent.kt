package com.arata.yukarilauncher.event.value

import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.feature.version.install.InstallTaskItem

/**
 * インストールタスクが開始されたときに通知するイベント
 * @see com.arata.yukarilauncher.ui.fragment.InstallGameFragment
 * @param minecraftVersion Minecraftのバージョン
 * @param customVersionName カスタムバージョンフォルダ名
 * @param taskMap インストールタスクのマップ
 */
class InstallGameEvent(
    val minecraftVersion: String,
    val customVersionName: String,
    val taskMap: Map<Addon, InstallTaskItem>
)