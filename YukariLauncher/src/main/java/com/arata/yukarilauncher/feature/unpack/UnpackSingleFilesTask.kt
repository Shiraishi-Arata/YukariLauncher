package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import android.os.Build
import com.arata.yukarilauncher.feature.log.Logging.e
import com.arata.yukarilauncher.utils.CopyDefaultFromAssets.Companion.copyFromAssets
import com.arata.yukarilauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import java.io.File

class UnpackSingleFilesTask(val context: Context) : AbstractUnpackTask() {

    override fun isNeedUnpack(): Boolean = true

    override fun run() {
        runCatching {
            // Existing unpack
            copyFromAssets(context)
            Tools.copyAssetFile(context, "resolv.conf", PathManager.DIR_DATA, false)

            // 🔥 Add playit binary matching device ABI
            val playitBinaryName = resolvePlayitBinaryName()
            Tools.copyAssetFile(context, playitBinaryName, PathManager.DIR_DATA, false)

            // Ensure executable permission
            val playitFile = File(PathManager.DIR_DATA, playitBinaryName)
            if (!playitFile.canExecute()) {
                playitFile.setExecutable(true)
                try {
                    Runtime.getRuntime().exec("chmod 755 ${playitFile.absolutePath}")
                } catch (_: Exception) {}
            }

        }.getOrElse {
            e("AsyncAssetManager", "Failed to unpack critical components!")
        }
    }

    private fun resolvePlayitBinaryName(): String {
        for (abi in Build.SUPPORTED_ABIS) {
            when (abi) {
                "arm64-v8a" -> return "playit-linux-aarch64"
                "armeabi-v7a", "armeabi" -> return "playit-linux-armv7"
                "x86" -> return "playit-linux-i686"
                "x86_64" -> return "playit-linux-amd64"
            }
        }
        return "playit-linux-aarch64"
    }
}
