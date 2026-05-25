package com.arata.yukarilauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.arata.yukarilauncher.feature.log.Logging.i
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class UnpackComponentsTask(val context: Context, val component: Components) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            versionFile = File("$rootDir/${component.component}/version")
            input = am.open("components/${component.component}/version")
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

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            i("Unpack Components", "${component.component}: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = Tools.read(input)
            val release2 = Tools.read(fis)
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                i("UnpackPrep", "${component.component}: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

/**
 * runする
 */
    override fun run() {
        listener?.onTaskStart()
        copyAssetDir("components/${component.component}", "$rootDir/${component.component}")
        listener?.onTaskEnd()
    }

/**
 * copyAssetDirする - recursively copy assets including subdirectories
 */
    private fun copyAssetDir(assetPath: String, outputPath: String) {
        val names = am.list(assetPath) ?: return
        for (name in names) {
            val fullAssetPath = "$assetPath/$name"
            val children = try { am.list(fullAssetPath) } catch (_: IOException) { null }
            if (children != null && children.isNotEmpty()) {
                File(outputPath, name).mkdirs()
                copyAssetDir(fullAssetPath, "$outputPath/$name")
            } else {
                Tools.copyAssetFile(context, fullAssetPath, outputPath, true)
            }
        }
    }

/**
 * requestEmptyParentDirする
 */
    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() and isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}