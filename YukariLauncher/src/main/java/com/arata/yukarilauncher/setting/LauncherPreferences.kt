package com.arata.yukarilauncher.setting

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.unpack.Jre
import com.arata.yukarilauncher.ui.activity.BaseActivity
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils

/** ランチャーの設定を管理するオブジェクト。 */
object LauncherPreferences {
    /** 設定を読み込む。 */
    fun loadPreferences() {
        val argLwjglLibname = "-Dorg.lwjgl.opengl.libname="
        val javaArgs = AllSettings.javaArgs.getValue()
        for (arg in JREUtils.parseJavaArguments(javaArgs)) {
            if (arg.startsWith(argLwjglLibname)) {
                AllSettings.javaArgs.put(javaArgs.replace(arg, "")).save()
            }
        }
        reloadRuntime()
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
        return 2048
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
