package com.arata.yukarilauncher.feature.download.platform.update

import java.io.File

data class ModUpdate(
    val modId: String,
    val modName: String,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String,
    val fileName: String,
    val needsUpdate: Boolean,
    val originalFile: File? = null
)