package com.arata.yukarilauncher.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Process
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.feature.version.install.HeadlessInstaller
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import java.io.File
import java.io.IOException

class InstallerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val javaArgs = intent.getStringExtra("javaArgs") ?: run {
            sendBroadcast(Intent(HeadlessInstaller.ACTION_INSTALL_DONE).setPackage(packageName))
            finishAndRemoveTask()
            return
        }
        val jreName = intent.getStringExtra(HeadlessInstaller.EXTRA_JRE_NAME)

        Thread({
            try {
                val latestLogFile = File(PathManager.DIR_GAME_HOME, "latestlog.txt")
                if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                    throw IOException("Failed to create a new log file")
                Logger.begin(latestLogFile.absolutePath)
            } catch (e: IOException) {
                Logging.e("InstallerActivity", "Failed to init logger", e)
            }

            val runtime = if (jreName != null) {
                MultiRTUtils.forceReread(jreName)
            } else {
                HeadlessInstaller.resolveRuntime(javaArgs)
            }

            if (runtime == null) {
                Logging.e("InstallerActivity", "No compatible runtime")
                sendBroadcast(Intent(HeadlessInstaller.ACTION_INSTALL_DONE).setPackage(packageName))
                runOnUiThread { finishAndRemoveTask() }
                Process.killProcess(Process.myPid())
                return@Thread
            }

            val argList = HeadlessInstaller.splitPreservingQuotes(javaArgs)
            val jvmArgList = mutableListOf<String>()
            jvmArgList.add("-Djava.awt.headless=true")
            jvmArgList.addAll(argList)

            Logging.i("InstallerActivity", "Installing with JRE: ${runtime.name}")
            Logging.i("InstallerActivity", "Args: ${jvmArgList.joinToString(" ")}")

            try {
                JREUtils.launchWithUtils(
                    this@InstallerActivity,
                    runtime,
                    null,
                    jvmArgList,
                    AllSettings.javaArgs.getValue()
                )
            } catch (e: Exception) {
                Logging.e("InstallerActivity", "JVM execution failed", e)
            }

            sendBroadcast(Intent(HeadlessInstaller.ACTION_INSTALL_DONE).setPackage(packageName))
            runOnUiThread { finishAndRemoveTask() }
            Process.killProcess(Process.myPid())
        }, "InstallerActivity-JVM").apply {
            isDaemon = true
            start()
        }
    }
}
