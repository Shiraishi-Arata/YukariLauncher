package com.arata.yukarilauncher.utils.runtime

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.event.value.JvmExitEvent
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionInfo
import com.arata.yukarilauncher.plugins.driver.DriverPluginManager
import com.arata.yukarilauncher.plugins.renderer.RendererPluginManager
import com.arata.yukarilauncher.plugins.renderer.RendererPlugin
import com.arata.yukarilauncher.renderer.RendererInterface
import com.arata.yukarilauncher.renderer.Renderers
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.ui.activity.ErrorActivity
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.LibPath
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.plugins.FFmpegPlugin
import com.google.gson.Gson
import com.oracle.dalvik.VMLauncher
import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.TimeZone
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/** JRE（Java Runtime Environment）のロード、環境変数設定、JVM起動ユーティリティを提供するオブジェクト。 */
object JREUtils {

    /** AWT画面フレームをレンダリングする（JNI）。 @return ピクセルデータの配列 */
    @JvmStatic external fun renderAWTScreenFrame(): IntArray?

    /** LD_LIBRARY_PATH環境変数の値。 */
    var LD_LIBRARY_PATH: String? = null
    /** JVMライブラリ（libjvm.so）のパス。 */
    var jvmLibraryPath: String? = null

    /**
     * LD_LIBRARY_PATHからライブラリの絶対パスを検索する。
     * @param libName ライブラリ名
     * @return 見つかった絶対パス、または元のライブラリ名
     */
    fun findInLdLibPath(libName: String): String {
        if (Os.getenv("LD_LIBRARY_PATH") == null) {
            try {
                if (LD_LIBRARY_PATH != null) {
                    Os.setenv("LD_LIBRARY_PATH", LD_LIBRARY_PATH, true)
                }
            } catch (e: ErrnoException) {
                Logging.e("JREUtils", Tools.printToString(e))
            }
            return libName
        }
        for (libPath in Os.getenv("LD_LIBRARY_PATH").split(":")) {
            val f = File(libPath, libName)
            if (f.exists() && f.isFile) {
                return f.absolutePath
            }
        }
        return libName
    }

    /**
     * ディレクトリ内の.soファイルを再帰的に検索する。
     * @param path 検索開始ディレクトリ
     * @return 見つかった.soファイルのリスト
     */
    fun locateLibs(path: File): ArrayList<File> {
        val returnValue = ArrayList<File>()
        val list = path.listFiles()
        if (list != null) {
            for (f in list) {
                if (f.isFile && f.name.endsWith(".so")) {
                    returnValue.add(f)
                } else if (f.isDirectory) {
                    returnValue.addAll(locateLibs(f))
                }
            }
        }
        return returnValue
    }

    /**
     * Javaランタイムのネイティブライブラリをロードする。
     * @param jreHome JREホームディレクトリ
     */
    fun initJavaRuntime(jreHome: String) {
        dlopen(findInLdLibPath("libjli.so"))
        if (!dlopen("libjvm.so")) {
            Logging.w("DynamicLoader", "Failed to load with no path, trying with full path")
            dlopen("$jvmLibraryPath/libjvm.so")
        }
        dlopen(findInLdLibPath("libverify.so"))
        dlopen(findInLdLibPath("libjava.so"))
        dlopen(findInLdLibPath("libnet.so"))
        dlopen(findInLdLibPath("libnio.so"))
        dlopen(findInLdLibPath("libawt.so"))
        dlopen(findInLdLibPath("libawt_headless.so"))
        dlopen(findInLdLibPath("libfreetype.so"))
        dlopen(findInLdLibPath("libfontmanager.so"))
        for (f in locateLibs(File(jreHome, Tools.DIRNAME_HOME_JRE))) {
            dlopen(f.absolutePath)
        }
    }

    /** JREのログ出力をlogcat経由でリダイレクトして表示する。 */
    fun redirectAndPrintJRELog() {
        Logging.v("jrelog", "Log starts here")
        Thread(Runnable {
            var failTime = 0
            var logcatPb: ProcessBuilder? = null
            fun runLogcat() {
                try {
                    if (logcatPb == null) {
                        logcatPb = ProcessBuilder().command("logcat", "-v", "brief", "-s", "jrelog:I", "LIBGL:I", "NativeInput").redirectErrorStream(true)
                    }

                    Logging.i("jrelog-logcat", "Clearing logcat")
                    ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start()
                    Logging.i("jrelog-logcat", "Starting logcat")
                    val p = logcatPb!!.start()

                    val buf = ByteArray(1024)
                    var len: Int
                    while (p.inputStream.read(buf).also { len = it } != -1) {
                        val currStr = String(buf, 0, len)
                        Logger.appendToLog(currStr)
                    }

                    if (p.waitFor() != 0) {
                        Logging.e("jrelog-logcat", "Logcat exited with code " + p.exitValue())
                        failTime++
                        Logging.i("jrelog-logcat", (if (failTime <= 10) "Restarting logcat" else "Too many restart fails") + " (attempt $failTime/10")
                        if (failTime <= 10) {
                            runLogcat()
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more Logging.")
                        }
                    }
                } catch (e: Throwable) {
                    Logging.e("jrelog-logcat", "Exception on logging thread", e)
                    Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e))
                }
            }
            runLogcat()
        }).start()
        Logging.i("jrelog-logcat", "Logcat thread started")
    }

    /**
     * JREのアーキテクチャに基づいてライブラリパスを再配置する。
     * @param runtime ランタイム情報
     * @param jreHome JREホームディレクトリ
     */
    fun relocateLibPath(runtime: Runtime, jreHome: String) {
        var JRE_ARCHITECTURE = runtime.arch!!
        if (Architecture.archAsInt(JRE_ARCHITECTURE) == Architecture.ARCH_X86) {
            JRE_ARCHITECTURE = "i386/i486/i586"
        }

        for (arch in JRE_ARCHITECTURE.split("/")) {
            val f = File(jreHome, "lib/$arch")
            if (f.exists() && f.isDirectory) {
                Tools.DIRNAME_HOME_JRE = "lib/$arch"
            }
        }

        val libName = if (Architecture.is64BitsDevice()) "lib64" else "lib"
        val ldLibraryPath = StringBuilder()
        if (FFmpegPlugin.isAvailable) {
            ldLibraryPath.append(FFmpegPlugin.libraryPath).append(":")
        }
        val customRenderer = RendererPluginManager.selectedRendererPlugin
        if (customRenderer != null) {
            ldLibraryPath.append(customRenderer.path).append(":")
        }
        ldLibraryPath.append(jreHome)
                .append("/").append(Tools.DIRNAME_HOME_JRE)
                .append("/jli:").append(jreHome).append("/").append(Tools.DIRNAME_HOME_JRE)
                .append(":")
        ldLibraryPath.append("/system/").append(libName).append(":")
                .append("/vendor/").append(libName).append(":")
                .append("/vendor/").append(libName).append("/hw:")
        val runtimeModDir = PathManager.DIR_RUNTIME_MOD
        if (runtimeModDir != null) {
            ldLibraryPath.append(runtimeModDir.absolutePath).append(":")
        }
        if (PathManager.DIR_MOD_LIBRARY.isNotEmpty()) {
            ldLibraryPath.append(PathManager.DIR_MOD_LIBRARY).append(":")
        }
        ldLibraryPath.append(PathManager.DIR_NATIVE_LIB)
        LD_LIBRARY_PATH = ldLibraryPath.toString()
    }

    /**
     * LD_LIBRARY_PATHを初期化して設定する。
     * @param jreHome JREホームディレクトリ
     */
    private fun initLdLibraryPath(jreHome: String) {
        val serverFile = File("$jreHome/${Tools.DIRNAME_HOME_JRE}/server/libjvm.so")
        jvmLibraryPath = "$jreHome/${Tools.DIRNAME_HOME_JRE}/${if (serverFile.exists()) "server" else "client"}"
        Logging.d("DynamicLoader", "Base LD_LIBRARY_PATH: $LD_LIBRARY_PATH")
        Logging.d("DynamicLoader", "Internal LD_LIBRARY_PATH: $jvmLibraryPath:$LD_LIBRARY_PATH")
        setLdLibraryPath("$jvmLibraryPath:$LD_LIBRARY_PATH")
    }

    /**
     * Java実行に必要な環境変数を設定する。
     * @param envMap 環境変数マップ
     * @param jreHome JREホームディレクトリ
     */
    private fun setJavaEnv(envMap: MutableMap<String, String>, jreHome: String) {
        envMap["POJAV_NATIVEDIR"] = PathManager.DIR_NATIVE_LIB
        envMap["DRIVER_PATH"] = DriverPluginManager.getDriver().path
        envMap["JAVA_HOME"] = jreHome
        envMap["HOME"] = PathManager.DIR_GAME_HOME
        envMap["TMPDIR"] = PathManager.DIR_CACHE.absolutePath
        envMap["LD_LIBRARY_PATH"] = LD_LIBRARY_PATH ?: ""
        envMap["PATH"] = "$jreHome/bin:${Os.getenv("PATH")}"
        envMap["FORCE_VSYNC"] = AllSettings.forceVsync.getValue().toString()
        envMap["AWTSTUB_WIDTH"] = (if (CallbackBridge.windowWidth > 0) CallbackBridge.windowWidth else CallbackBridge.physicalWidth).toString()
        envMap["AWTSTUB_HEIGHT"] = (if (CallbackBridge.windowHeight > 0) CallbackBridge.windowHeight else CallbackBridge.physicalHeight).toString()
        envMap["MOD_ANDROID_RUNTIME"] = PathManager.DIR_RUNTIME_MOD?.absolutePath ?: ""
        envMap["YUKARI_MOD_LIBRARY_DIR"] = PathManager.DIR_MOD_LIBRARY

        if (AllSettings.dumpShaders.getValue()) envMap["LIBGL_VGPU_DUMP"] = "1"
        if (AllSettings.zinkPreferSystemDriver.getValue()) envMap["POJAV_ZINK_PREFER_SYSTEM_DRIVER"] = "1"
        if (AllSettings.vsyncInZink.getValue()) envMap["POJAV_VSYNC_IN_ZINK"] = "1"
        if (AllSettings.bigCoreAffinity.getValue()) envMap["POJAV_BIG_CORE_AFFINITY"] = "1"
        if (FFmpegPlugin.isAvailable) envMap["POJAV_FFMPEG_PATH"] = FFmpegPlugin.executablePath ?: ""
    }

    /**
     * レンダラー関連の環境変数を設定する。
     * @param envMap 環境変数マップ
     */
    private fun setRendererEnv(envMap: MutableMap<String, String>) {
        val currentRenderer = Renderers.getCurrentRenderer()
        val rendererId = currentRenderer.getRendererId()

        if (rendererId.startsWith("opengles2")) {
            envMap["LIBGL_ES"] = "2"
            envMap["LIBGL_MIPMAP"] = "3"
            envMap["LIBGL_NOERROR"] = "1"
            envMap["LIBGL_NOINTOVLHACK"] = "1"
            envMap["LIBGL_NORMALIZE"] = "1"
        }

        envMap.putAll(currentRenderer.getRendererEnv().value)

        val eglName = currentRenderer.getRendererEGL()
        if (eglName != null) envMap["POJAVEXEC_EGL"] = eglName

        envMap["POJAV_RENDERER"] = rendererId

        if (RendererPluginManager.selectedRendererPlugin != null) {
            if (!envMap.containsKey("LIBGL_ES")) {
                envMap["LIBGL_ES"] = "3"
            }
            return
        }

        when {
            rendererId == "opengles3" -> {
                envMap["LIBGL_ES"] = "3"
                envMap["POJAV_RENDERER"] = "opengles3"
                envMap["POJAVEXEC_EGL"] = "libmobileglues.so"
                // MobileGlues 設定ファイルを書き込む
                writeMobileGluesConfig()
                envMap["MG_DIR_PATH"] = getMobileGluesDir().absolutePath
            }
            rendererId.startsWith("opengles") -> {
                envMap["LIBGL_ES"] = "2"
                envMap["LIBGL_MIPMAP"] = "3"
                envMap["LIBGL_NORMALIZE"] = "1"
                envMap["LIBGL_NOINTOVLHACK"] = "1"
                envMap["LIBGL_NOERROR"] = "1"
            }
            rendererId == "nggl4es" -> {
                envMap["LIBGL_USE_MC_COLOR"] = "1"
                envMap["DLOPEN"] = "libspirv-cross.so"
                envMap["LIBGL_GL"] = "31"
                envMap["LIBGL_ES"] = "3"
                envMap["LIBGL_NORMALIZE"] = "1"
                envMap["LIBGL_NOINTOVLHACK"] = "1"
                envMap["LIBGL_NOERROR"] = "1"
                envMap["POJAV_RENDERER"] = "nggl4es"
                envMap["POJAVEXEC_EGL"] = "libEGL.so"
                envMap["NGG_DIR_PATH"] = PathManager.DIR_CACHE.absolutePath
            }
            rendererId == "ltw" -> {
                envMap["LIBGL_ES"] = "3"
                envMap["LIBGL_NOERROR"] = "1"
                envMap["POJAV_RENDERER"] = "ltw"
                envMap["POJAVEXEC_EGL"] = "libltw.so"
            }
            else -> {
                envMap["MESA_GLSL_CACHE_DIR"] = PathManager.DIR_CACHE.absolutePath
                envMap["MESA_GL_VERSION_OVERRIDE"] = if (rendererId == "gallium_virgl") "4.3" else "4.6"
                envMap["MESA_GLSL_VERSION_OVERRIDE"] = if (rendererId == "gallium_virgl") "430" else "460"
                envMap["force_glsl_extensions_warn"] = "true"
                envMap["allow_higher_compat_version"] = "true"
                envMap["allow_glsl_extension_directive_midshader"] = "true"
                envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
                envMap["LIB_MESA_NAME"] = loadGraphicsLibrary() ?: ""
                when (rendererId) {
                    "gallium_virgl" -> {
                        envMap["POJAV_RENDERER"] = "gallium_virgl"
                        envMap["OSMESA_NO_FLUSH_FRONTBUFFER"] = "1"
                    }
                    "vulkan_zink" -> envMap["POJAV_RENDERER"] = "vulkan_zink"
                    "freedreno" -> envMap["POJAV_RENDERER"] = "gallium_freedreno"
                    "gallium_panfrost" -> envMap["POJAV_RENDERER"] = "gallium_panfrost"
                    "kopper_zink" -> {
                        envMap["POJAV_RENDERER"] = "kopper_zink"
                        envMap["POJAVEXEC_EGL"] = "libEGL_mesa.so"
                        if (Tools.shouldUseUBWC()) envMap["FD_DEV_FEATURES"] = "enable_tp_ubwc_flag_hint=1"
                    }
                }
            }
        }

        if (!envMap.containsKey("LIBGL_ES")) {
            val glesMajor = getDetectedVersion()
            Logging.i("glesDetect", "GLES version detected: $glesMajor")

            if (glesMajor < 3) {
                envMap["LIBGL_ES"] = "2"
            } else if (rendererId.startsWith("opengles")) {
                envMap["LIBGL_ES"] = rendererId.replace("opengles", "").replace("_5", "")
            } else {
                envMap["LIBGL_ES"] = "3"
            }
        }
    }

    /** custom_env.txtからカスタム環境変数を読み込んで設定する。 @param envMap 環境変数マップ */
    private fun setCustomEnv(envMap: MutableMap<String, String>) {
        val customEnvFile = File(PathManager.DIR_GAME_HOME, "custom_env.txt")
        if (customEnvFile.exists() && customEnvFile.isFile) {
            BufferedReader(FileReader(customEnvFile)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val index = line!!.indexOf("=")
                envMap[line!!.substring(0, index)] = line!!.substring(index + 1)
            }
        }
        }
    }

    /**
     * Java 17/21用のJSPH（Java Signal Proxy Handler）ライブラリを設定する。
     * @param envMap 環境変数マップ
     * @param runtime ランタイム情報
     */
    private fun checkAndUsedJSPH(envMap: MutableMap<String, String>, runtime: Runtime) {
        val onUseJSPH = runtime.javaVersion > 11
        if (!onUseJSPH) return
        val dir = File(PathManager.DIR_NATIVE_LIB)
        if (!dir.isDirectory) return
        val jsphName = if (runtime.javaVersion == 17) "libjsph17" else "libjsph21"
        val files = dir.listFiles { _, name -> name.startsWith(jsphName) }
        if (files != null && files.isNotEmpty()) {
            val libName = "${PathManager.DIR_NATIVE_LIB}/$jsphName.so"
            envMap["JSP"] = libName
        }
    }

    /**
     * JRE環境全体を設定する。
     * @param jreHome JREホームディレクトリ
     * @param runtime ランタイム情報
     * @param gameVersion ゲームバージョン情報
     */
    private fun setEnv(jreHome: String, runtime: Runtime, gameVersion: Version?) {
        val envMap: MutableMap<String, String> = ArrayMap()

        setJavaEnv(envMap, jreHome)
        setCustomEnv(envMap)

        if (gameVersion != null) {
            checkAndUsedJSPH(envMap, runtime)

            val versionInfo = gameVersion.getVersionInfo()
            if (versionInfo != null && versionInfo.loaderInfo != null) {
                for (loaderInfo in versionInfo.loaderInfo) {
                    val loaderEnvKey = loaderInfo.getLoaderEnvKey()
                    if (loaderEnvKey != null) {
                        envMap[loaderEnvKey] = "1"
                    }
                }
            }

            if (Renderers.isCurrentRendererValid()) {
                setRendererEnv(envMap)
            }

            envMap["YUKARI_VERSION_CODE"] = YLTools.getVersionCode().toString()
        }

        for (env in envMap) {
            Logger.appendToLog("Added custom env: ${env.key}=${env.value}")
            try {
                Os.setenv(env.key, env.value, true)
            } catch (exception: NullPointerException) {
                Logging.e("JREUtils", exception.toString())
            }
        }
    }

    /**
     * グラフィックおよびサウンドエンジンを初期化する。
     * @param renderer レンダラーを使用するかどうか
     */
    private fun initGraphicAndSoundEngine(renderer: Boolean) {
        dlopen("${PathManager.DIR_NATIVE_LIB}/libopenal.so")

        if (!renderer) return

        val rendererLib = loadGraphicsLibrary()
        val customRenderer = RendererPluginManager.selectedRendererPlugin

        if (customRenderer != null) {
            customRenderer.dlopen.forEach { lib -> dlopen("${customRenderer.path}/$lib") }
        }

        if (!dlopen(rendererLib ?: "") && !dlopen(findInLdLibPath(rendererLib ?: ""))) {
            Logging.e("RENDER_LIBRARY", "Failed to load renderer $rendererLib")
        }
    }

    /**
     * Java VMを起動する。
     * @param activity アクティビティ
     * @param runtimeHome ランタイムホームディレクトリ
     * @param gameVersion ゲームバージョン情報
     * @param JVMArgs JVM引数リスト
     * @param userArgsString ユーザー定義引数文字列
     */
    private fun launchJavaVM(
            activity: AppCompatActivity,
            runtimeHome: String,
            gameVersion: Version?,
            JVMArgs: List<String>,
            userArgsString: String
    ) {
        val userArgs = getJavaArgs(runtimeHome, userArgsString)
        purgeArg(userArgs, "-Xms")
        purgeArg(userArgs, "-Xmx")
        purgeArg(userArgs, "-d32")
        purgeArg(userArgs, "-d64")
        purgeArg(userArgs, "-Xint")
        purgeArg(userArgs, "-XX:+UseTransparentHugePages")
        purgeArg(userArgs, "-XX:+UseLargePagesInMetaspace")
        purgeArg(userArgs, "-XX:+UseLargePages")
        purgeArg(userArgs, "-Dorg.lwjgl.opengl.libname")
        purgeArg(userArgs, "-Dorg.lwjgl.freetype.libname")
        purgeArg(userArgs, "-XX:ActiveProcessorCount")

        userArgs.add("-javaagent:" + LibPath.MIO_LIB_PATCHER.absolutePath)

        var arch = System.getProperty("os.arch")
        when {
            arch.contains("aarch64") || arch.contains("arm64") -> arch = "arm64"
            arch.contains("arm") -> arch = "arm"
            arch.contains("x86_64") || arch.contains("amd64") -> arch = "x86_64"
            arch.contains("86") -> arch = "x86"
        }

        val sableRapierLib = File(PathManager.DIR_MOD_LIBRARY, "libsable_rapier-$arch.so")
        if (sableRapierLib.exists()) {
            userArgs.add("-Dsable_rapier_path=" + sableRapierLib.absolutePath)
        }

        val imguiMoulberry = File(PathManager.DIR_MOD_LIBRARY, "libimgui-moulberry92-java-$arch.so")
        if (imguiMoulberry.exists()) {
            userArgs.add("-Dimgui_moulberry_path=" + imguiMoulberry.absolutePath)
        }

        userArgs.add("-Xms" + AllSettings.ramAllocation.value.getValue() + "M")
        userArgs.add("-Xmx" + AllSettings.ramAllocation.value.getValue() + "M")
        if (Renderers.isCurrentRendererValid()) userArgs.add("-Dorg.lwjgl.opengl.libname=" + loadGraphicsLibrary())

        userArgs.add("-Dorg.lwjgl.freetype.libname=${PathManager.DIR_NATIVE_LIB}/libfreetype.so")

        userArgs.add("-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors())

        userArgs.addAll(JVMArgs)
        activity.runOnUiThread { Toast.makeText(activity, activity.getString(R.string.autoram_info_msg, AllSettings.ramAllocation.value.getValue()), Toast.LENGTH_SHORT).show() }
        println(JVMArgs)
        for (i in userArgs.indices) {
            val arg = userArgs[i]
            if (arg.startsWith("--accessToken")) {
                i + 1
            }
            Logger.appendToLog("JVMArg: $arg")
        }

        setupExitMethod(activity.application)
        initializeGameExitHook()
        chdir((gameVersion ?: ProfilePathHome.getGameHome()).let { if (it is String) it else (it as Version).getGameDir().absolutePath })
        userArgs.add(0, "java")

        val exitCode = VMLauncher.launchJVM(userArgs.toTypedArray())
        Logger.appendToLog("Java Exit code: $exitCode")
        if (exitCode != 0) {
            ErrorActivity.showExitMessage(activity, exitCode, false)
        }
        EventBus.getDefault().post(JvmExitEvent(exitCode))
    }

    /**
     * 統合JVM起動処理。ランタイムのパス解決、環境変数設定、ライブラリロード、VM起動を順次実行する。
     * @param activity アクティビティ
     * @param runtime ランタイム情報
     * @param gameVersion ゲームバージョン情報
     * @param JVMArgs JVM引数リスト
     * @param userArgsString ユーザー定義引数文字列
     */
    fun launchWithUtils(
            activity: AppCompatActivity,
            runtime: Runtime,
            gameVersion: Version?,
            JVMArgs: List<String>,
            userArgsString: String
    ) {
        val runtimeHome = MultiRTUtils.getRuntimeHome(runtime.name).absolutePath

        relocateLibPath(runtime, runtimeHome)

        initLdLibraryPath(runtimeHome)

        setEnv(runtimeHome, runtime, gameVersion)

        initJavaRuntime(runtimeHome)

        initGraphicAndSoundEngine(gameVersion != null)

        launchJavaVM(activity, runtimeHome, gameVersion, JVMArgs, userArgsString)
    }

    /**
     * JVM引数を生成する。ユーザー定義引数と上書き可能なデフォルト引数をマージする。
     * @param runtimeHome ランタイムホーム
     * @param userArgumentsString ユーザー定義引数文字列
     * @return 最終的なJVM引数リスト
     */
    fun getJavaArgs(runtimeHome: String, userArgumentsString: String): MutableList<String> {
        val userArguments = parseJavaArguments(userArgumentsString)
        val resolvFile = File(PathManager.DIR_DATA, "resolv.conf").absolutePath

        val overridableArguments = listOf(
                "-Djava.home=$runtimeHome",
                "-Djava.io.tmpdir=" + PathManager.DIR_CACHE.absolutePath,
                "-Djna.boot.library.path=" + PathManager.DIR_NATIVE_LIB,
                "-Duser.home=" + ProfilePathManager.getCurrentPath(),
                "-Duser.language=" + System.getProperty("user.language"),
                "-Dos.name=Linux",
                "-Dos.version=Android-" + Build.VERSION.RELEASE,
                "-Dpojav.path.minecraft=" + ProfilePathHome.getGameHome(),
                "-Dpojav.path.private.account=" + PathManager.DIR_ACCOUNT_NEW,
                "-Duser.timezone=" + TimeZone.getDefault().id,

                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
                "-Dglfwstub.windowWidth=" + Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.widthPixels, AllSettings.resolutionRatio.getValue() / 100F),
                "-Dglfwstub.windowHeight=" + Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.heightPixels, AllSettings.resolutionRatio.getValue() / 100F),
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=$resolvFile",
                "-Dlog4j2.formatMsgNoLookups=true",

                "-Dnet.minecraft.clientmodname=" + InfoDistributor.LAUNCHER_NAME,
                "-Dfml.earlyprogresswindow=false",
                "-Dloader.disable_forked_guis=true",
                "-Djdk.lang.Process.launchMechanism=FORK",
                "-Dsodium.checks.issue2561=false"
        )

        val additionalArguments = ArrayList<String>()
        for (arg in overridableArguments) {
            val strippedArg = arg.substring(0, arg.indexOf('='))
            var add = true
            for (uarg in userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false
                    break
                }
            }
            if (add) additionalArguments.add(arg)
            else Logging.i("ArgProcessor", "Arg skipped: $arg")
        }

        userArguments.addAll(additionalArguments)
        return userArguments
    }

    /**
     * ユーザー定義のJVM引数文字列をパースする。
     * @param args JVM引数文字列
     * @return パースされた引数リスト
     */
    fun parseJavaArguments(args: String): ArrayList<String> {
        val parsedArguments = ArrayList<String>(0)
        var processedArgs = args.trim().replace(" ", "")
        val separators = listOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")
        for (prefix in separators) {
            while (true) {
                val start = processedArgs.indexOf(prefix)
                if (start == -1) break
                var end = -1
                for (separator in separators) {
                    val tempEnd = processedArgs.indexOf(separator, start + prefix.length)
                    if (tempEnd == -1) continue
                    if (end == -1) {
                        end = tempEnd
                        continue
                    }
                    end = minOf(end, tempEnd)
                }
                if (end == -1) end = processedArgs.length

                val parsedSubString = processedArgs.substring(start, end)
                processedArgs = processedArgs.replace(parsedSubString, "")

                if (parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    val arraySize = parsedArguments.size
                    if (arraySize > 0) {
                        val lastString = parsedArguments[arraySize - 1]
                        if (lastString[lastString.length - 1] == ',' || parsedSubString.contains(",")) {
                            parsedArguments[arraySize - 1] = lastString + parsedSubString
                            continue
                        }
                    }
                    parsedArguments.add(parsedSubString)
                } else Logging.w("JAVA ARGS PARSER", "Removed improper arguments: $parsedSubString")
            }
        }
        return parsedArguments
    }

    /**
     * グラフィックライブラリのパスをロードする。
     * @return ライブラリパス、レンダラーが無効な場合は null
     */
    fun loadGraphicsLibrary(): String? {
        if (!Renderers.isCurrentRendererValid()) return null
        else {
            val rendererPlugin = RendererPluginManager.selectedRendererPlugin
            return if (rendererPlugin != null) {
                "${rendererPlugin.path}/${rendererPlugin.glName}"
            } else {
                Renderers.getCurrentRenderer().getRendererLibrary()
            }
        }
    }

    /**
     * 引数リストから指定された接頭辞で始まる引数を除去する。
     * @param argList 引数リスト
     * @param argStart 除去する接頭辞
     */
    private fun purgeArg(argList: MutableList<String>, argStart: String) {
        argList.removeAll { it.startsWith(argStart) }
    }

    /** OpenGL ES 1.x ビット。 */
    private const val EGL_OPENGL_ES_BIT = 0x0001
    /** OpenGL ES 2.0 ビット。 */
    private const val EGL_OPENGL_ES2_BIT = 0x0004
    /** OpenGL ES 3.0 ビット（KHR拡張）。 */
    private const val EGL_OPENGL_ES3_BIT_KHR = 0x0040

    /**
     * 拡張文字列中に指定された拡張が存在するか確認する。
     * @param extensions 拡張文字列
     * @param name 検索する拡張名
     * @return 存在する場合は true
     */
    private fun hasExtension(extensions: String, name: String): Boolean {
        var start = extensions.indexOf(name)
        while (start >= 0) {
            val end = start + name.length
            if (end == extensions.length || extensions[end] == ' ') {
                return true
            }
            start = extensions.indexOf(name, end)
        }
        return false
    }

    /**
     * デバイスでサポートされているOpenGL ESの最大メジャーバージョンを検出する。
     * @return GLESメジャーバージョン（エラー時は負の値）
     */
    fun getDetectedVersion(): Int {
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val numConfigs = IntArray(1)
        if (egl.eglInitialize(display, null)) {
            try {
                val checkES3 = hasExtension(egl.eglQueryString(display, EGL10.EGL_EXTENSIONS),
                        "EGL_KHR_create_context")
                if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                    val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
                    if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                        var highestEsVersion = 0
                        val value = IntArray(1)
                        for (i in 0 until numConfigs[0]) {
                            if (egl.eglGetConfigAttrib(display, configs[i],
                                            EGL10.EGL_RENDERABLE_TYPE, value)) {
                                if (checkES3 && value[0] and EGL_OPENGL_ES3_BIT_KHR == EGL_OPENGL_ES3_BIT_KHR) {
                                    if (highestEsVersion < 3) highestEsVersion = 3
                                } else if (value[0] and EGL_OPENGL_ES2_BIT == EGL_OPENGL_ES2_BIT) {
                                    if (highestEsVersion < 2) highestEsVersion = 2
                                } else if (value[0] and EGL_OPENGL_ES_BIT == EGL_OPENGL_ES_BIT) {
                                    if (highestEsVersion < 1) highestEsVersion = 1
                                }
                            } else {
                                Logging.w("glesDetect", "Getting config attribute with "
                                        + "EGL10#eglGetConfigAttrib failed "
                                        + "($i/${numConfigs[0]}): "
                                        + egl.eglGetError())
                            }
                        }
                        return highestEsVersion
                    } else {
                        Logging.e("glesDetect", "Getting configs with EGL10#eglGetConfigs failed: "
                                + egl.eglGetError())
                        return -1
                    }
                } else {
                    Logging.e("glesDetect", "Getting number of configs with EGL10#eglGetConfigs failed: "
                            + egl.eglGetError())
                    return -2
                }
            } finally {
                egl.eglTerminate(display)
            }
        } else {
            Logging.e("glesDetect", "Couldn't initialize EGL.")
            return -3
        }
    }

    /**
     * MobileGlues の設定ディレクトリをアプリのキャッシュ内に取得する
     * @return MG設定ディレクトリ（存在しない場合は作成される）
     */
    private fun getMobileGluesDir(): File {
        val dir = File(PathManager.DIR_CACHE, "MG")
        dir.mkdirs()
        return dir
    }

    /**
     * MobileGlues 設定ファイル（config.json）を書き込む
     * AllSettings の mg_* 設定値を読み取り、MGネイティブライブラリが解釈する形式のJSONとして出力する
     * ゲーム起動時に setRendererEnv() 内から呼ばれる
     */
    private fun writeMobileGluesConfig() {
        val config = com.google.gson.JsonObject().apply {
            addProperty("enableANGLE", AllSettings.mgAngle.getValue().toIntOrNull() ?: 1)
            // 0=DisableIfPossible, 1=EnableIfPossible, 2=ForceDisable, 3=ForceEnable
            addProperty("enableNoError", AllSettings.mgNoError.getValue().toIntOrNull() ?: 0)
            // 0=Auto, 1=Disable, 2=Level1, 3=Level2
            addProperty("enableExtTimerQuery", if (AllSettings.mgExtTimerQuery.getValue()) 1 else 0)
            // 0=有効（推奨）, 1=無効化（UIスイッチON時）
            addProperty("enableExtComputeShader", if (AllSettings.mgExtComputeShader.getValue()) 1 else 0)
            // 不完全なARB_compute_shader拡張
            addProperty("enableExtDirectStateAccess", if (AllSettings.mgExtDirectStateAccess.getValue()) 1 else 0)
            // 実験的なdirect_state_access拡張
            addProperty("maxGlslCacheSize", AllSettings.mgGlslCacheSize.getValue().toIntOrNull() ?: 32)
            // MB単位、-1で無効化
            addProperty("multidrawMode", AllSettings.mgMultidrawMode.getValue().toIntOrNull() ?: 0)
            // 0=Auto, 1=Indirect, 2=BaseVertex, 3=MultidrawIndirect, 4=DrawElements, 5=Compute
            addProperty("angleDepthClearFixMode", AllSettings.mgAngleDepthClearFixMode.getValue().toIntOrNull() ?: 0)
            // 0=Disable, 1=Mode1
            addProperty("customGLVersion", AllSettings.mgCustomGLVersion.getValue().toIntOrNull() ?: 0)
            // 0=無効, 32/33/40-46
            addProperty("fsr1Setting", if (AllSettings.mgFsr1.getValue()) 1 else 0)
            // FSR1超解像度
            addProperty("hideMGEnvLevel", if (AllSettings.mgHideMG.getValue()) 1 else 0)
            // F3画面からMG情報を隠す
        }
        val configFile = File(getMobileGluesDir(), "config.json")
        configFile.writeText(Gson().toJson(config))
        Logging.i("MobileGlues", "Config written to ${configFile.absolutePath}")
    }

    /** @param path カレントディレクトリを変更するパス @return 成功時は0 */
    @JvmStatic external fun chdir(path: String): Int
    @JvmStatic external fun dlopen(libPath: String): Boolean
    @JvmStatic external fun setLdLibraryPath(ldLibraryPath: String)
    @JvmStatic external fun setupBridgeWindow(surface: Any?)
    @JvmStatic external fun releaseBridgeWindow()
    @JvmStatic external fun initializeGameExitHook()
    @JvmStatic external fun setupExitMethod(context: Context)

    init {
        System.loadLibrary("exithook")
        System.loadLibrary("pojavexec")
        System.loadLibrary("pojavexec_awt")
    }
}