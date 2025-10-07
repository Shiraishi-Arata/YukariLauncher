package com.arata.yukarilauncher.feature.download

import androidx.lifecycle.ViewModel
import com.arata.yukarilauncher.feature.download.item.InfoItem
import com.arata.yukarilauncher.feature.download.platform.AbstractPlatformHelper

class InfoViewModel : ViewModel() {
    var platformHelper: AbstractPlatformHelper? = null
    var infoItem: InfoItem? = null
}