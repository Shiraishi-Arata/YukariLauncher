package com.arata.yukarilauncher.plugins

import android.content.Context
import com.arata.yukarilauncher.feature.log.Logging
import java.io.File

/** FFmpegプラグインを管理するオブジェクト。 */
object FFmpegPlugin {
    /** プラグインが利用可能か */
    var isAvailable = false
    /** ライブラリパス */
    var libraryPath: String? = null
    /** 実行可能ファイルパス */
    var executablePath: String? = null

    /** FFmpegプラグインを探索する。 @param context コンテキスト */
    fun discover(context: Context) {
        val manager = context.packageManager
        try {
            val ffmpegPluginInfo = manager.getPackageInfo("net.kdt.pojavlaunch.ffmpeg", 0)
            libraryPath = ffmpegPluginInfo.applicationInfo!!.nativeLibraryDir
            val ffmpegExecutable = File(libraryPath, "libffmpeg.so")
            executablePath = ffmpegExecutable.absolutePath
            isAvailable = ffmpegExecutable.exists()
        } catch (e: Exception) {
            Logging.i("FFmpegPlugin", "Failed to discover plugin", e)
        }
    }
}