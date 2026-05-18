package com.arata.yukarilauncher.ui.fragment.download.addon

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.mod.modloader.FabricLikeUtils

class DownloadQuiltFragment : DownloadFabricLikeFragment(FabricLikeUtils.QUILT_UTILS, R.drawable.ic_quilt) {
    companion object {
        const val TAG: String = "DownloadQuiltFragment"
    }
}