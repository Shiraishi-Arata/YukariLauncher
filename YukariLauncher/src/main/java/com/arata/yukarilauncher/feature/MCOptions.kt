package com.arata.yukarilauncher.feature

import android.content.Context
import android.os.Build
import android.os.FileObserver
import com.arata.yukarilauncher.event.single.MCOptionChangeEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.Tools
import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge.windowHeight
import org.lwjgl.glfw.CallbackBridge.windowWidth
import java.io.File
import java.io.IOException

object MCOptions {
    private val parameterMap = mutableMapOf<String, String>()
    private var fileObserver: FileObserver? = null
    private lateinit var versionGetter: MinecraftVersionGetter

    /**
     * 初始化 MCOptions
     * 检查 options.txt 是否存在，如果不存在，将会复制一份默认的 options.txt 文件
     */
/**
 * MCOptionsを初期化する
 * options.txtが存在しない場合はデフォルトファイルをコピーする
 */
    fun setup(context: Context, versionGetter: MinecraftVersionGetter) {
        this.versionGetter = versionGetter
        parameterMap.clear()
        fileObserver?.stopWatching()
        fileObserver = null

        getOptionsFile().apply {
            if (!exists()) {
                try {
                    Tools.copyAssetFile(
                        context,
                        "options.txt",
                        versionGetter.getVersion().getGameDir().absolutePath,
                        false
                    )
                } catch (e: Exception) {
                    Logging.e("MCOptions", "Failed to copy the default options.txt file.", e)
                }
            }
        }

        load()
    }

/**
 * options.txtファイルから設定を読み込む
 */
    private fun load() {
        val optionFile = getOptionsFile().apply {
            if (!exists()) {
                try {
                    createNewFile()
                } catch (e: IOException) {
                    Logging.e("MCOptions", Tools.printToString(e))
                }
            }
        }

        if (fileObserver == null) {
            setupFileObserver()
        }

        parameterMap.clear()

        try {
            optionFile.forEachLine { line ->
                line.indexOf(':').takeIf { it >= 0 }?.let { colonIndex ->
                    parameterMap[line.substring(0, colonIndex)] = line.substring(colonIndex + 1)
                } ?: Logging.w("MCOptions", "Invalid line format: $line")
            }
        } catch (e: IOException) {
            Logging.w("MCOptions", "Could not load options.txt", e)
        }
    }

/**
 * setする
 */
    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

/**
 * getする
 */
    fun get(key: String): String? = parameterMap[key]

/**
 * 指定されたキーが存在するかを確認する
 * @param key 設定キー
 * @return 存在する場合はtrue
 */
    fun containsKey(key: String): Boolean = key in parameterMap

/**
 * saveする
 */
    fun save() {
        getOptionsFile().takeIf { it.exists() }?.let { optionsFile ->
            val optionsString = parameterMap.entries.joinToString("\n") { "${it.key}:${it.value}" }
            try {
                fileObserver?.stopWatching()
                optionsFile.writeText(optionsString)
            } catch (e: IOException) {
                Logging.w("MCOptions", "Could not save options.txt", e)
            } finally {
                fileObserver?.startWatching()
            }
        }
    }

    /**
     * MinecraftのGUIスケールを取得する
     * guiScale設定と画面サイズから適切なスケール値を計算する
     */
    val mcScale: Int
        get() {
            val guiScale = get("guiScale")?.toIntOrNull() ?: 0
            val scale = minOf(windowWidth / 320, windowHeight / 240).coerceAtLeast(1)
            return if (guiScale == 0 || scale < guiScale) scale else guiScale
        }

/**
 * options.txtのFileオブジェクトを取得する
 * @return options.txtファイル
 */
    private fun getOptionsFile() = File(versionGetter.getVersion().getGameDir(), "options.txt")

/**
 * ファイル変更監視を設定する
 */
    private fun setupFileObserver() {
        fileObserver = createFileObserver(getOptionsFile()).apply {
            startWatching()
        }
    }

/**
 * FileObserverを作成する
 * @param file 監視対象ファイル
 * @return FileObserver
 */
    private fun createFileObserver(file: File): FileObserver {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file, MODIFY) {
/**
 * onEventする
 */
                override fun onEvent(event: Int, path: String?) {
                    handleFileChange()
                }
            }
        } else {
            object : FileObserver(file.absolutePath, MODIFY) {
/**
 * onEventする
 */
                override fun onEvent(event: Int, path: String?) {
                    handleFileChange()
                }
            }
        }
    }

/**
 * ファイル変更時に設定を再読み込みしてイベントを発行する
 */
    private fun handleFileChange() {
        load()
        EventBus.getDefault().post(MCOptionChangeEvent())
    }

    /**
     * 这个接口用于获取 Minecraft 版本信息
     */
/**
 * interfaceする
 */
    fun interface MinecraftVersionGetter {
/**
 * Minecraftバージョン情報を取得する
 * @return バージョン情報
 */
        fun getVersion(): Version
    }
}