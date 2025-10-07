package com.arata.yukarilauncher.feature.download.platform.update

import android.app.AlertDialog
import android.content.Context
import java.io.File

object UpdateResultDialog {
    fun show(context: Context, updates: List<ModUpdate>, versionDir: File) {
        val message = updates.joinToString("\n") { "${it.modName} → ${it.latestVersion}" }

        AlertDialog.Builder(context)
            .setTitle("Mod Updates Available")
            .setMessage(message)
            .setPositiveButton("Update All") { _, _ ->
                updates.forEach { update ->
                    ModDownloader.download(
                        url = update.downloadUrl,
                        modName = "${update.modName}-${update.latestVersion}.jar",
                        versionDir = versionDir
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        }
    }
