package com.arata.yukarilauncher.feature.download.platform.update

import android.os.Handler
import android.os.Looper
import com.arata.yukarilauncher.feature.download.platform.curseforge.update.CurseForgeUpdateHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.update.ModrinthUpdateHelper
import java.io.File
import java.util.concurrent.Executors

object ModUpdateChecker {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun checkUpdatesAsync(modsDir: File, onComplete: (List<ModUpdate>) -> Unit) {
        executor.execute {
            val installedMods = InstalledModsScanner.scan(modsDir)
            val updates = mutableListOf<ModUpdate>()

            installedMods.forEach { mod ->
                // Try Modrinth first
                val modrinth = ModrinthUpdateHelper.checkUpdate(mod.modId, mod.version)
                if (modrinth?.needsUpdate == true) {
                    updates.add(modrinth)
                } else {
                    val curseforge = CurseForgeUpdateHelper.checkUpdate(mod.modId, mod.version)
                    if (curseforge?.needsUpdate == true) updates.add(curseforge)
                }
            }

            // Return results to main thread
            mainHandler.post { onComplete(updates) }
        }
    }
}
