package com.arata.yukarilauncher.feature.download.platform.update

data class ModUpdate(
    val modId: String,
    val modName: String,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String,
    val needsUpdate: Boolean
)
