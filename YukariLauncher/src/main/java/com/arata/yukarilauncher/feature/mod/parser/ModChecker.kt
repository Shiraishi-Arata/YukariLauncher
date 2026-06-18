package com.arata.yukarilauncher.feature.mod.parser

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mio.util.AndroidUtil
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.http.NetworkUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.feature.mod.ModUtils
import com.arata.yukarilauncher.plugins.FFmpegPlugin
import java.io.File
import java.net.URL
import java.util.Locale
import java.util.zip.ZipFile

class ModChecker {
    class ModCheckResult() : Parcelable {
        var hasTouchController: Boolean = false
        var hasSodiumOrEmbeddium: Boolean = false
        var hasPhysics: Boolean = false
        var hasMCEF: Boolean = false
        var hasValkyrienSkies: Boolean = false
        var hasYesSteveModel: Boolean = false
        var hasIMBlockerOrInGameIME: Boolean = false
        var hasReplayMod: Boolean = false
        var hasBorderlesswindow: Boolean = false
        var hasSable: Boolean = false
        var hasFlashBack: Boolean = false
        var hasVeil: Boolean = false

/**
 * Booleanする
 */
        /**
         * Boolean値をIntに変換する
         * @return trueの場合は1、falseの場合は0
         */
        private fun Boolean.getInt(): Int = if (this) 1 else 0
        /**
         * Int値をBooleanに変換する
         * @return 0以外の場合はtrue
         */
        private fun Int.toBoolean(): Boolean = this != 0

        /**
         * ParcelからModCheckResultを復元する
         * @param parcel 復元元のParcel
         */
        constructor(parcel: Parcel) : this() {
            hasTouchController = parcel.readInt().toBoolean()
            hasPhysics = parcel.readInt().toBoolean()
            hasMCEF = parcel.readInt().toBoolean()
            hasValkyrienSkies = parcel.readInt().toBoolean()
            hasYesSteveModel = parcel.readInt().toBoolean()
            hasIMBlockerOrInGameIME = parcel.readInt().toBoolean()
            hasReplayMod = parcel.readInt().toBoolean()
            hasBorderlesswindow = parcel.readInt().toBoolean()
            hasSable = parcel.readInt().toBoolean()
            hasFlashBack = parcel.readInt().toBoolean()
            hasVeil = parcel.readInt().toBoolean()
        }

/**
 * describeContentsする
 */
        override fun describeContents(): Int = 0

/**
 * writeToParcelする
 */
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(hasTouchController.getInt())
            dest.writeInt(hasPhysics.getInt())
            dest.writeInt(hasMCEF.getInt())
            dest.writeInt(hasValkyrienSkies.getInt())
            dest.writeInt(hasYesSteveModel.getInt())
            dest.writeInt(hasIMBlockerOrInGameIME.getInt())
            dest.writeInt(hasReplayMod.getInt())
            dest.writeInt(hasBorderlesswindow.getInt())
            dest.writeInt(hasSable.getInt())
            dest.writeInt(hasFlashBack.getInt())
            dest.writeInt(hasVeil.getInt())
        }

        companion object CREATOR : Parcelable.Creator<ModCheckResult> {
/**
 * createFromParcelする
 */
            override fun createFromParcel(parcel: Parcel): ModCheckResult {
                return ModCheckResult(parcel)
            }

/**
 * newArrayする
 */
            override fun newArray(size: Int): Array<ModCheckResult?> {
                return arrayOfNulls(size)
            }
        }
    }

/**
 * DownloadTaskする
 */
    private data class DownloadTask(
        val url: String,
        val targetFile: File,
        val modFile: File,
        val libFileName: String,
        val modFileName: String,
        val tag: String,
        val settings: AllModCheckSettings,
        val downloadingResId: Int,
        val errorResId: Int,
        val debugResId: Int
    )

    /**
     * 检查所有模组，并对一些已知的模组进行判断
     */
/**
 * スポンサー情報のチェックを実行する
 * @param listener チェック結果のコールバックリスナー
 */
    fun check(context: Context, modInfoList: List<ModInfo>, executeTask: (ModCheckResult?) -> Unit) {
        runCatching {
            initAbis()
            val modCheckSettings = mutableMapOf<AllModCheckSettings, Pair<String, String>>()

            if (modInfoList.isNotEmpty()) {
                Logger.appendToLog("Mod Perception: ${modInfoList.size} Mods parsed successfully")
            }

            val modResult = ModCheckResult()
            val downloadTasks = mutableListOf<DownloadTask>()

            modInfoList.forEach { mod ->
                when (mod.id) {
                    "touchcontroller" -> {
                        if (!modResult.hasTouchController) {
                            modResult.hasTouchController = true
                            modCheckSettings[AllModCheckSettings.TOUCH_CONTROLLER] = Pair(
                                "1",
                                context.getString(R.string.mod_check_touch_controller, mod.file.name)
                            )
                        }
                    }
                    "physicsmod" -> {
                        if (!modResult.hasPhysics) {
                            modResult.hasPhysics = true
                            val arch = AndroidUtil.getElfArchFromZip(
                                mod.file,
                                "de/fabmax/physxjni/linux/libPhysXJniBindings_64.so"
                            )
                            if (arch.isBlank() or (!Architecture.isx86Device() and arch.contains("x86"))) {
                                val targetFile = File(PathManager.DIR_MOD_LIBRARY, "libPhysXJniBindings_64.so")
                                if (!targetFile.exists()) {
                                    downloadTasks.add(
                                        DownloadTask(
                                            url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/PhysX/libPhysXJniBindings_64.so",
                                            targetFile = targetFile,
                                            modFile = mod.file,
                                            libFileName = "libPhysXJniBindings_64.so",
                                            modFileName = mod.file.name,
                                            tag = "Physics",
                                            settings = AllModCheckSettings.PHYSICS_MOD,
                                            downloadingResId = R.string.mod_check_physics_downloading,
                                            errorResId = R.string.mod_check_physics_failed,
                                            debugResId = R.string.mod_check_physics_debug
                                        )
                                    )
                                }
                            }
                        }
                    }
                    "mcef" -> {
                        if (!modResult.hasMCEF) {
                            modResult.hasMCEF = true
                            modCheckSettings[AllModCheckSettings.MCEF] = Pair(
                                "1",
                                context.getString(R.string.mod_check_mcef, mod.file.name)
                            )
                        }
                    }
                    "valkyrienskies" -> {
                        if (!modResult.hasValkyrienSkies) {
                            modResult.hasValkyrienSkies = true
                            modCheckSettings[AllModCheckSettings.VALKYRIEN_SKIES] = Pair(
                                "1",
                                context.getString(R.string.mod_check_valkyrien_skies, mod.file.name)
                            )
                        }
                    }
                    "yes_steve_model" -> {
                        if (!modResult.hasYesSteveModel) {
                            modResult.hasYesSteveModel = true
                            val defaultArch = AndroidUtil.getElfArchFromZip(
                                mod.file,
                                "META-INF/native/libysm-core.so"
                            )
                            val androidArch = AndroidUtil.getElfArchFromZip(
                                mod.file,
                                "META-INF/native/libysm-core-android.so"
                            )
                            if (defaultArch.isNotBlank() && androidArch.isBlank()) {
                                modCheckSettings[AllModCheckSettings.YES_STEVE_MODEL] = Pair(
                                    "1",
                                    context.getString(R.string.mod_check_yes_steve_model, mod.file.name)
                                )
                            }
                        }
                    }
                    "imblocker", "ingameime" -> {
                        if (!modResult.hasIMBlockerOrInGameIME) {
                            modResult.hasIMBlockerOrInGameIME = true
                            modCheckSettings[AllModCheckSettings.IM_BLOCKER] = Pair(
                                "2",
                                context.getString(R.string.mod_check_imblocker, mod.file.name)
                            )
                        }
                    }
                    "replaymod" -> {
                        if (!modResult.hasReplayMod) {
                            modResult.hasReplayMod = true
                            FFmpegPlugin.discover(context)
                            if (!FFmpegPlugin.isAvailable) {
                                modCheckSettings[AllModCheckSettings.REPLAY_MOD] = Pair(
                                    "1",
                                    context.getString(R.string.mod_check_replay_mod, mod.file.name,
                                        "https://github.com/FCL-Team/FoldCraftLauncher/releases/download/ffmpeg/Pojav.FFmpeg.Plugin.1.1.APK",
                                        "https://pan.quark.cn/s/6201574edb62"
                                    )
                                )
                            }
                        }
                    }
                    "borderlesswindow" -> {
                        if (!modResult.hasBorderlesswindow) {
                            modResult.hasBorderlesswindow = true
                            modCheckSettings[AllModCheckSettings.BORDERLESS_WINDOW] = Pair(
                                "1",
                                context.getString(R.string.mod_check_borderlesswindow, mod.file.name)
                            )
                        }
                    }
                    "sable" -> {
                        if (!modResult.hasSable) {
                            modResult.hasSable = true
                            val libFileName = "libsable_rapier-$abiTag.so"
                            val targetFile = File(PathManager.DIR_MOD_LIBRARY, libFileName)
                            if (!targetFile.exists()) {
                                downloadTasks.add(
                                    DownloadTask(
                                        url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libFileName",
                                        targetFile = targetFile,
                                        modFile = mod.file,
                                        libFileName = libFileName,
                                        modFileName = mod.file.name,
                                        tag = "Sable",
                                        settings = AllModCheckSettings.SABLE,
                                        downloadingResId = R.string.mod_check_sable_downloading,
                                        errorResId = R.string.mod_check_sable_failed,
                                        debugResId = R.string.mod_check_sable_debug
                                    )
                                )
                            }
                        }
                    }
                    "veil" -> {
                        if (!modResult.hasVeil) {
                            modResult.hasVeil = true
                            modCheckSettings[AllModCheckSettings.VEIL] = Pair(
                                "1",
                                context.getString(R.string.mod_check_veil, mod.file.name)
                            )
                        }
                    }
                    "flashback" -> {
                        if (!modResult.hasFlashBack) {
                            modResult.hasFlashBack = true
                            val libFileName = "libimgui-moulberry90-java-$abiTag.so"
                            val targetFile = File(PathManager.DIR_MOD_LIBRARY, libFileName)
                            if (!targetFile.exists()) {
                                downloadTasks.add(
                                    DownloadTask(
                                        url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libFileName",
                                        targetFile = targetFile,
                                        modFile = mod.file,
                                        libFileName = libFileName,
                                        modFileName = mod.file.name,
                                        tag = "Flashback",
                                        settings = AllModCheckSettings.FLASHBACK,
                                        downloadingResId = R.string.mod_check_axiom_downloading,
                                        errorResId = R.string.mod_check_axiom_failed,
                                        debugResId = R.string.mod_check_axiom_debug
                                    )
                                )
                            }
                        }
                    }
                }

                if (mod.file.name.matches(Regex("Axiom-.*\\.jar", RegexOption.IGNORE_CASE))) {
                    var foundMatch = false
                    ZipFile(mod.file).use { zipFile ->
                        val entries = zipFile.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val name = entry.name
                            if (!name.contains("zstd-jni-", true)) continue
                            if (!name.endsWith(".so")) continue
                            if (!matchesAbi(name)) continue

                            val versionStart = "zstd-jni-"
                            val rawIndex = name.indexOf(versionStart)
                            if (rawIndex == -1) continue

                            val startIndex = rawIndex + versionStart.length
                            val endIndex = name.indexOf(".so", startIndex)
                            if (endIndex <= startIndex) continue

                            val version = name.substring(startIndex, endIndex)
                            foundMatch = true

                            Logging.i("Axiom", "Extracted version: $version from $name (ABI: $abiTag)")

                            val libraries = listOf(
                                "libzstd-jni-$version.so",
                                "libimgui-moulberry92-java-$abiTag.so"
                            )

                            for (libraryName in libraries) {
                                val targetFile = File(PathManager.DIR_MOD_LIBRARY, libraryName)
                                if (targetFile.exists()) {
                                    Logging.i("Axiom", "Library already exists: $targetFile")
                                    continue
                                }
                                val url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libraryName"
                                Logging.i("Axiom", "Queuing download from: $url")

                                downloadTasks.add(
                                    DownloadTask(
                                        url = url,
                                        targetFile = targetFile,
                                        modFile = mod.file,
                                        libFileName = libraryName,
                                        modFileName = mod.file.name,
                                        tag = "Axiom",
                                        settings = AllModCheckSettings.AXIOM,
                                        downloadingResId = R.string.mod_check_axiom_downloading,
                                        errorResId = R.string.mod_check_axiom_failed,
                                        debugResId = R.string.mod_check_axiom_debug
                                    )
                                )
                            }
                            break
                        }
                    }
                    if (!foundMatch) {
                        modCheckSettings[AllModCheckSettings.AXIOM] = Pair(
                            "1",
                            context.getString(R.string.mod_check_axiom_failed, mod.file.name) + "\n" +
                                    context.getString(R.string.mod_check_axiom_debug, "No suitable native library found in JAR")
                        )
                    }
                }
            }

            if (downloadTasks.isEmpty()) {
                showResultDialog(context, modCheckSettings) {
                    executeTask(modResult)
                }
            } else {
                processNextDownload(context, downloadTasks, 0, modCheckSettings, modResult) {
                    showResultDialog(context, modCheckSettings) {
                        executeTask(modResult)
                    }
                }
            }
        }.onFailure { e ->
            Logging.e("LaunchGame", "An error occurred while trying to process existing mod information", e)
            executeTask(null)
        }
    }

/**
 * processNextDownloadする
 */
    private fun processNextDownload(
        context: Context,
        tasks: List<DownloadTask>,
        index: Int,
        modCheckSettings: MutableMap<AllModCheckSettings, Pair<String, String>>,
        modResult: ModCheckResult,
        onComplete: () -> Unit
    ) {
        if (index >= tasks.size) {
            onComplete()
            return
        }

        val task = tasks[index]
        downloadLibraryWithDialog(context, task) { error ->
            if (error != null) {
                modCheckSettings[task.settings] = Pair("1", error)
                Logging.i(task.tag, "Download failed, moving to next task")
            }
            processNextDownload(context, tasks, index + 1, modCheckSettings, modResult, onComplete)
        }
    }

/**
 * downloadLibraryWithDialogする
 */
    private fun downloadLibraryWithDialog(
        context: Context,
        task: DownloadTask,
        onResult: (String?) -> Unit
    ) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            TaskExecutors.runInUIThread {
                TipDialog.Builder(context)
                    .setTitle(R.string.generic_warning)
                    .setMessage(context.getString(R.string.mod_check_no_network_library, task.modFileName))
                    .setCheckBox(R.string.mod_check_disable_check)
                    .setShowCheckBox(true)
                    .setConfirm(R.string.generic_confirm)
                    .setConfirmClickListener { disable ->
                        if (disable) {
                            ModUtils.disableMod(task.modFile)
                            task.settings.unit.put("1").save()
                        }
                        onResult(null)
                    }
                    .setCancel(R.string.generic_cancel)
                    .setCancelClickListener {
                        val error = context.getString(task.errorResId, task.modFileName) + "\n" +
                                context.getString(task.debugResId, context.getString(R.string.generic_no_network))
                        onResult(error)
                    }
                    .setShowCancel(true)
                    .setCancelable(false)
                    .showDialog()
            }
            return
        }

        val uiHandler = Handler(Looper.getMainLooper())
        val dialog = createProgressDialog(context, task.libFileName)
        var lastUpdateTime = System.nanoTime()
        var lastBytes = 0L
        var speed = ""

        dialog.setOnDismissListener {
            TaskExecutors.runInUIThread {
                com.kdt.mcgui.ProgressLayout.clearProgress(com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE)
            }
        }

        TaskExecutors.runInUIThread {
            com.kdt.mcgui.ProgressLayout.setProgress(
                com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                0,
                task.downloadingResId,
                task.libFileName
            )
            dialog.show()
        }

        val thread = Thread {
            var error: String? = null
            try {
                val connection = URL(task.url).openConnection().apply {
                    setRequestProperty("User-Agent", "YukariLauncher")
                    connect()
                }

                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                connection.getInputStream().use { input ->
                    task.targetFile.parentFile?.mkdirs()
                    task.targetFile.outputStream().use { output ->
                        while (true) {
                            val readCount = input.read(buffer)
                            if (readCount <= 0) break
                            output.write(buffer, 0, readCount)
                            downloadedBytes += readCount

                            val progress = if (totalBytes > 0L) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            val now = System.nanoTime()
                            val elapsed = (now - lastUpdateTime) / 1_000_000_000.0
                            if (elapsed >= 0.5) {
                                val bytesDelta = downloadedBytes - lastBytes
                                val bytesPerSec = (bytesDelta / elapsed).toLong()
                                speed = formatBytes(bytesPerSec) + "/s"
                                lastUpdateTime = now
                                lastBytes = downloadedBytes
                            }

                            val dlBytes = downloadedBytes
                            val total = totalBytes
                            uiHandler.post {
                                com.kdt.mcgui.ProgressLayout.setProgress(
                                    com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                                    progress,
                                    task.downloadingResId,
                                    task.libFileName
                                )
                                updateProgressDialog(dialog, progress, dlBytes, total, speed)
                            }
                        }
                    }
                }

                uiHandler.post {
                    com.kdt.mcgui.ProgressLayout.setProgress(
                        com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                        100,
                        task.downloadingResId,
                        task.libFileName
                    )
                    updateProgressDialog(dialog, 100, downloadedBytes, totalBytes, speed)
                }

                Logging.i(task.tag, "Successfully downloaded ${task.libFileName}")
            } catch (e: Exception) {
                Logging.e(task.tag, "Failed to download ${task.libFileName}", e)
                if (task.targetFile.exists()) {
                    task.targetFile.delete()
                    Logging.i(task.tag, "Deleted partial file: ${task.targetFile}")
                }
                val errorDetail = "${e.javaClass.simpleName}: ${e.message ?: "No message"}"
                error = context.getString(task.errorResId, task.modFileName) + "\n" +
                        context.getString(task.debugResId, errorDetail)
            }

            uiHandler.post {
                dialog.dismiss()
                if (error != null) {
                    showRetryDialog(context, task, error, onResult)
                } else {
                    onResult(null)
                }
            }
        }
        thread.start()
    }

/**
 * createProgressDialogする
 */
    private fun createProgressDialog(context: Context, libFileName: String): AlertDialog {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val progressIndicator = LinearProgressIndicator(context).apply {
            setTrackThickness(dp(8))
            trackCornerRadius = dp(4)
            max = 100
        }

        val sizeText = TextView(context).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setTextColor(context.getColor(R.color.primary_text))
            textSize = 14f
        }

        val speedText = TextView(context).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setTextColor(context.getColor(R.color.primary_text))
            textSize = 12f
        }

        val card = MaterialCardView(context).apply {
            setCardBackgroundColor(context.getColor(R.color.background_menu_element))
            setStrokeColor(context.getColor(R.color.settings_category))
            strokeWidth = dp(1)
            radius = dp(10).toFloat()
        }

        card.addView(row)
        row.addView(progressIndicator, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        row.addView(sizeText)
        row.addView(speedText)

        return MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.mod_check_download_progress, libFileName))
            .setView(card)
            .setCancelable(false)
            .create()
    }

/**
 * updateProgressDialogする
 */
    private fun updateProgressDialog(dialog: AlertDialog, progress: Int, downloadedBytes: Long, totalBytes: Long, speed: String) {
        val customFrame = dialog.findViewById<android.widget.FrameLayout>(android.R.id.custom) ?: return
        val card = customFrame.getChildAt(0) as? MaterialCardView ?: return
        val row = card.getChildAt(0) as? LinearLayout ?: return
        val progressIndicator = row.getChildAt(0) as? LinearProgressIndicator ?: return
        val sizeText = row.getChildAt(1) as? TextView ?: return
        val speedText = row.getChildAt(2) as? TextView ?: return

        progressIndicator.progress = progress
        sizeText.text = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            formatBytes(downloadedBytes)
        }
        speedText.text = speed
    }

/**
 * showRetryDialogする
 */
    private fun showRetryDialog(
        context: Context,
        task: DownloadTask,
        error: String,
        onResult: (String?) -> Unit
    ) {
        TaskExecutors.runInUIThread {
            TipDialog.Builder(context)
                .setTitle(R.string.mod_check_download_retry_title)
                .setMessage(context.getString(R.string.mod_check_download_retry_message, task.modFileName) + "\n\n$error")
                .setConfirm(R.string.mod_check_download_retry)
                .setConfirmClickListener {
                    downloadLibraryWithDialog(context, task, onResult)
                }
                .setCancel(R.string.mod_check_download_skip)
                .setCancelClickListener {
                    onResult(error)
                }
                .setShowCancel(true)
                .setCancelable(false)
                .showDialog()
        }
    }

    private var abiTag: String = "aarch64"

/**
 * initAbisする
 */
    private fun initAbis() {
        val deviceAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        abiTag = when {
            deviceAbi.contains("arm64") -> "arm64"
            deviceAbi.contains("x86_64") -> "x86_64"
            deviceAbi.contains("x86") -> "x86"
            deviceAbi.contains("armeabi-v7a") -> "arm"
            else -> "arm64"
        }

        Logging.i("ABI", "Detected ABI: $deviceAbi -> $abiTag")
    }

/**
 * matchesAbiする
 */
    private fun matchesAbi(name: String): Boolean {
        return when (abiTag) {
            "arm64" ->
                name.contains("aarch64", true) ||
                name.contains("arm64", true)

            "x86_64" ->
                name.contains("x86_64", true) ||
                name.contains("amd64", true)

            "x86" ->
                name.contains("x86", true) &&
                !name.contains("x86_64", true)

            "arm" ->
                name.contains("arm", true) &&
                !name.contains("arm64", true) &&
                !name.contains("aarch64", true)

            else -> true
        }
    }

/**
 * showResultDialogする
 */
    private fun showResultDialog(
        context: Context,
        modCheckSettings: MutableMap<AllModCheckSettings, Pair<String, String>>,
        executeTask: () -> Unit
    ) {
        val messages = modCheckSettings
            .mapNotNull { (setting, valuePair) ->
                if (setting.unit.getValue() != valuePair.first) valuePair.second else null
            }.withIndex()
            .joinToString("\r\n\r\n") {
                "${it.index + 1}. ${it.value}"
            }

        if (messages.isEmpty()) {
            executeTask()
            return
        }

        TaskExecutors.runInUIThread {
            TipDialog.Builder(context)
                .setTitle(R.string.mod_check_dialog_title)
                .setMessage(messages)
                .setCheckBox(R.string.generic_no_more_reminders)
                .setShowCheckBox(true)
                .setCenterMessage(false)
                .setCancelable(false)
                .setSelectable(true)
                .setConfirmClickListener { check ->
                    if (check) {
                        modCheckSettings.forEach { (setting, valuePair) ->
                            setting.unit.put(valuePair.first).save()
                        }
                    }
                    executeTask()
                }.showDialog()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192

/**
 * dpする
 */
        private fun dp(value: Int): Int {
            return (value * Resources.getSystem().displayMetrics.density).toInt()
        }

/**
 * formatBytesする
 */
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}