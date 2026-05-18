package com.arata.yukarilauncher.feature.download.platform.update

import android.content.Context
import com.arata.yukarilauncher.feature.download.platform.curseforge.update.CurseForgeUpdateHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.update.ModrinthUpdateHelper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import java.io.File

object ModUpdateManager {

    fun checkUpdates(
        context: Context,
        modsDir: File,
        minecraftVersion: String,
        selectedLoader: String?,
        onProgress: (Int, Int, String) -> Unit,
        onComplete: (List<ModUpdate>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        TaskExecutors.getDefault().execute {
            try {
                Logging.i("ModUpdate", "Checking updates for Minecraft version: $minecraftVersion, selected loader: ${selectedLoader ?: "none"}")
                val installedMods = InstalledModsScanner.scan(modsDir)
                Logging.i("ModUpdate", "Found ${installedMods.size} mods")
                val updates = mutableListOf<ModUpdate>()

                installedMods.forEachIndexed { index, mod ->
                    TaskExecutors.runInUIThread {
                        onProgress(index + 1, installedMods.size, mod.modName)
                    }

                    try {
                        val targetLoader = selectedLoader
                            ?.lowercase()
                            ?.takeIf { mod.supportedLoaders.contains(it) }
                            ?: mod.loader.lowercase()
                        Logging.i(
                            "ModUpdate",
                            "Checking ${mod.modName} (${mod.modId}) loader: ${mod.loader}, selected: ${selectedLoader ?: "none"}, target: $targetLoader, version: ${mod.version}"
                        )

                        val update: ModUpdate? = when (targetLoader) {

                            "fabric", "quilt" -> {
                                // PRIMARY: resolve by SHA-1 hash — works regardless of whether the
                                // fabric.mod.json "id" matches the Modrinth project slug.
                                val byHash = mod.sha1?.let { sha1 ->
                                    Logging.i("ModUpdate", "Trying hash lookup for ${mod.modName} (sha1=$sha1)")
                                    ModrinthUpdateHelper.checkUpdateByHash(
                                        sha1 = sha1,
                                        fallbackModName = mod.modName,
                                        minecraftVersion = minecraftVersion,
                                        loader = targetLoader
                                    )
                                }

                                // FALLBACK: slug/id-based lookup (may fail if IDs don't match)
                                byHash ?: run {
                                    Logging.i("ModUpdate", "Hash lookup missed, falling back to slug for ${mod.modName}")
                                    ModrinthUpdateHelper.checkUpdate(
                                        projectIdOrSlug = mod.modId,
                                        currentVersion = mod.version,
                                        minecraftVersion = minecraftVersion,
                                        loader = targetLoader,
                                        currentFileName = mod.file.name
                                    )
                                }
                            }

                            "forge", "neoforge" -> {
                                val loaderKey = targetLoader

                                // PRIMARY: hash lookup on Modrinth
                                val byHash = mod.sha1?.let { sha1 ->
                                    Logging.i("ModUpdate", "Trying hash lookup for ${mod.modName} (sha1=$sha1)")
                                    ModrinthUpdateHelper.checkUpdateByHash(
                                        sha1 = sha1,
                                        fallbackModName = mod.modName,
                                        minecraftVersion = minecraftVersion,
                                        loader = loaderKey
                                    )
                                }
                                if (byHash != null) return@forEachIndexed run {
                                    if (byHash.needsUpdate) {
                                        Logging.i("ModUpdate", "Update available for ${mod.modName}: ${byHash.latestVersion}")
                                        updates.add(byHash.copy(originalFile = mod.file))
                                    } else {
                                        Logging.i("ModUpdate", "No update for ${mod.modName}")
                                    }
                                }

                                // FALLBACK 1: Modrinth slug lookup
                                Logging.i("ModUpdate", "Hash lookup missed, trying Modrinth slug for ${mod.modName}")
                                var result = ModrinthUpdateHelper.checkUpdate(
                                    projectIdOrSlug = mod.modId,
                                    currentVersion = mod.version,
                                    minecraftVersion = minecraftVersion,
                                    loader = loaderKey,
                                    currentFileName = mod.file.name
                                )

                                // FALLBACK 2: CurseForge (numeric IDs only)
                                if (result == null && mod.modId.toLongOrNull() != null) {
                                    Logging.i("ModUpdate", "Modrinth failed, trying CurseForge for ${mod.modName}")
                                    result = CurseForgeUpdateHelper.checkUpdate(
                                        modId = mod.modId,
                                        currentVersion = mod.version,
                                        minecraftVersion = minecraftVersion,
                                        currentFileName = mod.file.name
                                    )
                                }
                                result
                            }

                            else -> {
                                Logging.i("ModUpdate", "Unsupported loader: ${mod.loader} for ${mod.modName}")
                                null
                            }
                        }

                        if (update?.needsUpdate == true) {
                            Logging.i("ModUpdate", "Update available for ${mod.modName}: ${update.latestVersion}")
                            updates.add(update.copy(originalFile = mod.file))
                        } else {
                            Logging.i("ModUpdate", "No update for ${mod.modName}")
                        }
                    } catch (e: Exception) {
                        Logging.e("ModUpdate", "Failed to check ${mod.modName}", e)
                    }
                }

                TaskExecutors.runInUIThread {
                    onComplete(updates)
                }
            } catch (e: Exception) {
                Logging.e("ModUpdate", "Update check failed", e)
                TaskExecutors.runInUIThread {
                    onError(e)
                }
            }
        }
    }

    fun applyUpdates(
        context: Context,
        updates: List<ModUpdate>,
        gameDir: File,
        onProgress: (Int, Int, String, Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (updates.isEmpty()) {
            onComplete()
            return
        }

        TaskExecutors.getDefault().execute {
            try {
                val modsDir = File(gameDir, "mods")
                if (!modsDir.exists()) modsDir.mkdirs()

                var successCount = 0
                val total = updates.size

                updates.forEachIndexed { index, update ->
                    val targetFile = File(modsDir, update.fileName)

                    if (targetFile.exists()) {
                        successCount++
                        TaskExecutors.runInUIThread {
                            onProgress(index + 1, total, update.fileName, 100)
                        }
                        // Delete old file if it's a different file
                        update.originalFile?.takeIf { it.exists() && it != targetFile }?.delete()
                        return@forEachIndexed
                    }

                    var lastPercent = 0
                    ModDownloader.downloadWithProgress(
                        url = update.downloadUrl,
                        outputFile = targetFile,
                        onProgress = { percent ->
                            if (percent > lastPercent) {
                                lastPercent = percent
                                TaskExecutors.runInUIThread {
                                    onProgress(index + 1, total, update.fileName, percent)
                                }
                            }
                        }
                    )
                    // Delete old file after successful download
                    update.originalFile?.takeIf { it.exists() && it != targetFile }?.delete()
                    successCount++
                    Logging.i("ModUpdate", "Downloaded ${update.fileName}")
                }

                TaskExecutors.runInUIThread {
                    onComplete()
                }
            } catch (e: Exception) {
                Logging.e("ModUpdate", "Apply updates failed", e)
                TaskExecutors.runInUIThread {
                    onError(e)
                }
            }
        }
    }
}
