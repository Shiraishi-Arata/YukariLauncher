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
import net.kdt.pojavlaunch.AWTCanvasView
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.value.MinecraftAccount
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.io.File

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

    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())
        if (!hasClasspathInJvmArgs) {
            argsList.add("-cp")
            argsList.add("${Tools.getLWJGL3ClassPath()}:$launchClassPath")
        }

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = versionInfo.mainClass.substring(0, versionInfo.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(versionInfo.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        return argsList
    }

    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        val physXFallbackDir = preparePhysXNativeFallback()
        physXFallbackDir?.let {
            argsList.add("-Dphysxjni.nativeLibLocation=${it.absolutePath}")
            argsList.add("-Dphysxjni.loadFromResources=false")
        }

        prepareImGuiMoulberryNativeFallback()

        if (AccountUtils.isOtherLoginAccount(account)) {
            if (account.otherBaseUrl.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${account.otherBaseUrl.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=${account.otherBaseUrl}")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        val is7 = VersionNumber.compare(VersionNumber.asVersion(versionInfo.id ?: "0.0").canonical, "1.12") < 0
        val configFilePath = if (is7) LibPath.LOG4J_XML_1_7 else LibPath.LOG4J_XML_1_12
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")

        // Build the library path string
        val versionSpecificNativesDir = File(PathManager.DIR_CACHE, "natives/${minecraftVersion.getVersionName()}")
        val libraryPath = StringBuilder()
        if (versionSpecificNativesDir.exists()) {
            libraryPath.append(versionSpecificNativesDir.absolutePath).append(":")
        }
        if (PathManager.DIR_MOD_LIBRARY.isNotEmpty()) {
            libraryPath.append(PathManager.DIR_MOD_LIBRARY).append(":")
        }
        libraryPath.append(PathManager.DIR_NATIVE_LIB)
        argsList.add("-Djava.library.path=$libraryPath")

        // JNA boot library path (used by JNA, which some mods may rely on)
        val jnaPath = StringBuilder()
        if (versionSpecificNativesDir.exists()) {
            jnaPath.append(versionSpecificNativesDir.absolutePath).append(":")
        }
        if (PathManager.DIR_MOD_LIBRARY.isNotEmpty()) {
            jnaPath.append(PathManager.DIR_MOD_LIBRARY).append(":")
        }
        jnaPath.append(PathManager.DIR_NATIVE_LIB)
        argsList.add("-Djna.boot.library.path=$jnaPath")

        return argsList
    }

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
                if (argument.startsWith("-Djava.library.path=")) {
                    return@let "-Djava.library.path=${PathManager.DIR_NATIVE_LIB}"
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
                        "${Tools.getLWJGL3ClassPath()}:$launchClassPath"
                    }

                    else -> argument
                }
            }

            it.jvm?.forEach { arg ->
                arg.processJvmArg()?.let(minecraftArgs::add)
            }
        }
        return JSONUtils.insertJSONValueList(minecraftArgs.toTypedArray<String>(), varArgMap)
    }

    private fun getMinecraftClientArgs(): Array<String> {
        val verArgMap: MutableMap<String, String> = ArrayMap()
        verArgMap["auth_session"] = account.accessToken
        verArgMap["auth_access_token"] = account.accessToken
        verArgMap["auth_player_name"] = account.username
        verArgMap["auth_uuid"] = account.profileId.replace("-", "")
        // Newer Minecraft versions (including snapshots like 26.2-snapshot-1)
        // expect a resolved client id placeholder.
        verArgMap["clientid"] = account.clientToken
        // Keep compatibility with launch argument templates using the underscored variant.
        verArgMap["client_id"] = account.clientToken
        verArgMap["auth_xuid"] = account.xuid
        verArgMap["assets_root"] = ProfilePathHome.getAssetsHome()
        verArgMap["assets_index_name"] = versionInfo.assets
        verArgMap["game_assets"] = ProfilePathHome.getAssetsHome()
        verArgMap["game_directory"] = gameDirPath.absolutePath
        verArgMap["user_properties"] = "{}"
        verArgMap["user_type"] = "msa"
        val resolvedVersionName = (versionInfo.inheritsFrom ?: versionInfo.id)
            ?.takeIf { it.isNotBlank() }
            ?: minecraftVersion.getVersionName()
        verArgMap["version_name"] = resolvedVersionName
        // Compatibility aliases used by some transformers/custom argument templates.
        verArgMap["version"] = resolvedVersionName
        verArgMap["game_version"] = resolvedVersionName

        setLauncherInfo(verArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        versionInfo.arguments?.apply {
            // Support Minecraft 1.13+
            game.forEach { if (it is String) minecraftArgs.add(it) }
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

    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = InfoDistributor.LAUNCHER_NAME
        verArgMap["launcher_version"] = YLTools.getVersionName()
        verArgMap["version_type"] = minecraftVersion.getCustomInfo()
            .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: versionInfo.type
    }


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

        // Copy to DIR_NATIVE_LIB so java.library.path can find it
        val nativeLibTarget = File(PathManager.DIR_NATIVE_LIB, fallbackLib.name)
        kotlin.runCatching {
            nativeLibTarget.parentFile?.mkdirs()
            fallbackLib.copyTo(nativeLibTarget, overwrite = true)
            Logging.i("LaunchArgs", "Prepared imgui-moulberry92 JNI fallback in native lib path: $nativeLibTarget")
        }.onFailure {
            Logging.w("LaunchArgs", "Unable to place imgui-moulberry92 JNI fallback in native lib path: $nativeLibTarget")
        }

        // Replace any already-extracted (glibc/x86_64) libs in the cache with our bionic aarch64 build.
        // The mod extracts to DIR_CACHE/imgui-moulberry92-java-natives/<version>/libimgui-moulberry92-java64.so
        // and calls System.load() on that absolute path, so we must overwrite in place.
        val extractDir = File(PathManager.DIR_CACHE, "imgui-moulberry92-java-natives")
        if (extractDir.exists() && extractDir.isDirectory) {
            extractDir.listFiles()?.forEach { versionDir ->
                if (!versionDir.isDirectory) return@forEach
                versionDir.listFiles()?.forEach { extractedLib ->
                    if (!extractedLib.name.startsWith("libimgui-moulberry92-java") ||
                        !extractedLib.name.endsWith(".so")) return@forEach
                    kotlin.runCatching {
                        // Unlock both dir and file in case we already locked them on a prior launch
                        versionDir.setWritable(true, false)
                        extractedLib.setWritable(true, false)
                        fallbackLib.copyTo(extractedLib, overwrite = true)
                        // Lock the file so it can't be opened for writing
                        extractedLib.setReadOnly()
                        // Lock the directory so the extractor can't unlink+recreate the file
                        // (unlink requires write permission on the parent directory)
                        versionDir.setReadOnly()
                        Logging.i("LaunchArgs", "Replaced and locked imgui-moulberry92 native with fallback: $extractedLib")
                    }.onFailure {
                        Logging.w("LaunchArgs", "Unable to replace imgui-moulberry92 native at $extractedLib")
                    }
                }
            }
        }
    }

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

    private fun isTableWithinFile(fileSize: Long, offset: Long, entrySize: Long, count: Long): Boolean {
        if (count == 0L) return true
        if (offset <= 0L || entrySize <= 0L) return false
        if (offset >= fileSize) return false
        if (count > Long.MAX_VALUE / entrySize) return false
        val tableSize = count * entrySize
        if (tableSize > fileSize) return false
        return offset <= fileSize - tableSize
    }

    private fun readU16LE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun readU64LE(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[offset + i].toLong() and 0xffL) shl (8 * i))
        }
        return result
    }
    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }

    companion object {
        @JvmStatic
        fun getCacioJavaArgs(isJava8: Boolean): List<String> {
            val argsList: MutableList<String> = ArrayList()

            // Caciocavallo config AWT-enabled version
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

                // Opens the java.net package to Arc DNS injector on Java 9+
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