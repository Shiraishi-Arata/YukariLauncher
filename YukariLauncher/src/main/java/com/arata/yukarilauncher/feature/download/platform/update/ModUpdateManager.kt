package com.arata.yukarilauncher.feature.download.platform.update

import android.content.Context
import com.arata.yukarilauncher.feature.download.platform.curseforge.update.CurseForgeUpdateHelper
import com.arata.yukarilauncher.feature.download.platform.modrinth.update.ModrinthUpdateHelper
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import java.io.File

object ModUpdateManager {

    /**
     * インストール済みModのアップデートを確認する
     * @param context コンテキスト
     * @param modsDir Modディレクトリ
     * @param minecraftVersion Minecraftのバージョン
     * @param selectedLoader 選択されたローダー
     * @param onProgress 進捗コールバック（現在数、合計数、Mod名）
     * @param onComplete 完了コールバック（アップデートリスト）
     * @param onError エラーコールバック
     */
/**
 * checkUpdatesする
 */
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
                                // プライマリ: SHA-1ハッシュで解決 — fabric.mod.jsonの"id"がModrinthのスラグと一致しなくても動作
                                val byHash = mod.sha1?.let { sha1 ->
                                    Logging.i("ModUpdate", "Trying hash lookup for ${mod.modName} (sha1=$sha1)")
                                    ModrinthUpdateHelper.checkUpdateByHash(
                                        sha1 = sha1,
                                        fallbackModName = mod.modName,
                                        minecraftVersion = minecraftVersion,
                                        loader = targetLoader
                                    )
                                }

                                // フォールバック: スラグ/IDベースの検索（IDが一致しないと失敗する可能性あり）
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

                                // プライマリ: Modrinthでハッシュ検索
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

                                // フォールバック1: Modrinthスラグ検索
                                Logging.i("ModUpdate", "Hash lookup missed, trying Modrinth slug for ${mod.modName}")
                                var result = ModrinthUpdateHelper.checkUpdate(
                                    projectIdOrSlug = mod.modId,
                                    currentVersion = mod.version,
                                    minecraftVersion = minecraftVersion,
                                    loader = loaderKey,
                                    currentFileName = mod.file.name
                                )

                                // フォールバック2: CurseForge（数値IDのみ）
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

    /**
     * アップデートを適用する（ファイルのダウンロードと置き換え）
     * @param context コンテキスト
     * @param updates 適用するアップデートのリスト
     * @param gameDir ゲームディレクトリ
     * @param onProgress 進捗コールバック（現在数、合計数、ファイル名、パーセンテージ）
     * @param onComplete 完了コールバック
     * @param onError エラーコールバック
     */
/**
 * applyUpdatesする
 */
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
                        // 別のファイルであれば古いファイルを削除
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
                    // ダウンロード成功後に古いファイルを削除
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