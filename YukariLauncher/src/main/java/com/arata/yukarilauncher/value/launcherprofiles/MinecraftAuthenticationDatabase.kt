package com.arata.yukarilauncher.value.launcherprofiles

import androidx.annotation.Keep

@Keep
class MinecraftAuthenticationDatabase {
    var accessToken: String? = null
    var displayName: String? = null
    var username: String? = null
    var uuid: String? = null
}