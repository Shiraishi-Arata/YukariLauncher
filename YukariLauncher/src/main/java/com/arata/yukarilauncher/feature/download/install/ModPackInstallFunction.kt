package com.arata.yukarilauncher.feature.download.install

import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper
import java.io.File
import java.io.IOException

fun interface ModPackInstallFunction {
    @Throws(IOException::class)
    fun install(modpackFile: File, targetPath: File): ModLoaderWrapper?
}