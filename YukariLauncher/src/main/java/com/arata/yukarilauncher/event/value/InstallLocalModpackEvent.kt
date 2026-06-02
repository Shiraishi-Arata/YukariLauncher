package com.arata.yukarilauncher.event.value

import com.arata.yukarilauncher.feature.mod.modpack.install.InstallExtra

/**
 * ローカルModpackのインストールが要求されたことを通知するイベント
 * @param installExtra インストールに必要な追加情報
 */
data class InstallLocalModpackEvent(val installExtra: InstallExtra)