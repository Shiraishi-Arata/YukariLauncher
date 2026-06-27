package com.arata.yukarilauncher.feature.version.install

import android.app.Activity
import android.content.Intent
import android.os.Process
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.launch.LaunchArgs
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.LauncherProfiles
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.utils.runtime.Runtime as JreRuntime
import com.arata.yukarilauncher.utils.runtime.SelectRuntimeUtils
import androidx.appcompat.app.AppCompatActivity
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.task.ProgressKeeper
import java.io.File
import java.util.zip.ZipFile

object HeadlessInstaller {

    private fun splitPreservingQuotes(str: String): List<String> {
        val result = mutableListOf<String>()
        val currentPart = StringBuilder()
        var inQuotes = false
        for (i in str.indices) {
            val c = str[i]
            if (c == '"' && (i == 0 || str[i - 1] != '\\')) {
                inQuotes = !inQuotes
            } else if (c.isWhitespace() && !inQuotes) {
                if (currentPart.isNotEmpty()) {
                    result.add(currentPart.toString())
                    currentPart.clear()
                }
            } else {
                currentPart.append(c)
            }
        }
        if (currentPart.isNotEmpty()) {
            result.add(currentPart.toString())
        }
        return result
    }

    private fun findJarPath(argList: List<String>): File? {
        val argsSize = argList.size
        for (i in 0 until argsSize) {
            if (argList[i] != "-jar") continue
            val pathIndex = i + 1
            if (pathIndex >= argsSize) return null
            return File(argList[pathIndex])
        }
        return null
    }

    private fun classVersionToJavaVersion(majorVersion: Int): Int {
        return if (majorVersion < 46) 2 else majorVersion - 44
    }

    private fun getJavaVersionFromJar(jarFile: File): Int {
        try {
            ZipFile(jarFile).use { zipFile ->
                val manifest = zipFile.getEntry("META-INF/MANIFEST.MF") ?: return -1
                val manifestString = Tools.read(zipFile.getInputStream(manifest))
                val mainClass = Tools.extractUntilCharacter(manifestString, "Main-Class:", '\n') ?: return -1
                val mainClassPath = mainClass.trim().replace('.', '/') + ".class"
                val mainClassFile = zipFile.getEntry(mainClassPath) ?: return -1
                val classStream = zipFile.getInputStream(mainClassFile)
                val bytesWeNeed = ByteArray(8)
                val readCount = classStream.read(bytesWeNeed)
                classStream.close()
                if (readCount < 8) return -1
                val byteBuffer = java.nio.ByteBuffer.wrap(bytesWeNeed)
                if (byteBuffer.int != 0xCAFEBABE.toInt()) return -1
                val majorVersion = byteBuffer.short.toInt()
                return classVersionToJavaVersion(majorVersion)
            }
        } catch (e: Exception) {
            Logging.e("HeadlessInstaller", "Failed to read Java version from JAR", e)
            return -1
        }
    }

    private fun selectRuntimeForJar(jarFile: File): JreRuntime? {
        val javaVersion = getJavaVersionFromJar(jarFile)
        if (javaVersion == -1) return null
        val nearestRuntimeName = MultiRTUtils.getNearestJreName(javaVersion) ?: return null
        return MultiRTUtils.forceReread(nearestRuntimeName)
    }

    /**
     * 引数文字列からRuntimeを選択する。
     */
    private fun resolveRuntime(javaArgs: String, jreName: String?): JreRuntime? {
        if (jreName != null) {
            return MultiRTUtils.forceReread(jreName)
        }
        val argList = splitPreservingQuotes(javaArgs)
        val jarFile = findJarPath(argList)
        if (jarFile != null) {
            return selectRuntimeForJar(jarFile) ?: MultiRTUtils.forceReread(AllSettings.defaultRuntime.getValue())
        }
        return MultiRTUtils.forceReread(AllSettings.defaultRuntime.getValue())
    }

    /**
     * ヘッドレス（GUI無し）でModLoaderインストーラーを実行する。
     * @param activity Activity
     * @param javaArgs インストーラーJVM引数文字列（InstallArgsUtilsが生成したもの）
     * @param jreName 使用するJRE名（nullの場合は自動選択）
     * @param addonName アドオン名（通知表示用、null可）
     */
    @JvmStatic
    fun install(
        activity: Activity,
        javaArgs: String,
        jreName: String?,
        addonName: String? = null
    ) {
        val context = activity.applicationContext
        Task.runTask {
            try {
                ProgressKeeper.submitProgress(
                    ProgressLayout.INSTALL_RESOURCE, 0,
                    R.string.generic_waiting
                )

                val runtime = resolveRuntime(javaArgs, jreName)
                if (runtime == null) {
                    TaskExecutors.runInUIThread {
                        TipDialog.Builder(activity)
                            .setTitle(R.string.generic_error)
                            .setMessage(R.string.multirt_nocompatiblert)
                            .setWarning()
                            .showDialog()
                    }
                    return@runTask null
                }

                val argList = splitPreservingQuotes(javaArgs)

                val jvmArgList = mutableListOf<String>()
                jvmArgList.add("-Djava.awt.headless=true")
                jvmArgList.addAll(argList)

                Logging.i("HeadlessInstaller", "Installing with JRE: ${runtime.name}")
                Logging.i("HeadlessInstaller", "Args: ${jvmArgList.joinToString(" ")}")

                JREUtils.launchWithUtils(
                    activity as AppCompatActivity,
                    runtime,
                    null,
                    jvmArgList,
                    AllSettings.javaArgs.getValue()
                )
            } catch (e: Exception) {
                Logging.e("HeadlessInstaller", "Installation failed", e)
                TaskExecutors.runInUIThread {
                    Tools.showError(activity, e, false)
                }
            } finally {
                TaskExecutors.runInUIThread {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                }
            }
            null
        }.execute()
    }

    /**
     * InstallArgsUtilsを使用してヘッドレスインストールを実行する。
     * @param activity Activity
     * @param mcVersion Minecraftバージョン
     * @param selectVersion 選択されたアドオンバージョン
     * @param setArgs InstallArgsUtilsのメソッドを呼び出すラムダ
     * @param addonName アドオン名
     */
    @JvmStatic
    fun installWithArgs(
        activity: Activity,
        mcVersion: String,
        selectVersion: String,
        setArgs: (Intent, InstallArgsUtils) -> Unit,
        addonName: String? = null
    ) {
        val intent = Intent()
        val argUtils = InstallArgsUtils(mcVersion, selectVersion)
        setArgs(intent, argUtils)
        val javaArgs = intent.getStringExtra("javaArgs") ?: run {
            Tools.showError(activity, IllegalArgumentException("Failed to build installer args"), false)
            return
        }

        SelectRuntimeUtils.selectRuntime(
            activity,
            addonName?.let { activity.getString(R.string.version_install_new_modloader, it) }
        ) { jreName ->
            LauncherProfiles.generateLauncherProfiles()
            install(activity, javaArgs, jreName, addonName)
        }
    }
}
