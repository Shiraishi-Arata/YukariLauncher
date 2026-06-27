package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import android.os.Build
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.http.DownloadUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ComponentUpdateChecker(private val context: Context) {
    companion object {
        private const val REMOTE_VERSION_URL = "https://raw.githubusercontent.com/Shiraishi-Arata/Yukari-Fixes/main/version"
        private const val VERSION_FILE = "component_version"
    }

    private val versionFile get() = File(PathManager.DIR_DATA, VERSION_FILE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun checkForUpdate() {
        scope.launch {
            try {
                val remoteText = DownloadUtils.downloadString(REMOTE_VERSION_URL)
                val remote = parseVersions(remoteText)
                if (remote.isEmpty()) return@launch

                scanLocalVersions()
                val local = readVersions()
                if (local.isEmpty()) return@launch

                val outdatedComponents = Components.entries.filter { c ->
                    c.downloadUrl != null && remote[componentKey(c)]?.let { r ->
                        val l = local[componentKey(c)]
                        l != null && r > l
                    } == true
                }

                val outdatedJres = Jre.entries.filter { j ->
                    remote[jreKey(j)]?.let { r ->
                        val l = local[jreKey(j)]
                        l != null && r > l
                    } == true
                }

                if (outdatedComponents.isNotEmpty() || outdatedJres.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog()
                    }
                }
            } catch (e: Exception) {
                Logging.e("ComponentUpdate", "Check failed: ${Tools.printToString(e)}")
            }
        }
    }

    private fun readVersions(): Map<String, Long> {
        if (!versionFile.exists()) return emptyMap()
        return try {
            parseVersions(versionFile.readText())
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeWrapFile(versions: Map<String, Long>) {
        try {
            versionFile.parentFile?.mkdirs()
            val content = versions.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value}" }
            versionFile.writeText(content)
        } catch (_: Exception) {}
    }

    private fun scanLocalVersions() {
        val map = mutableMapOf<String, Long>()

        for (component in Components.entries) {
            if (component.downloadUrl == null) continue
            val rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            val targetDir = File(rootDir, component.component)
            val versionFile = File(targetDir, "version")
            if (versionFile.exists()) {
                val v = try { versionFile.readText().trim().toLongOrNull() } catch (_: Exception) { null }
                map[componentKey(component)] = v ?: 0L
            }
        }

        for (jre in Jre.entries) {
            val targetDir = File(PathManager.DIR_MULTIRT_HOME, jre.jreName)
            val vFile = File(targetDir, "pojav_version")
            if (vFile.exists()) {
                val raw = try { vFile.readText().trim() } catch (_: Exception) { null }
                val v = raw?.toLongOrNull()
                map[jreKey(jre)] = v ?: 0L
            }
        }

        writeWrapFile(map)
    }

    private fun componentKey(component: Components): String {
        return component.component.replace("/", "_").replace(".", "_")
    }

    private fun jreKey(jre: Jre): String {
        return jre.name.lowercase()
    }

    private fun parseVersions(text: String): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().toLongOrNull()
                    if (key.isNotEmpty() && value != null) {
                        map[key] = value
                    }
                }
            }
        }
        return map
    }

    private fun showUpdateDialog() {
        TipDialog.Builder(context)
            .setTitle(R.string.component_update_title)
            .setMessage(R.string.component_update_message)
            .setConfirm(R.string.update_dialog_yes)
            .setCancel(R.string.generic_cancel)
            .setConfirmClickListener { startUpdate() }
            .setCancelable(false)
            .showDialog()
    }

    private fun startUpdate() {
        scope.launch {
            try {
                val remoteText = DownloadUtils.downloadString(REMOTE_VERSION_URL)
                val remote = parseVersions(remoteText)
                val cacheDir = PathManager.DIR_CACHE
                cacheDir.mkdirs()

                val allUpdates = Components.entries.filter { it.downloadUrl != null && remote[componentKey(it)] != null } +
                    Jre.entries.filter { remote[jreKey(it)] != null }
                val total = allUpdates.size
                if (total == 0) return@launch

                ProgressKeeper.submitProgress("component_update", 0, R.string.component_update_title)

                var done = 0
                for (component in Components.entries) {
                    val url = component.downloadUrl ?: continue
                    val key = componentKey(component)
                    if (remote[key] == null) continue

                    val rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
                    val targetDir = File(rootDir, component.component)

                    val tempZip = File(cacheDir, "${component.component.replace("/", "_")}.zip")
                    DownloadUtils.downloadFile(url, tempZip)
                    if (targetDir.exists()) targetDir.deleteRecursively()
                    targetDir.mkdirs()
                    extractComponentZip(tempZip, targetDir, component.component.substringAfterLast("/"))
                    tempZip.delete()
                    done++
                    ProgressKeeper.submitProgress("component_update", done * 100 / total, R.string.component_update_title, done, total)
                }

                for (jre in Jre.entries) {
                    val key = jreKey(jre)
                    if (remote[key] == null) continue

                    val tempZip = File(cacheDir, "${jre.jrePath}.zip")
                    DownloadUtils.downloadFileMonitored(jre.downloadUrl, tempZip, null, object : Tools.DownloaderFeedback {
                        override fun updateProgress(curr: Long, max: Long) {
                            val p = if (max > 0) ((done * 100 + (curr.toFloat() / max * 100).toInt()) / total).coerceIn(0, 99) else 0
                            ProgressKeeper.submitProgress("component_update", p, R.string.component_update_title, done + 1, total)
                        }
                    })
                    val (universalFile, binFile) = extractJreArchives(tempZip, cacheDir, jre)
                    MultiRTUtils.installRuntimeNamedBinpack(
                        FileInputStream(universalFile),
                        FileInputStream(binFile),
                        jre.jreName,
                        java.lang.Long.toString(System.currentTimeMillis())
                    )
                    MultiRTUtils.postPrepare(jre.jreName)
                    val runtimeDir = File(PathManager.DIR_MULTIRT_HOME, jre.jreName)
                    runtimeDir.mkdirs()
                    File(runtimeDir, "download_url").writeText(jre.downloadUrl)
                    universalFile.delete()
                    binFile.delete()
                    tempZip.delete()
                    done++
                    ProgressKeeper.submitProgress("component_update", done * 100 / total, R.string.component_update_title, done, total)
                }

                ProgressKeeper.submitProgress("component_update", -1, -1)

                withContext(Dispatchers.Main) {
                    TipDialog.Builder(context)
                        .setTitle(R.string.component_update_done_title)
                        .setMessage(R.string.component_update_done_message)
                        .setConfirm(R.string.generic_ok)
                        .setShowCancel(false)
                        .showDialog()
                }
            } catch (e: Exception) {
                Logging.e("ComponentUpdate", "Update failed: ${Tools.printToString(e)}")
                withContext(Dispatchers.Main) {
                    TipDialog.Builder(context)
                        .setTitle(R.string.generic_error)
                        .setMessage(context.getString(R.string.component_update_failed_message, Tools.printToString(e)))
                        .setConfirm(R.string.generic_ok)
                        .setShowCancel(false)
                        .showDialog()
                }
            }
        }
    }

    private fun extractComponentZip(zipFile: File, targetDir: File, zipRoot: String) {
        val supportedAbis = Build.SUPPORTED_ABIS.toSet()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val relativePath = entryName.removePrefix("$zipRoot/")
                if (!entry.isDirectory && relativePath != entryName && relativePath.isNotEmpty()) {
                    val shouldExtract = if (relativePath.startsWith("native/")) {
                        val abi = relativePath.removePrefix("native/").substringBefore("/")
                        abi in supportedAbis
                    } else true
                    if (shouldExtract) {
                        val outputFile = File(targetDir, relativePath)
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractJreArchives(zipFile: File, cacheDir: File, jre: Jre): Pair<File, File> {
        val archName = Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)
        val binName = "bin-$archName.tar.xz"
        val universalFile = File(cacheDir, "${jre.jrePath}_universal.tar.xz")
        val binFile = File(cacheDir, "${jre.jrePath}_$binName")

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val relativePath = entry.name.removePrefix("${jre.jrePath}/")
                    if (relativePath != entry.name) {
                        when (relativePath) {
                            "universal.tar.xz" -> FileOutputStream(universalFile).use { zis.copyTo(it) }
                            binName -> FileOutputStream(binFile).use { zis.copyTo(it) }
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
        return Pair(universalFile, binFile)
    }
}
