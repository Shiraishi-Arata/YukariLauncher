package com.arata.yukarilauncher.value

import androidx.annotation.Keep
import com.arata.yukarilauncher.value.JMinecraftVersionList.Arguments.ArgValue.ArgRules

@Keep
class DependentLibrary {
    var rules: Array<ArgRules>? = null
    var name: String? = null
    var downloads: LibraryDownloads? = null
    var url: String? = null

    @Keep
    class LibraryDownloads(val artifact: MinecraftLibraryArtifact)
}