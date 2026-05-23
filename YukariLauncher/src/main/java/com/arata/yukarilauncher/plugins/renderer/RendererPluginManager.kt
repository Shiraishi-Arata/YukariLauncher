package com.arata.yukarilauncher.plugins.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.update.UpdateUtils
import com.arata.yukarilauncher.renderer.Renderers
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.ZipUtils
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

/**
 * FCL、ZalithLauncher レンダラープラグイン管理
 * ローカルレンダラープラグインも同時にサポートする
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()
    private val apkRendererPluginList: MutableList<ApkRendererPlugin> = mutableListOf()
    private val localRendererPluginList: MutableList<LocalRendererPlugin> = mutableListOf()

    /**
     * 現在読み込まれているすべてのレンダラープラグインを取得する
     */
    @JvmStatic
    fun getRendererList() = rendererPluginList

    /**
     * 指定されたレンダラープラグインを削除する
     */
    @JvmStatic
    fun removeRenderer(rendererPlugins: Collection<RendererPlugin>) {
        rendererPluginList.removeAll(rendererPlugins)
    }

    /**
     * ローカルレンダラープラグインのリストを取得する
     */
    @JvmStatic
    fun getAllLocalRendererList() = localRendererPluginList

    /**
     * レンダラープラグインが利用可能かどうかを返す
     * @return 利用可能な場合はtrue
     */
    @JvmStatic
    fun isAvailable(): Boolean {
        return rendererPluginList.isNotEmpty()
    }

    /**
     * 現在選択されているレンダラープラグインを取得する
     * レンダラーの一意識別子に基づいて判断する
     */
    @JvmStatic
    val selectedRendererPlugin: RendererPlugin?
        get() {
            val currentRenderer = runCatching {
                Renderers.getCurrentRenderer().getUniqueIdentifier()
            }.getOrNull()
            return rendererPluginList.find { it.uniqueIdentifier == currentRenderer }
        }

    /**
     * すべてのレンダラープラグインをクリアする
     */
    fun clearPlugin() {
        rendererPluginList.clear()
        apkRendererPluginList.clear()
        localRendererPluginList.clear()
    }

    /**
     * 設定可能なレンダラープラグインを取得する（ソフトウェア方式、ホワイトリストパッケージ）
     */
    @JvmStatic
    fun getConfigurablePluginOrNull(rendererUniqueIdentifier: String): RendererPlugin? {
        val renderer = apkRendererPluginList.find { it.uniqueIdentifier == rendererUniqueIdentifier }
        return renderer?.takeIf { it.packageName in setOf(
                "com.bzlzhh.plugin.ngg",
                "com.bzlzhh.plugin.ngg.angleless",
                "com.fcl.plugin.mobileglues"
            ) }
    }

    /**
     * APKからレンダラープラグインを解析する（ZalithLauncher / FCL形式）
     */
    fun parseApkPlugin(context: Context, info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("yukariRendererPlugin", false)
            ) {
                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")

                var rendererId: String = renderer[0]
                val envList = mutableMapOf<String, String>()
                val dlopenList = mutableListOf<String>()
                pojavEnvString.split(":").forEach { envString ->
                    if (envString.contains("=")) {
                        val stringList = envString.split("=")
                        val key = stringList[0]
                        val value = stringList[1]
                        when (key) {
                            "POJAV_RENDERER" -> rendererId = value
                            "DLOPEN" -> {
                                value.split(",").forEach { lib ->
                                    dlopenList.add(lib)
                                }
                            }
                            "LIB_MESA_NAME", "MESA_LIBRARY" -> envList[key] = "$nativeLibraryDir/$value"
                            else -> envList[key] = value
                        }
                    }
                }

                val packageName = info.packageName

                val plugin = ApkRendererPlugin(
                    rendererId,
                    "$des (${
                        context.getString(
                            R.string.setting_renderer_from_plugins,
                            runCatching {
                                context.packageManager.getApplicationLabel(info)
                            }.getOrElse {
                                context.getString(R.string.generic_unknown)
                            }
                        )
                    })",
                    packageName,
                    renderer[1],
                    renderer[2].progressEglName(nativeLibraryDir),
                    nativeLibraryDir,
                    envList,
                    dlopenList,
                    packageName
                )

                rendererPluginList.add(plugin)
                apkRendererPluginList.add(plugin)
            }
        }
    }

    /**
     * ローカルディレクトリからレンダラープラグインを解析する
     * @return 有効なプラグインかどうか
     *
     * レンダラーフォルダの形式
     * renderer_plugins/
     * ----フォルダ名/
     * --------config（レンダラー設定ファイル）
     * --------libs/（レンダラー.soファイルの格納ディレクトリ）
     * ------------arm64-v8a/ (arm64)
     * ----------------レンダラーライブラリ.so
     * ------------armeabi-v7a/ (arm32)
     * ----------------レンダラーライブラリ.so
     * ------------x86/ (x86)
     * ----------------レンダラーライブラリ.so
     * ------------x86_64/ (x86_64)
     * ----------------レンダラーライブラリ.so
     */
    fun parseLocalPlugin(context: Context, directory: File): Boolean {
        val archModel: String = UpdateUtils.getArchModel(Architecture.getDeviceArchitecture()) ?: return false
        val libsDirectory: File = File(directory, "libs/$archModel").takeIf { it.exists() && it.isDirectory } ?: return false
        val rendererConfigFile: File = File(directory, "config").takeIf { it.exists() && it.isFile } ?: return false
        val rendererConfig: RendererConfig = runCatching {
            Tools.GLOBAL_GSON.fromJson(readLocalRendererPluginConfig(rendererConfigFile), RendererConfig::class.java)
        }.getOrElse { e ->
            Logging.e("LocalRendererPlugin", "Failed to parse the configuration file", e)
            return false
        }
        val uniqueIdentifier = directory.name
        rendererConfig.run {
            val libPath = libsDirectory.absolutePath

            val plugin = LocalRendererPlugin(
                rendererId,
                "$rendererDisplayName (${
                    context.getString(
                        R.string.setting_renderer_from_plugins,
                        uniqueIdentifier
                    )
                })",
                uniqueIdentifier,
                glName,
                eglName.progressEglName(libPath),
                libPath,
                pojavEnv.filter { it.key != "POJAV_RENDERER" },
                dlopenList ?: emptyList(),
                directory
            )

            rendererPluginList.add(plugin)
            localRendererPluginList.add(plugin)
        }
        return true
    }

    /**
     * EGL名を処理する
     * 相対パスの場合はライブラリパスを先頭に付加する
     */
    private fun String.progressEglName(libPath: String): String =
        if (startsWith("/")) "$libPath$this"
        else this

    /**
     * ローカルレンダラープラグインの設定ファイルを読み込む
     * @param configFile 設定ファイル
     * @return ファイルの内容（UTF-8文字列）
     */
    private fun readLocalRendererPluginConfig(configFile: File): String {
        return FileInputStream(configFile).use { fileInputStream ->
            DataInputStream(fileInputStream).use { dataInputStream ->
                dataInputStream.readUTF()
            }
        }
    }

    /**
     * ローカルレンダラープラグインを圧縮ファイルからインポートする
     * @param pluginFile プラグインの圧縮ファイル
     * @return インポート成功時はtrue
     */
    fun importLocalRendererPlugin(pluginFile: File): Boolean {
        if (!pluginFile.exists() || !pluginFile.isFile) {
            Logging.i("importLocalRendererPlugin", "The compressed file does not exist or is not a valid file.")
            return false
        }

        return try {
            ZipFile(pluginFile).use { pluginZip ->
                val configEntry = pluginZip.entries().asSequence().find { it.name == "config" }
                    ?: throw IllegalArgumentException("The plugin package does not meet the requirements!")

                pluginZip.getInputStream(configEntry).use { inputStream ->
                    DataInputStream(inputStream).use { dataInputStream ->
                        val configContent = dataInputStream.readUTF()
                        Tools.GLOBAL_GSON.fromJson(configContent, RendererConfig::class.java)
                    }
                }

                val pluginFolder = File(
                    PathManager.DIR_INSTALLED_RENDERER_PLUGIN,
                    StringUtilsKt.generateUniqueUUID(
                        { string ->
                            string.replace("-", "").substring(0, 8)
                        },
                        { uuid ->
                            File(PathManager.DIR_INSTALLED_RENDERER_PLUGIN, uuid).exists()
                        }
                    )
                )

                ZipUtils.zipExtract(pluginZip, "", pluginFolder)
            }
            true
        } catch (e: Exception) {
            Logging.i("importLocalRendererPlugin", "Error: ${e.message}")
            false
        }
    }
}
