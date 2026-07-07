package com.arata.yukarilauncher.feature.version.install

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.install.forge.ForgeInstallTask
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.activity.InstallerActivity
import com.arata.yukarilauncher.utils.LauncherProfiles
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.utils.runtime.Runtime as JreRuntime
import com.arata.yukarilauncher.utils.runtime.SelectRuntimeUtils
import com.kdt.mcgui.ProgressLayout
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

object HeadlessInstaller {

    const val ACTION_INSTALL_DONE = "com.arata.yukarilauncher.action.INSTALL_DONE"
    const val EXTRA_JRE_NAME = "jre_name"

    @JvmStatic
    fun splitPreservingQuotes(str: String): List<String> {
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

    @JvmStatic
    fun resolveRuntime(javaArgs: String, jreName: String? = null): JreRuntime? {
        if (jreName != null) {
            return MultiRTUtils.forceReread(jreName)
        }
        val argList = splitPreservingQuotes(javaArgs)
        val jarFile = findJarPath(argList)
        if (jarFile != null) {
            return selectRuntimeForJar(jarFile) ?: MultiRTUtils.forceReread(
                com.arata.yukarilauncher.setting.AllSettings.defaultRuntime.getValue()
            )
        }
        return MultiRTUtils.forceReread(
            com.arata.yukarilauncher.setting.AllSettings.defaultRuntime.getValue()
        )
    }

    /**
     * ForgeインストーラーをHMCL方式で実行する。
     * @param activity Activity
     * @param mcVersion Minecraftバージョン
     * @param selectVersion 選択されたForgeバージョン
     * @param installerJar インストーラーJARファイル
     * @param customVersionName カスタムバージョン名
     * @param loaderName ローダー表示名
     */
    @JvmStatic
    fun installForge(
        activity: Activity,
        mcVersion: String,
        selectVersion: String,
        installerJar: File,
        customVersionName: String,
        loaderName: String
    ) {
        SelectRuntimeUtils.selectRuntime(
            activity,
            activity.getString(R.string.version_install_new_modloader, loaderName)
        ) { jreName ->
            LauncherProfiles.generateLauncherProfiles()

            TaskExecutors.runInUIThread {
                ProgressLayout.setProgress(
                    ProgressLayout.INSTALL_RESOURCE, 0,
                    R.string.generic_waiting
                )
            }

            Thread({
                try {
                    val success = ForgeInstallTask.install(
                        context = activity,
                        installerJar = installerJar,
                        loaderName = loaderName,
                        customVersionName = customVersionName,
                        mcVersion = mcVersion,
                        jreName = jreName
                    )
                    TaskExecutors.runInUIThread {
                        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                        if (success) {
                            Logging.i("HeadlessInstaller", "Forge installation completed")
                        } else {
                            Logging.e("HeadlessInstaller", "Forge installation failed")
                        }
                    }
                } catch (e: Exception) {
                    TaskExecutors.runInUIThread {
                        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                        Logging.e("HeadlessInstaller", "Forge installation error", e)
                        Tools.showError(activity, e, true)
                    }
                }
            }, "ForgeInstaller").apply {
                isDaemon = true
                start()
            }
        }
    }

    /**
     * ヘッドレス（GUI無し）でModLoaderインストーラーを実行する。
     * JVMは:installerプロセスで起動し、System.exit()によるプロセス終了がメインプロセスに影響しない。
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
        val installDone = AtomicBoolean(false)

        val installDoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!installDone.compareAndSet(false, true)) return
                try { activity.unregisterReceiver(this) } catch (_: IllegalArgumentException) {}
                TaskExecutors.runInUIThread {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                    Logging.i("HeadlessInstaller", "Installer process finished")
                }
            }
        }

        activity.registerReceiver(installDoneReceiver, IntentFilter(ACTION_INSTALL_DONE))

        TaskExecutors.runInUIThread {
            ProgressLayout.setProgress(
                ProgressLayout.INSTALL_RESOURCE, 0,
                R.string.generic_waiting
            )
        }

        activity.startActivity(Intent(activity, InstallerActivity::class.java).apply {
            putExtra("javaArgs", javaArgs)
            jreName?.let { putExtra(EXTRA_JRE_NAME, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        Thread({
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(10))
                if (!installDone.compareAndSet(false, true)) return@Thread
                try {
                    activity.unregisterReceiver(installDoneReceiver)
                } catch (_: IllegalArgumentException) {}
                TaskExecutors.runInUIThread {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                    Logging.w("HeadlessInstaller", "Installer timed out after 10 minutes")
                }
            } catch (_: InterruptedException) {
            }
        }, "InstallerWatchdog").apply {
            isDaemon = true
            start()
        }
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
