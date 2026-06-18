package com.arata.yukarilauncher.setting

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.unpack.Jre
import com.arata.yukarilauncher.ui.activity.BaseActivity
import com.arata.yukarilauncher.utils.mouse.CursorPackUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import java.io.File

/** ランチャーの設定を管理するオブジェクト。 */
object LauncherPreferences {
    const val DEFAULT_MOUSE_PACK_NAME = "default"
    private const val DEFAULT_MOUSE_PACK_ARCHIVE = "$DEFAULT_MOUSE_PACK_NAME.zip"

    /** 設定を読み込む。 */
    fun loadPreferences() {
        installDefaultCustomMouse()
        val argLwjglLibname = "-Dorg.lwjgl.opengl.libname="
        val javaArgs = AllSettings.javaArgs.getValue()
        for (arg in JREUtils.parseJavaArguments(javaArgs)) {
            if (arg.startsWith(argLwjglLibname)) {
                AllSettings.javaArgs.put(javaArgs.replace(arg, "")).save()
            }
        }
        reloadRuntime()
    }

    /** 同梱カーソルパックをマウスディレクトリへ展開し、未選択時のデフォルトにする。 */
    private fun installDefaultCustomMouse() {
        runCatching {
            val mouseRoot = File(PathManager.DIR_CUSTOM_MOUSE).apply {
                if (!exists()) mkdirs()
            }
            val defaultMouseDir = File(mouseRoot, DEFAULT_MOUSE_PACK_NAME)
            if (!CursorPackUtils.isSupportedCursorSource(defaultMouseDir)) {
                if (defaultMouseDir.exists()) defaultMouseDir.deleteRecursively()
                val archiveFile = File(mouseRoot, DEFAULT_MOUSE_PACK_ARCHIVE)
                Tools.copyAssetFile(
                    ContextExecutor.getApplication(),
                    DEFAULT_MOUSE_PACK_ARCHIVE,
                    mouseRoot.absolutePath,
                    DEFAULT_MOUSE_PACK_ARCHIVE,
                    true
                )
                CursorPackUtils.extractCursorArchive(archiveFile, mouseRoot)
                archiveFile.delete()
            }

            if (!Settings.Manager.contains(AllSettings.customMouse.key) || AllSettings.customMouse.getValue().isEmpty()) {
                AllSettings.customMouse.put(DEFAULT_MOUSE_PACK_NAME).save()
            }
        }.onFailure { e ->
            Logging.w("LauncherPreferences", "Failed to install bundled default cursor pack", e)
        }
    }

    /** ランタイム設定を再読み込みする。 */
    fun reloadRuntime() {
        if (!Settings.Manager.contains("defaultRuntime") && !MultiRTUtils.runtimes.isEmpty()) {
            AllSettings.defaultRuntime.put(Jre.JRE_8.jreName).save()
        }
    }

    /** 最適なRAM割り当て量を算出する。 @param ctx コンテキスト @return 推奨RAMサイズ（MB） */
    fun findBestRAMAllocation(ctx: Context): Int {
        val deviceRam = Tools.getTotalDeviceMemory(ctx)
        if (deviceRam < 1024) return 296
        if (deviceRam < 1536) return 448
        if (deviceRam < 2048) return 656
        if (Architecture.is32BitsDevice()) return 696
        if (deviceRam < 3064) return 936
        if (deviceRam < 4096) return 1144
        if (deviceRam < 6144) return 1536
        if (deviceRam < 8192) return 2048
        if (deviceRam < 12288) return 3072
        return 4096
    }

    /** ノッチサイズを計算する。 @param activity ベースアクティビティ */
    fun computeNotchSize(activity: BaseActivity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val cutout: Rect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                cutout = activity.windowManager.currentWindowMetrics.windowInsets.displayCutout!!.boundingRects[0]
            } else {
                cutout = activity.window.decorView.rootWindowInsets!!.displayCutout!!.boundingRects[0]
            }
            val orientation = activity.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_PORTRAIT) AllStaticSettings.notchSize = cutout.height()
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE) AllStaticSettings.notchSize = cutout.width()
            else AllStaticSettings.notchSize = Math.min(cutout.width(), cutout.height())
        } catch (e: Exception) {
            Logging.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode")
            AllStaticSettings.notchSize = -1
        }
        Tools.updateWindowSize(activity)
    }
}