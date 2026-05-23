package com.arata.yukarilauncher.event.sticky

import com.arata.yukarilauncher.feature.version.install.Addon
import com.arata.yukarilauncher.feature.version.install.InstallTask

/**
 * インストールタスクが選択されたときに通知するイベント
 * @param addon どのアドオンのインストールタスクか
 * @param selectedVersion 選択されたバージョン
 * @param task 選択されたタスク
 * @see com.arata.yukarilauncher.feature.version.install.Addon
 */
class SelectInstallTaskEvent(val addon: Addon, val selectedVersion: String, val task: InstallTask)
