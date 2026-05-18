package com.arata.yukarilauncher.ui.fragment.download.addon

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.mod.modloader.FabricLikeUtils

class DownloadFabricFragment : DownloadFabricLikeFragment(FabricLikeUtils.FABRIC_UTILS, R.drawable.ic_fabric) {
    companion object {
        const val TAG: String = "DownloadFabricFragment"
    }
}