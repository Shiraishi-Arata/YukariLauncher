package com.arata.yukarilauncher.feature.download.install

import java.io.File

/**
 * interfaceする
 */
fun interface OnFileDownloadedListener {
/**
 * onEndedする
 */
    fun onEnded(file: File)
}