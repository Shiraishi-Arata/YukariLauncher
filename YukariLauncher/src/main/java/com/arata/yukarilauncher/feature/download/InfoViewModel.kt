package com.arata.yukarilauncher.feature.download

import androidx.lifecycle.ViewModel
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper

/**
 * ダウンロード情報画面のViewModel。
 * 現在選択されているプラットフォームヘルパーと情報アイテムを保持する。
 */
class InfoViewModel : ViewModel() {
    var platformHelper: AbstractPlatformHelper? = null
    var infoItem: InfoItem? = null
}
