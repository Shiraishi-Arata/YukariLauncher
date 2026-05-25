package com.arata.yukarilauncher.feature.mod.parser

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.mio.util.AndroidUtil
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.plugins.FFmpegPlugin
import java.io.File
import java.net.URL
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
                                // Try to download the native library (no ABI check)
                                val errorMessage = handlePhysics(context, mod.file)
                                if (errorMessage != null) {
                                    modCheckSettings[AllModCheckSettings.PHYSICS_MOD] = Pair(
                                        "1",
                                        errorMessage
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
                            val errorMessage = handleSable(context, mod.file)
                            if (errorMessage != null) {
                                modCheckSettings[AllModCheckSettings.SABLE] = Pair(
                                    "1",
                                    errorMessage
                                )
                            }
                        }
                    }
                    "flashback" -> {
                        if (!modResult.hasFlashBack) {
                            modResult.hasFlashBack = true
                            val errorMessage = handleFlashback(context, mod.file)
                            if (errorMessage != null) {
                                modCheckSettings[AllModCheckSettings.FLASHBACK] = Pair(
                                    "1",
                                    errorMessage
                                )
                            }
                        }
                    }
                }
                
                if (mod.file.name.matches(Regex("Axiom-.*\\.jar", RegexOption.IGNORE_CASE))) {
                    val errorMessage = handleAxiom(context, mod.file)
                    if (errorMessage != null) {
                        modCheckSettings[AllModCheckSettings.AXIOM] = Pair("1", errorMessage)
                    }
                }
            }

            showResultDialog(context, modCheckSettings) {
                executeTask(modResult)
            }
        }.onFailure { e ->
            Logging.e("LaunchGame", "An error occurred while trying to process existing mod information", e)
            executeTask(null)
        }
    }

/**
 * handlePhysicsする
 */
    private fun handlePhysics(context: Context, modFile: File): String? {
        val libFileName = "libPhysXJniBindings_64.so"
        val targetFile = File(PathManager.DIR_MOD_LIBRARY, libFileName)

        if (targetFile.exists()) {
            Logging.i("Physics", "Library already exists: $targetFile")
            return null
        }

        val url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/PhysX/$libFileName"
        Logging.i("Physics", "Attempting to download $libFileName from $url")

        TaskExecutors.runInUIThread {
            com.kdt.mcgui.ProgressLayout.setProgress(
                com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                0,
                R.string.mod_check_physics_downloading,
                libFileName
            )
        }

        var error: String? = null
        val thread = Thread {
            try {
                downloadLibraryWithProgress(
                    url,
                    targetFile,
                    R.string.mod_check_physics_downloading,
                    libFileName
                )
                Logging.i("Physics", "Successfully downloaded $libFileName")
            } catch (e: Exception) {
                Logging.e("Physics", "Failed to download $libFileName", e)
                val errorDetail = "${e.javaClass.simpleName}: ${e.message ?: "No message"}"
                error = context.getString(R.string.mod_check_physics_failed, modFile.name) + "\n" +
                        context.getString(R.string.mod_check_physics_debug, errorDetail)
            } finally {
                TaskExecutors.runInUIThread {
                    com.kdt.mcgui.ProgressLayout.clearProgress(com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE)
                }
            }
        }
        thread.start()
        thread.join()
        return error
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
 * handleAxiomする
 */
    private fun handleAxiom(context: Context, modFile: File): String? {

        ZipFile(modFile).use { zipFile ->
    
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
    
                Logging.i(
                    "Axiom",
                    "Extracted version: $version from $name (ABI: $abiTag)"
                )
    
                val libraries = listOf(
                    "libzstd-jni-$version.so",
                    "libimgui-moulberry92-java-$abiTag.so"
                )
    
                for (libraryName in libraries) {
    
                    val targetFile =
                        File(PathManager.DIR_MOD_LIBRARY, libraryName)
    
                    if (targetFile.exists()) {
                        Logging.i(
                            "Axiom",
                            "Library already exists: $targetFile"
                        )
                        continue
                    }
    
                    val url =
                        "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libraryName"
    
                    Logging.i(
                        "Axiom",
                        "Attempting to download from: $url"
                    )
    
                    TaskExecutors.runInUIThread {
                        com.kdt.mcgui.ProgressLayout.setProgress(
                            com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                            0,
                            R.string.mod_check_axiom_downloading,
                            libraryName
                        )
                    }
    
                    var error: String? = null
    
                    val thread = Thread {
    
                        try {
    
                            downloadLibraryWithProgress(
                                url,
                                targetFile,
                                R.string.mod_check_axiom_downloading,
                                libraryName
                            )
    
                            Logging.i(
                                "Axiom",
                                "Successfully downloaded $libraryName"
                            )
    
                        } catch (e: Exception) {
    
                            Logging.e(
                                "Axiom",
                                "Download failed",
                                e
                            )
    
                            val errorDetail =
                                "${e.javaClass.simpleName}: ${e.message ?: "No message"}"
    
                            error =
                                context.getString(
                                    R.string.mod_check_axiom_failed,
                                    modFile.name
                                ) + "\n" +
                                context.getString(
                                    R.string.mod_check_axiom_debug,
                                    errorDetail
                                )
    
                        } finally {
    
                            TaskExecutors.runInUIThread {
                                com.kdt.mcgui.ProgressLayout.clearProgress(
                                    com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE
                                )
                            }
                        }
                    }
    
                    thread.start()
                    thread.join()
    
                    if (error != null) {
                        return error
                    }
                }
    
                return null
            }
        }
    
        return context.getString(
            R.string.mod_check_axiom_failed,
            modFile.name
        ) + "\n" +
        context.getString(
            R.string.mod_check_axiom_debug,
            "No suitable native library found in JAR"
        )
    }
    
/**
 * handleFlashbackする
 */
    private fun handleFlashback(context: Context, modFile: File): String? {
        val libFileName = "libimgui-moulberry90-java-$abiTag.so"
        val targetFile = File(PathManager.DIR_MOD_LIBRARY, libFileName)

        if (targetFile.exists()) {
            Logging.i("Flashback", "Library already exists: $targetFile")
            return null
        }

        val url = "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libFileName"
        Logging.i("Flashback", "Attempting to download $libFileName from $url")

        TaskExecutors.runInUIThread {
            com.kdt.mcgui.ProgressLayout.setProgress(
                com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                0,
                R.string.mod_check_axiom_downloading,
                libFileName
            )
        }

        var error: String? = null

        val thread = Thread {
            try {
                downloadLibraryWithProgress(
                    url,
                    targetFile,
                    R.string.mod_check_axiom_downloading,
                    libFileName
                )
                Logging.i("Flashback", "Successfully downloaded $libFileName")
            } catch (e: Exception) {
                Logging.e("Flashback", "Failed to download $libFileName", e)
                val errorDetail = "${e.javaClass.simpleName}: ${e.message ?: "No message"}"
                error = context.getString(R.string.mod_check_axiom_failed, modFile.name) + "\n" +
                        context.getString(R.string.mod_check_axiom_debug, errorDetail)
            } finally {
                TaskExecutors.runInUIThread {
                    com.kdt.mcgui.ProgressLayout.clearProgress(
                        com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE
                    )
                }
            }
        }

        thread.start()
        thread.join()

        return error
    }

/**
 * handleSableする
 */
    private fun handleSable(context: Context, modFile: File): String? {
    
        val libFileName = "libsable_rapier-$abiTag.so"
        val targetFile = File(PathManager.DIR_MOD_LIBRARY, libFileName)
    
        if (targetFile.exists()) {
            Logging.i("Sable", "Library already exists: $targetFile")
            return null
        }
    
        val url =
            "https://github.com/Shiraishi-Arata/Yukari-Fixes/releases/download/$abiTag/$libFileName"
    
        Logging.i("Sable", "Attempting to download $libFileName from $url")
    
        TaskExecutors.runInUIThread {
            com.kdt.mcgui.ProgressLayout.setProgress(
                com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                0,
                R.string.mod_check_sable_downloading,
                libFileName
            )
        }
    
        var error: String? = null
    
        val thread = Thread {
            try {
    
                downloadLibraryWithProgress(
                    url,
                    targetFile,
                    R.string.mod_check_sable_downloading,
                    libFileName
                )
    
                Logging.i(
                    "Sable",
                    "Successfully downloaded $libFileName"
                )
    
            } catch (e: Exception) {
    
                Logging.e(
                    "Sable",
                    "Failed to download $libFileName",
                    e
                )
    
                val errorDetail =
                    "${e.javaClass.simpleName}: ${e.message ?: "No message"}"
    
                error =
                    context.getString(
                        R.string.mod_check_sable_failed,
                        modFile.name
                    ) + "\n" +
                    context.getString(
                        R.string.mod_check_sable_debug,
                        errorDetail
                    )
    
            } finally {
                TaskExecutors.runInUIThread {
                    com.kdt.mcgui.ProgressLayout.clearProgress(
                        com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE
                    )
                }
            }
        }
    
        thread.start()
        thread.join()
    
        return error
    }


/**
 * downloadLibraryWithProgressする
 */
    private fun downloadLibraryWithProgress(url: String, targetFile: File, progressResId: Int, progressArg: String) {
        val connection = URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "YukariLauncher")
            connect()
        }

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        connection.getInputStream().use { input ->
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { output ->
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
                    TaskExecutors.runInUIThread {
                        com.kdt.mcgui.ProgressLayout.setProgress(
                            com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                            progress,
                            progressResId,
                            progressArg
                        )
                    }
                }
            }
        }

        TaskExecutors.runInUIThread {
            com.kdt.mcgui.ProgressLayout.setProgress(
                com.kdt.mcgui.ProgressLayout.INSTALL_RESOURCE,
                100,
                progressResId,
                progressArg
            )
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
}