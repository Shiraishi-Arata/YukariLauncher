package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils

class UnpackJreTask(val context: Context, val jre: Jre) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            launcherRuntimeVersion = Tools.read(assetManager.open(jre.jrePath + "/version"))
        }.getOrElse {
            isCheckFailed = true
        }
    }

/**
 * isCheckFailedする
 */
    fun isCheckFailed() = isCheckFailed

/**
 * isNeedUnpackする
 */
    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        runCatching {
            val installedRuntimeVersion = MultiRTUtils.readInternalRuntimeVersion(jre.jreName)
            return launcherRuntimeVersion != installedRuntimeVersion
        }.getOrElse { e ->
            Logging.e("CheckInternalRuntime", Tools.printToString(e))
            return false
        }
    }

/**
 * runする
 */
    override fun run() {
        listener?.onTaskStart()
        runCatching {
            MultiRTUtils.installRuntimeNamedBinpack(
                assetManager.open(jre.jrePath + "/universal.tar.xz"),
                assetManager.open(
                    jre.jrePath + "/bin-" + Architecture.archAsString(
                        Tools.DEVICE_ARCHITECTURE
                    ) + ".tar.xz"
                ),
                jre.jreName, launcherRuntimeVersion
            )
            MultiRTUtils.postPrepare(jre.jreName)
        }.getOrElse { e -> Logging.e("UnpackJREAuto", "Internal JRE unpack failed", e) }
        listener?.onTaskEnd()
    }
}