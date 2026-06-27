package com.arata.yukarilauncher.launch

import android.os.Build
import androidx.collection.ArrayMap
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.feature.accounts.AccountUtils
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome.Companion.getLibrariesHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.LibPath
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.ui.view.AWTCanvasView
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.utils.runtime.Runtime
import com.arata.yukarilauncher.utils.JSONUtils
import com.arata.yukarilauncher.value.MinecraftAccount
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile

class LaunchArgs(
    private val account: MinecraftAccount,
    private val gameDirPath: File,
    private val minecraftVersion: Version,
    private val versionInfo: JMinecraftVersionList.Version,
    private val versionFileName: String,
    private val runtime: Runtime,
    private val launchClassPath: String
) {
    private var hasClasspathInJvmArgs = false

    /**
     * ゲーム起動に必要なすべての引数を取得する
     * Java引数、Minecraft JVM引数、クラスパス、メインクラス、クライアント引数を含む
     * @return 引数リスト
     */
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())
        if (!hasClasspathInJvmArgs) {
            argsList.add("-cp")
            argsList.add("${Tools.getLWJGL3ClassPath(minecraftVersion.getLWJGLVersion())}:$launchClassPath")
        }

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = versionInfo.mainClass!!.substring(0, versionInfo.mainClass!!.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(versionInfo.mainClass ?: "")
        argsList.addAll(getMinecraftClientArgs())

        return argsList
    }

    /**
     * Java仮想マシンに渡す引数を生成する
     * PhysXフォールバック、ImGui Moulberryネイティブ、認証ライブラリ、Cacio、ログ設定などを含む
     * @return Java引数リスト
     */
    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        val physXFallbackDir = preparePhysXNativeFallback()
        physXFallbackDir?.let {
            argsList.add("-Dphysxjni.nativeLibLocation=${it.absolutePath}")
            argsList.add("-Dphysxjni.loadFromResources=false")
        }

        prepareImGuiMoulberryNativeFallback()

        if (AccountUtils.isOtherLoginAccount(account)) {
            if (account.otherBaseUrl!!.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${account.otherBaseUrl!!.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=${account.otherBaseUrl}")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        // MioLibPatcher BootstrapJarLoader needs reflective access to Unsafe
        argsList.add("--add-opens=java.base/sun.misc=ALL-UNNAMED")
        argsList.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
        argsList.add("--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED")
        argsList.add("--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED")
        argsList.add("--add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED")

        // Pre-load lazyyyy's bootstrap JAR into the boot classloader
        // to avoid SIGSEGV from Instrumentation.appendToBootstrapClassLoaderSearch()
        extractBootstrapClasspathJar()?.let { bootJar ->
            argsList.add("-Xbootclasspath/a:$bootJar")
        }

        val is7 = VersionNumber.compare(VersionNumber.asVersion(versionInfo.id ?: "0.0").canonical, "1.12") < 0
        val configFilePath = if (is7) LibPath.LOG4J_XML_1_7 else LibPath.LOG4J_XML_1_12
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")

        val libraryPath = getNativeLibrarySearchPath()
        argsList.add("-Djava.library.path=$libraryPath")

        // JNAブートライブラリパス（一部のModが依存するJNAで使用）
        argsList.add("-Djna.boot.library.path=$libraryPath")

        // LWJGL専用のネイティブライブラリ検索パス
        getLwjglNativeLibraryPath().takeIf { it.isNotEmpty() }?.let { lwjglNativeLibraryPath ->
            argsList.add("-Dorg.lwjgl.library.path=$lwjglNativeLibraryPath")
            argsList.add("-Dorg.lwjgl.librarypath=$lwjglNativeLibraryPath")
        }

        return argsList
    }


    /**
     * Scan the launch classpath for lazYYYYY's core JAR, extract its embedded
     * bootstrap JAR, and return the path for -Xbootclasspath/a: injection.
     * This avoids the SIGSEGV that Instrumentation.appendToBootstrapClassLoaderSearch()
     * triggers on Android Internal-17 JREs.
     */
    private fun extractBootstrapClasspathJar(): String? {
        val bootstrapJarName = "lazyyyyy-lexforge-bootstrap.jar"
        if (launchClassPath.isEmpty()) return null

        try {
            for (entry in launchClassPath.split(":")) {
                val f = File(entry)
                if (!f.isFile || !f.name.contains("lazyyyyy-lexforge-core")) continue
                JarFile(f).use { jar ->
                    val bootEntry: JarEntry = jar.getJarEntry(bootstrapJarName) ?: continue
                    val tmp = File.createTempFile("lazyyyyy-bootstrap-", ".jar")
                    tmp.deleteOnExit()
                    jar.getInputStream(bootEntry).use { input ->
                        FileOutputStream(tmp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Logging.i("LaunchArgs", "Extracted $bootstrapJarName -> ${tmp.absolutePath}")
                    return tmp.absolutePath
                }
            }
        } catch (e: Exception) {
            Logging.w("LaunchArgs", "Failed to extract $bootstrapJarName: $e")
        }
        return null
    }

    private fun getVersionSpecificNativesDir(): File =
        File(PathManager.DIR_CACHE, "natives/${minecraftVersion.getVersionName()}")

    // LWJGLネイティブディレクトリをキャッシュして起動時のファイルシステムアクセスを削減
    private val lwjglNativeDirs: List<File> by lazy {
        val lwjglVersion = minecraftVersion.getLWJGLVersion()
        val supportedAbis = Build.SUPPORTED_ABIS.takeIf { it.isNotEmpty() } ?: arrayOf("arm64-v8a")
        supportedAbis
            .map { abi -> File(PathManager.DIR_DATA, "lwjgl/$lwjglVersion/native/$abi") }
            .filter { nativeDir -> File(nativeDir, "liblwjgl.so").exists() }
    }

    private fun getLwjglNativeLibraryPath(): String =
        lwjglNativeDirs.joinToString(":") { it.absolutePath }

    private fun getNativeLibrarySearchPath(): String {
        val libraryDirs = ArrayList<String>()
        getVersionSpecificNativesDir().takeIf { it.exists() }?.let { libraryDirs.add(it.absolutePath) }
        lwjglNativeDirs.forEach { libraryDirs.add(it.absolutePath) }
        PathManager.DIR_MOD_LIBRARY.takeIf { it.isNotEmpty() }?.let { libraryDirs.add(it) }
        libraryDirs.add(PathManager.DIR_NATIVE_LIB)
        return libraryDirs.joinToString(":")
    }

    /**
     * MinecraftのJVM引数をバージョン情報から取得・加工する
     * @return JVM引数の配列
     */
    private fun getMinecraftJVMArgs(): Array<String> {
        val versionInfo = Tools.getVersionInfo(minecraftVersion, true)

        val varArgMap: MutableMap<String, String?> = android.util.ArrayMap()
        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = getLibrariesHome()
        varArgMap["version_name"] = versionInfo.id
        varArgMap["natives_directory"] = PathManager.DIR_NATIVE_LIB
        varArgMap["launcher_name"] = InfoDistributor.LAUNCHER_NAME
        varArgMap["launcher_version"] = YLTools.getVersionName()

        val minecraftArgs: MutableList<String> = java.util.ArrayList()
        hasClasspathInJvmArgs = false
        versionInfo.arguments?.let {
            fun Any.processJvmArg(): String? = (this as? String)?.let { argument ->
                // Keep version JSON-provided native paths from overriding the richer path
                // assembled in getJavaArgs(); otherwise LWJGL natives can disappear.
                if (argument.startsWith("-Djava.library.path=")) {
                    return@let "-Djava.library.path=${getNativeLibrarySearchPath()}"
                }
                if (argument.startsWith("-Dorg.lwjgl.library.path=") ||
                    argument.startsWith("-Dorg.lwjgl.librarypath=")) {
                    return@let getLwjglNativeLibraryPath().takeIf { it.isNotEmpty() }?.let { lwjglNativeLibraryPath ->
                        argument.substringBefore("=") + "=$lwjglNativeLibraryPath"
                    }
                }

                when {
                    argument.startsWith("-DignoreList=") -> {
                        "$argument,$versionFileName.jar"
                    }

                    argument.contains("-Dio.netty.native.workdir") ||
                        argument.contains("-Djna.tmpdir") ||
                        argument.contains("-Dorg.lwjgl.system.SharedLibraryExtractPath") -> {
                        argument.replace("\${natives_directory}", PathManager.DIR_CACHE.absolutePath)
                    }

                    argument == "\${classpath}" -> {
                        hasClasspathInJvmArgs = true
                        "${Tools.getLWJGL3ClassPath(minecraftVersion.getLWJGLVersion())}:$launchClassPath"
                    }

                    else -> argument
                }
            }

            it.jvm?.forEach { arg ->
                arg.processJvmArg()?.let(minecraftArgs::add)
            }
        }
        return JSONUtils.insertJSONValueList(minecraftArgs.toTypedArray<String>(), varArgMap as Map<String, String>)
    }

    /**
     * Minecraftクライアント引数をバージョン情報から生成する
     * 認証情報、アセットパス、ゲームディレクトリなどのプレースホルダを実際の値で置換する
     * @return クライアント引数の配列
     */
    private fun getMinecraftClientArgs(): Array<String> {
        val verArgMap: MutableMap<String, String> = ArrayMap()
        verArgMap["auth_session"] = account.accessToken
        verArgMap["auth_access_token"] = account.accessToken
        verArgMap["auth_player_name"] = account.username
        verArgMap["auth_uuid"] = account.profileId.replace("-", "")
        // 新しいMinecraftバージョンでは解決済みのclientidプレースホルダーが必要
        verArgMap["clientid"] = account.clientToken
        // アンダースコア付きのバリアントとの互換性を維持
        verArgMap["client_id"] = account.clientToken
        verArgMap["auth_xuid"] = account.xuid ?: ""
        verArgMap["assets_root"] = ProfilePathHome.getAssetsHome()
        verArgMap["assets_index_name"] = versionInfo.assets ?: ""
        verArgMap["game_assets"] = ProfilePathHome.getAssetsHome()
        verArgMap["game_directory"] = gameDirPath.absolutePath
        verArgMap["user_properties"] = "{}"
        verArgMap["user_type"] = "msa"
        val resolvedVersionName = (versionInfo.inheritsFrom ?: versionInfo.id)
            ?.takeIf { it.isNotBlank() }
            ?: minecraftVersion.getVersionName()
        verArgMap["version_name"] = resolvedVersionName
        // 一部のトランスフォーマーやカスタム引数テンプレートで使用される互換エイリアス
        verArgMap["version"] = resolvedVersionName
        verArgMap["game_version"] = resolvedVersionName

        setLauncherInfo(verArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        versionInfo.arguments?.apply {
            // Minecraft 1.13+ 対応
            game?.forEach { if (it is String) minecraftArgs.add(it) }
        }

        val finalArgs = JSONUtils.insertJSONValueList(
            splitAndFilterEmpty(
                versionInfo.minecraftArguments ?:
                Tools.fromStringArray(minecraftArgs.toTypedArray())
            ), verArgMap
        )
        val customGameArgs = minecraftVersion.getGameArgs()
        if (customGameArgs.isBlank()) return finalArgs
        return finalArgs + splitAndFilterEmpty(customGameArgs)
    }

    /**
     * ランチャー情報を引数マップに設定する
     * @param verArgMap 引数マップ
     */
    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = InfoDistributor.LAUNCHER_NAME
        verArgMap["launcher_version"] = YLTools.getVersionName() ?: ""
        verArgMap["version_type"] = minecraftVersion.getCustomInfo()
            .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: (versionInfo.type ?: "release")
    }


    /**
     * ImGui Moulberryネイティブライブラリのフォールバック処理を準備する
     * Modのネイティブライブラリを互換性のあるバイナリで上書きする
     */
    private fun prepareImGuiMoulberryNativeFallback() {
        val deviceAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a" 
        val abiTag = when { 
            deviceAbi.contains("arm64") -> "arm64" 
            deviceAbi.contains("x86_64") -> "x86_64" 
            deviceAbi.contains("x86") -> "x86" 
            deviceAbi.contains("armeabi-v7a") -> "arm" 
            else -> "arm64" 
        }
        val fallbackLib = File(PathManager.DIR_MOD_LIBRARY, "libimgui-moulberry92-java-$abiTag.so")
        if (!fallbackLib.exists()) return

        if (!isAarch64Elf(fallbackLib)) {
            Logging.w("LaunchArgs", "Ignoring invalid imgui-moulberry92 fallback library: ${fallbackLib.absolutePath}")
            return
        }

        // java.library.pathで見つけられるようにDIR_NATIVE_LIBにコピーする
        val nativeLibTarget = File(PathManager.DIR_NATIVE_LIB, fallbackLib.name)
        kotlin.runCatching {
            nativeLibTarget.parentFile?.mkdirs()
            fallbackLib.copyTo(nativeLibTarget, overwrite = true)
            Logging.i("LaunchArgs", "Prepared imgui-moulberry92 JNI fallback in native lib path: $nativeLibTarget")
        }.onFailure {
            Logging.w("LaunchArgs", "Unable to place imgui-moulberry92 JNI fallback in native lib path: $nativeLibTarget")
        }

        // キャッシュ内の既存の抽出済みライブラリを当デバイス用のビルドで置き換える
        // ModはDIR_CACHE/imgui-moulberry92-java-natives/<version>/libimgui-moulberry92-java64.so に抽出し、
        // その絶対パスでSystem.load()を呼び出すため、その場で上書きする必要がある
        val extractDir = File(PathManager.DIR_CACHE, "imgui-moulberry92-java-natives")
        if (extractDir.exists() && extractDir.isDirectory) {
            extractDir.listFiles()?.forEach { versionDir ->
                if (!versionDir.isDirectory) return@forEach
                versionDir.listFiles()?.forEach { extractedLib ->
                    if (!extractedLib.name.startsWith("libimgui-moulberry92-java") ||
                        !extractedLib.name.endsWith(".so")) return@forEach
                    kotlin.runCatching {
                        // 以前の起動でロックした場合に備えて、ディレクトリとファイルのロックを解除
                        versionDir.setWritable(true, false)
                        extractedLib.setWritable(true, false)
                        fallbackLib.copyTo(extractedLib, overwrite = true)
                        // ファイルをロックして書き込み不可にする
                        extractedLib.setReadOnly()
                        // ディレクトリをロックして抽出側がファイルを削除・再作成できないようにする
                        versionDir.setReadOnly()
                        Logging.i("LaunchArgs", "Replaced and locked imgui-moulberry92 native with fallback: $extractedLib")
                    }.onFailure {
                        Logging.w("LaunchArgs", "Unable to replace imgui-moulberry92 native at $extractedLib")
                    }
                }
            }
        }
    }

    /**
     * PhysXネイティブライブラリのフォールバック処理を準備する
     * @return フォールバックが準備できた場合はModライブラリディレクトリ、そうでない場合はnull
     */
    private fun preparePhysXNativeFallback(): File? {
        val fallbackLib = File(PathManager.DIR_MOD_LIBRARY, "libPhysXJniBindings_64.so")
        if (!fallbackLib.exists()) return null

        if (!isAarch64Elf(fallbackLib)) {
            Logging.w("LaunchArgs", "Ignoring invalid PhysX fallback library: ${fallbackLib.absolutePath}")
            return null
        }

        val libName = fallbackLib.name
        val nativeLibTarget = File(PathManager.DIR_NATIVE_LIB, libName)
        kotlin.runCatching {
            nativeLibTarget.parentFile?.mkdirs()
            fallbackLib.copyTo(nativeLibTarget, overwrite = true)
            Logging.i("LaunchArgs", "Prepared PhysX JNI fallback in native lib path: $nativeLibTarget")
        }.onFailure {
            Logging.w("LaunchArgs", "Unable to place PhysX JNI fallback in native lib path: $nativeLibTarget")
        }

        val physXPath = File(PathManager.DIR_CACHE, "de.fabmax.physx-jni")
        if (physXPath.exists() && physXPath.isDirectory) {
            physXPath.listFiles()?.forEach { versionDir ->
                val extractedLib = File(versionDir, libName)
                if (!extractedLib.exists()) return@forEach
                kotlin.runCatching {
                    fallbackLib.copyTo(extractedLib, overwrite = true)
                    Logging.i("LaunchArgs", "Replaced incompatible PhysX JNI with fallback: $extractedLib")
                }.onFailure {
                    Logging.w("LaunchArgs", "Unable to replace PhysX JNI fallback at $extractedLib")
                }
            }
        }

        return File(PathManager.DIR_MOD_LIBRARY)
    }

    /**
     * ファイルがaarch64（ARM64）のELFバイナリかどうかを検証する
     * ELFヘッダーを解析してアーキテクチャを確認する
     * @param file 検証するファイル
     * @return aarch64 ELFの場合はtrue
     */
    private fun isAarch64Elf(file: File): Boolean {
        return kotlin.runCatching {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()
                if (fileSize < 64L) return false

                val header = ByteArray(64)
                raf.seek(0)
                raf.readFully(header)

                val elfMagicValid = header[0] == 0x7f.toByte() &&
                    header[1] == 'E'.code.toByte() &&
                    header[2] == 'L'.code.toByte() &&
                    header[3] == 'F'.code.toByte()
                if (!elfMagicValid) return false

                val elfClass64 = header[4].toInt() == 2
                if (!elfClass64) return false

                val littleEndian = header[5].toInt() == 1
                if (!littleEndian) return false

                val eMachine = readU16LE(header, 18)
                if (eMachine != 183) return false

                val ePhoff = readU64LE(header, 32)
                val eShoff = readU64LE(header, 40)
                val ePhentsize = readU16LE(header, 54).toLong()
                val ePhnum = readU16LE(header, 56).toLong()
                val eShentsize = readU16LE(header, 58).toLong()
                val eShnum = readU16LE(header, 60).toLong()

                if (!isTableWithinFile(fileSize, ePhoff, ePhentsize, ePhnum)) return false
                if (!isTableWithinFile(fileSize, eShoff, eShentsize, eShnum)) return false

                true
            }
        }.getOrElse { false }
    }

    /**
     * ELFのテーブル（プログラムヘッダー/セクションヘッダー）がファイルサイズ内に収まっているかを確認する
     * @param fileSize ファイルサイズ
     * @param offset テーブルのオフセット
     * @param entrySize エントリサイズ
     * @param count エントリ数
     * @return テーブルが有効な範囲内にある場合はtrue
     */
    private fun isTableWithinFile(fileSize: Long, offset: Long, entrySize: Long, count: Long): Boolean {
        if (count == 0L) return true
        if (offset <= 0L || entrySize <= 0L) return false
        if (offset >= fileSize) return false
        if (count > Long.MAX_VALUE / entrySize) return false
        val tableSize = count * entrySize
        if (tableSize > fileSize) return false
        return offset <= fileSize - tableSize
    }

    /**
     * バイト配列からリトルエンディアンで16ビット符号なし整数を読み取る
     * @param bytes バイト配列
     * @param offset 読み取り開始位置
     * @return 16ビット整数値
     */
    private fun readU16LE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8)
    }

    /**
     * バイト配列からリトルエンディアンで64ビット符号なし整数を読み取る
     * @param bytes バイト配列
     * @param offset 読み取り開始位置
     * @return 64ビット整数値
     */
    private fun readU64LE(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[offset + i].toLong() and 0xffL) shl (8 * i))
        }
        return result
    }

    /**
     * スペース区切りの引数文字列を分割し、空要素を除去する
     * @param arg 分割する引数文字列
     * @return フィルタリングされた引数配列
     */
    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }

    companion object {
        /**
         * Caciocavallo（AWT実装）のJava引数を生成する
         * Java 8とそれ以降で異なる設定を適用する
         * @param isJava8 Java 8かどうか
         * @return Cacio関連の引数リスト
         */
        @JvmStatic
        fun getCacioJavaArgs(isJava8: Boolean): List<String> {
            val argsList: MutableList<String> = ArrayList()

            // Caciocavallo AWT対応バージョンの設定
            argsList.add("-Djava.awt.headless=false")
            argsList.add("-Dcacio.managed.screensize=" + AWTCanvasView.AWT_CANVAS_WIDTH + "x" + AWTCanvasView.AWT_CANVAS_HEIGHT)
            argsList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
            argsList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
            argsList.add("-Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel")
            if (isJava8) {
                argsList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            } else {
                argsList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")
                argsList.add("-javaagent:" + LibPath.CACIO_17_AGENT.getAbsolutePath())
                argsList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")

                // Java 9+でArc DNSインジェクターのためにjava.netパッケージを開く
                argsList.add("--add-opens=java.base/java.net=ALL-UNNAMED")
            }

            val cacioClassPath = StringBuilder()
            cacioClassPath.append("-Xbootclasspath/").append(if (isJava8) "p" else "a")
            val cacioFiles = if (isJava8) LibPath.CACIO_8 else LibPath.CACIO_17
            cacioFiles.listFiles()?.onEach {
                if (it.name.endsWith(".jar")) cacioClassPath.append(":").append(it.absolutePath)
            }

            argsList.add(cacioClassPath.toString())

            return argsList
        }
    }
}