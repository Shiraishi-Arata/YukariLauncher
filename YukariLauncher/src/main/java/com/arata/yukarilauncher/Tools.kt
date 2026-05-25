package com.arata.yukarilauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.ArrayMap
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.context.ContextExecutorTask
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.feature.OldVersionsUtils
import com.arata.yukarilauncher.feature.PojavProfile
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.plugins.FFmpegPlugin
import com.arata.yukarilauncher.renderer.GLInfoUtils
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.setting.AllStaticSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.ui.activity.BaseActivity
import com.arata.yukarilauncher.ui.activity.JavaGUILauncherActivity
import com.arata.yukarilauncher.ui.activity.LauncherActivity
import com.arata.yukarilauncher.ui.activity.MainActivity
import com.arata.yukarilauncher.ui.activity.MissingStorageActivity
import com.arata.yukarilauncher.ui.activity.ShowErrorActivity
import com.arata.yukarilauncher.ui.dialog.EditTextDialog
import com.arata.yukarilauncher.ui.dialog.LifecycleAwareAlertDialog
import com.arata.yukarilauncher.ui.view.AWTCanvasView
import com.arata.yukarilauncher.utils.DateUtils
import com.arata.yukarilauncher.utils.JSONUtils
import com.arata.yukarilauncher.utils.LauncherProfiles
import com.arata.yukarilauncher.utils.file.FileUtils
import com.arata.yukarilauncher.utils.http.DownloadUtils
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.utils.platform.MemoryHoleFinder
import com.arata.yukarilauncher.utils.platform.SelfMapsParser
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.utils.runtime.SelectRuntimeUtils
import com.arata.yukarilauncher.utils.stringutils.StringUtils
import com.arata.yukarilauncher.value.DependentLibrary
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.value.MinecraftAccount
import com.arata.yukarilauncher.value.MinecraftLibraryArtifact
import com.arata.yukarilauncher.value.MinecraftProfile

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.IOUtils
import org.lwjgl.glfw.CallbackBridge

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.Callable

@Suppress("IOStreamConstructor")
object Tools {
    const val NOTIFICATION_CHANNEL_DEFAULT = "channel_id"
    const val BYTE_TO_MB = 1024 * 1024f
    val GLOBAL_GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    const val LAUNCHERPROFILES_RTPREFIX = "pojav://"
    private const val isClientFirst = false
    var DEVICE_ARCHITECTURE = 0
    var DIRNAME_HOME_JRE = "lib"

    var DIR_DATA: String = ""
    lateinit var DIR_CACHE: File
    var NATIVE_LIB_DIR: String = ""
    var DIR_GAME_HOME: String = ""
    var DIR_GAME_NEW: String = ""
    var DIR_HOME_VERSION: String = ""
    var DIR_HOME_LIBRARY: String = ""
    var DIR_HOME_CRASH: String = ""
    var ASSETS_PATH: String = ""
    var OBSOLETE_RESOURCES_PATH: String = ""
    var DIR_ACCOUNT_NEW: String = ""
    var CTRLMAP_PATH: String = ""
    var CTRLDEF_FILE: String = ""
    var GAME_PROFILES_FILE: String = ""

    lateinit var currentDisplayMetrics: DisplayMetrics

    private val SDK_INT = Build.VERSION.SDK_INT
    private var LOCAL_RENDERER: String? = null

    // ============================== Storage / Path ==============================

    private fun getPojavStorageRoot(ctx: Context): File? {
        return if (SDK_INT >= 29) {
            ctx.getExternalFilesDir(null)
        } else {
            File(Environment.getExternalStorageDirectory(), "games/YukariLauncher")
        }
    }

    @JvmStatic fun checkStorageRoot(): Boolean {
        return try {
            Environment.getExternalStorageState(File(DIR_GAME_HOME)) == Environment.MEDIA_MOUNTED
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic fun checkStorageRoot(context: Context): Boolean {
        val externalFilesDir = if (DIR_GAME_HOME.isEmpty()) getPojavStorageRoot(context) else File(DIR_GAME_HOME)
        return externalFilesDir != null && Environment.getExternalStorageState(externalFilesDir) == Environment.MEDIA_MOUNTED
    }

    fun checkStorageInteractive(context: Activity): Boolean {
        if (!checkStorageRoot(context)) {
            context.startActivity(Intent(context, MissingStorageActivity::class.java))
            context.finish()
            return false
        }
        return true
    }

    // ============================== Notification ==============================

    fun buildNotificationChannel(context: Context) {
        if (SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_DEFAULT,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    // ============================== Display / Window ==============================

    @JvmStatic fun getDisplayMetrics(activity: BaseActivity): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) {
            return activity.resources.displayMetrics.also { currentDisplayMetrics = it }
        } else {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.getRealMetrics(displayMetrics)
            } else {
                activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            if (!activity.shouldIgnoreNotch()) {
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    displayMetrics.heightPixels -= AllStaticSettings.notchSize
                else
                    displayMetrics.widthPixels -= AllStaticSettings.notchSize
            }
        }
        currentDisplayMetrics = displayMetrics
        return displayMetrics
    }

    @JvmStatic fun setFullscreen(activity: Activity) {
        val decorView = activity.window.decorView
        val visibilityChangeListener = View.OnSystemUiVisibilityChangeListener { visibility ->
            val multiWindowMode = SDK_INT >= 24 && activity.isInMultiWindowMode
            if (!multiWindowMode) {
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        decorView.setOnSystemUiVisibilityChangeListener(visibilityChangeListener)
        visibilityChangeListener.onSystemUiVisibilityChange(decorView.systemUiVisibility)
    }

    @JvmStatic fun updateWindowSize(activity: BaseActivity) {
        currentDisplayMetrics = getDisplayMetrics(activity)
        CallbackBridge.physicalWidth = currentDisplayMetrics.widthPixels
        CallbackBridge.physicalHeight = currentDisplayMetrics.heightPixels
    }

    @JvmStatic fun dpToPx(dp: Float): Float = dp * currentDisplayMetrics.density

    fun pxToDp(px: Float): Float = px / currentDisplayMetrics.density

    fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
        var result = (displaySideRes * scaling).toInt()
        if (result % 2 != 0) result--
        return result
    }

    // ============================== Notch ==============================

    @JvmStatic fun ignoreNotch(shouldIgnore: Boolean, activity: BaseActivity) {
        if (SDK_INT >= Build.VERSION_CODES.P) {
            activity.window.attributes.layoutInDisplayCutoutMode = if (shouldIgnore)
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            else
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            updateWindowSize(activity)
        }
    }

    // ============================== Splash ==============================

    fun disableSplash(dir: File) {
        val configDir = File(dir, "config")
        if (FileUtils.ensureDirectorySilently(configDir)) {
            val forgeSplashFile = File(dir, "config/splash.properties")
            var forgeSplashContent = "enabled=true"
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = read(forgeSplashFile.absolutePath)
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    write(forgeSplashFile.absolutePath, forgeSplashContent.replace("enabled=true", "enabled=false"))
                }
            } catch (e: IOException) {
                Logging.w(InfoDistributor.LAUNCHER_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e)
            }
        } else {
            Logging.w(InfoDistributor.LAUNCHER_NAME, "Failed to create the configuration directory")
        }
    }

    // ============================== String utils ==============================

    fun fromStringArray(strArr: Array<String>): String {
        val builder = StringBuilder()
        for (i in strArr.indices) {
            if (i > 0) builder.append(" ")
            builder.append(strArr[i])
        }
        return builder.toString()
    }

    fun isValidString(string: String?): Boolean = string != null && string.isNotEmpty()

    fun extractUntilCharacter(input: String, whatFor: String, terminator: Char): String? {
        var whatForStart = input.indexOf(whatFor)
        if (whatForStart == -1) return null
        whatForStart += whatFor.length
        val terminatorIndex = input.indexOf(terminator, whatForStart)
        if (terminatorIndex == -1) return null
        return input.substring(whatForStart, terminatorIndex)
    }

    // ============================== Library / Classpath ==============================

    fun artifactToPath(library: DependentLibrary): String? {
        val downloads = library.downloads
        if (downloads != null && downloads.artifact.path != null) {
            return downloads.artifact.path
        }
        val libInfos = library.name?.split(":") ?: return null
        if (libInfos.size < 3) {
            Logging.e("Tools_artifactToPath", "Invalid library name format: " + library.name)
            return null
        }
        val groupId = libInfos[0].replace('.', '/')
        val artifactId = libInfos[1]
        val version = libInfos[2]
        val classifier = if (libInfos.size > 3) "-" + libInfos[3] else ""
        return "$groupId/$artifactId/$version/$artifactId-$version$classifier.jar"
    }

    fun getClientClasspath(version: Version): String {
        return File(version.getVersionPath(), version.getVersionName() + ".jar").absolutePath
    }

    fun getClientClasspath(versionName: String): String {
        return "$DIR_HOME_VERSION/$versionName/$versionName.jar"
    }

    fun getLWJGL3ClassPath(): String = getLWJGL3ClassPath("3.3.6")

    fun getLWJGL3ClassPath(version: String): String {
        val libStr = StringBuilder()
        val dirName = "lwjgl/$version"
        val lwjgl3Folder = File(PathManager.DIR_DATA, dirName)
        val lwjgl3Files = lwjgl3Folder.listFiles()
        if (lwjgl3Files != null) {
            for (file in lwjgl3Files) {
                if (file.name.endsWith(".jar")) {
                    libStr.append(file.absolutePath).append(":")
                }
            }
        }
        if (libStr.isNotEmpty()) libStr.setLength(libStr.length - 1)
        return libStr.toString()
    }

    fun generateLaunchClassPath(info: JMinecraftVersionList.Version, minecraftVersion: Version): String {
        val finalClasspath = StringBuilder()
        val classpath = generateLibClasspath(info)
        val clientClasspath = getClientClasspath(minecraftVersion)

        if (isClientFirst) {
            finalClasspath.append(clientClasspath)
        }
        for (jarFile in classpath) {
            if (!FileUtils.exists(jarFile)) {
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Ignored non-exists file: $jarFile")
                continue
            }
            finalClasspath.append((if (isClientFirst) ":" else "")).append(jarFile).append(if (!isClientFirst) ":" else "")
        }
        if (!isClientFirst) {
            finalClasspath.append(clientClasspath)
        }
        return finalClasspath.toString()
    }

    fun generateLaunchClassPath(info: JMinecraftVersionList.Version, versionName: String): String {
        val finalClasspath = StringBuilder()
        val classpath = generateLibClasspath(info)
        val clientClasspath = getClientClasspath(versionName)

        if (isClientFirst) {
            finalClasspath.append(clientClasspath)
        }
        for (jarFile in classpath) {
            if (!FileUtils.exists(jarFile)) {
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Ignored non-exists file: $jarFile")
                continue
            }
            finalClasspath.append((if (isClientFirst) ":" else "")).append(jarFile).append(if (!isClientFirst) ":" else "")
        }
        if (!isClientFirst) {
            finalClasspath.append(clientClasspath)
        }
        return finalClasspath.toString()
    }

    fun generateLibClasspath(info: JMinecraftVersionList.Version): Array<String> {
        val libDir = ArrayList<String>()
        if (info.libraries == null) return libDir.toTypedArray()
        for (libItem in info.libraries) {
            if (!checkRules(libItem.rules)) continue
            val libName = libItem.name
            if (libName == null) continue
            if (libName.contains("org.lwjgl") ||
                libName.contains("jinput-platform") ||
                libName.contains("twitch-platform")
            ) {
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Ignored unusable dependency: $libName")
                continue
            }
            val libArtifactPath = artifactToPath(libItem)
            if (libArtifactPath == null) continue
            libDir.add(ProfilePathHome.getLibrariesHome() + "/" + libArtifactPath)
        }
        return libDir.toTypedArray()
    }

    @SuppressLint("PrivateApi")
    private fun systemPropertiesGet(systemProperty: String): String {
        val cSystemProperties = Class.forName("android.os.SystemProperties")
        val get = cSystemProperties.getMethod("get", String::class.java)
        return get.invoke(null, systemProperty) as String
    }

    private fun isAdreno740(): Boolean {
        return try {
            BufferedReader(FileReader("/sys/class/kgsl/kgsl-3d0/gpu_model")).use { br ->
                val gpuRenderer = br.readLine()
                gpuRenderer != null &&
                    gpuRenderer.lowercase().contains("adreno") &&
                    gpuRenderer.contains("740")
            }
        } catch (e: IOException) {
            false
        }
    }

    fun shouldUseUBWC(): Boolean {
        return try {
            val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
            val isOneUI = !systemPropertiesGet("ro.build.version.oneui").isBlank()
            isOneUI && isSamsung && isAdreno740()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRules(rules: Array<JMinecraftVersionList.Arguments.ArgValue.ArgRules>?): Boolean {
        if (rules == null) return true
        for (rule in rules) {
            val os = rule.os
            if (rule.action == "allow" && os != null && os.name == "osx") {
                return false
            }
        }
        return true
    }

    fun preProcessLibraries(libraries: Array<DependentLibrary>?) {
        if (libraries == null) return
        for (libItem in libraries) {
            val nameParts = libItem.name?.split(":") ?: continue
            if (nameParts.size < 3) continue
            val versionParts = nameParts[2].split("\\.".toRegex())
            val libName = libItem.name ?: continue
            when {
                libName.startsWith("net.java.dev.jna:jna:") -> {
                    if (versionParts.size >= 2) {
                        val major = versionParts[0].toIntOrNull()
                        val minor = versionParts[1].toIntOrNull()
                        if (major != null && minor != null && major >= 5 && minor >= 13) continue
                    }
                    Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 5.13.0")
                    createLibraryInfo(libItem)
                    val downloads = libItem.downloads ?: continue
                    val artifact = downloads.artifact
                    libItem.name = "net.java.dev.jna:jna:5.13.0"
                    artifact.path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                    artifact.sha1 = "1200e7ebeedbe0d10062093f32925a912020e747"
                    artifact.url = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                }
                libName.startsWith("com.github.oshi:oshi-core:") -> {
                    if (versionParts.size >= 2) {
                        val major = versionParts[0].toIntOrNull()
                        val minor = versionParts[1].toIntOrNull()
                        if (major != null && minor != null && (major != 6 || minor != 2)) continue
                    }
                    Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 6.3.0")
                    createLibraryInfo(libItem)
                    val downloads = libItem.downloads ?: continue
                    val artifact = downloads.artifact
                    libItem.name = "com.github.oshi:oshi-core:6.3.0"
                    artifact.path = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                    artifact.sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac"
                    artifact.url = "https://repo1.maven.org/maven2/com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                }
                libName.startsWith("org.ow2.asm:asm-all:") -> {
                    val firstVer = versionParts[0].toIntOrNull()
                    if (firstVer != null && firstVer >= 5) continue
                    Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 5.0.4")
                    createLibraryInfo(libItem)
                    val downloads = libItem.downloads ?: continue
                    val artifact = downloads.artifact
                    libItem.name = "org.ow2.asm:asm-all:5.0.4"
                    libItem.url = null
                    artifact.path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                    artifact.sha1 = "e6244859997b3d4237a552669279780876228909"
                    artifact.url = "https://repo1.maven.org/maven2/org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                }
            }
        }
    }

    private fun createLibraryInfo(library: DependentLibrary) {
        val downloads = library.downloads
        if (downloads == null || downloads.artifact == null)
            library.downloads = DependentLibrary.LibraryDownloads(MinecraftLibraryArtifact())
    }

    // ============================== Version info (Version object) ==============================

    fun getVersionInfo(version: Version): JMinecraftVersionList.Version = getVersionInfo(version, false)

    @Suppress("UNCHECKED_CAST", "RAW_USE_OF_PARAMETER_TYPE")
    fun getVersionInfo(version: Version, skipInheriting: Boolean): JMinecraftVersionList.Version {
        try {
            val customVer = GLOBAL_GSON.fromJson(
                read(File(version.getVersionPath(), version.getVersionName() + ".json")),
                JMinecraftVersionList.Version::class.java
            )
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                preProcessLibraries(customVer.libraries)
            } else {
                val inheritsVer: JMinecraftVersionList.Version = try {
                    GLOBAL_GSON.fromJson(
                        read(version.getVersionsFolder() + "/" + customVer.inheritsFrom + "/" + customVer.inheritsFrom + ".json"),
                        JMinecraftVersionList.Version::class.java
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for " + version.getVersionName() + " (req version=" + customVer.inheritsFrom + ")")
                }
                insertSafety(inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )
                val inheritsLibraries = inheritsVer.libraries
                val customLibraries = customVer.libraries
                if (inheritsLibraries != null && customLibraries != null) {
                    val inheritLibraryList = ArrayList(listOf(*inheritsLibraries))
                    outer_loop@ for (library in customLibraries) {
                        val libName = library.name ?: continue
                        val libPrefix = libName.substring(0, libName.lastIndexOf(":"))
                        val iterator = inheritLibraryList.iterator()
                        while (iterator.hasNext()) {
                            val inheritLibrary = iterator.next()
                            val inheritLibName = inheritLibrary.name ?: continue
                            val inheritPrefix = inheritLibName.substring(0, inheritLibName.lastIndexOf(":"))
                            if (libPrefix == inheritPrefix) {
                                Logging.d(InfoDistributor.LAUNCHER_NAME, "Library $libName: Replaced version " +
                                        libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                        inheritPrefix.substring(inheritPrefix.lastIndexOf(":") + 1))
                                iterator.remove()
                                continue@outer_loop
                            }
                        }
                    }
                    inheritLibraryList.addAll(listOf(*customLibraries))
                    inheritsVer.libraries = inheritLibraryList.toTypedArray()
                    preProcessLibraries(inheritsVer.libraries)
                }

                if (inheritsVer.arguments != null && customVer.arguments != null) {
                    val inheritsArgs = inheritsVer.arguments!!
                    val customArgs = customVer.arguments!!
                    val inheritsGame = inheritsArgs.game
                    val customGame = customArgs.game
                    if (inheritsGame != null && customGame != null) {
                        val totalArgList = ArrayList(listOf(*inheritsGame))
                        var nskip = 0
                        for (i in customGame.indices) {
                            if (nskip > 0) {
                                nskip--
                                continue
                            }
                            var perCustomArg = customGame[i]
                            if (perCustomArg is String) {
                                var perCustomArgStr = perCustomArg
                                if (perCustomArgStr.startsWith("--") && totalArgList.contains(perCustomArgStr)) {
                                    perCustomArg = customGame[i + 1]
                                    if (perCustomArg is String) {
                                        perCustomArgStr = perCustomArg
                                        if (!perCustomArgStr.startsWith("--")) {
                                            nskip++
                                        }
                                    }
                                } else {
                                    totalArgList.add(perCustomArgStr)
                                }
                            } else if (!totalArgList.contains(perCustomArg)) {
                                totalArgList.add(perCustomArg)
                            }
                        }
                        inheritsArgs.game = totalArgList.toTypedArray()
                    }
                }
                return inheritsVer
            }
            val javaVersion = customVer.javaVersion
            if (javaVersion != null && javaVersion.majorVersion == 0) {
                javaVersion.majorVersion = javaVersion.version
            }
            return customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // ============================== Version info (String overload) ==============================

    fun getVersionInfo(versionName: String): JMinecraftVersionList.Version = getVersionInfo(versionName, false)

    @Suppress("UNCHECKED_CAST", "RAW_USE_OF_PARAMETER_TYPE")
    fun getVersionInfo(versionName: String, skipInheriting: Boolean): JMinecraftVersionList.Version {
        try {
            val customVer = GLOBAL_GSON.fromJson(
                read("$DIR_HOME_VERSION/$versionName/$versionName.json"),
                JMinecraftVersionList.Version::class.java
            )
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                preProcessLibraries(customVer.libraries)
            } else {
                val inheritsVer: JMinecraftVersionList.Version = try {
                    GLOBAL_GSON.fromJson(
                        read("$DIR_HOME_VERSION/${customVer.inheritsFrom}/${customVer.inheritsFrom}.json"),
                        JMinecraftVersionList.Version::class.java
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for $versionName (req version=${customVer.inheritsFrom})")
                }
                insertSafety(inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )
                val inheritsLibraries = inheritsVer.libraries
                val customLibraries = customVer.libraries
                if (inheritsLibraries != null && customLibraries != null) {
                    val inheritLibraryList = ArrayList(listOf(*inheritsLibraries))
                    outer_loop@ for (library in customLibraries) {
                        val libName = library.name ?: continue
                        val libPrefix = libName.substring(0, libName.lastIndexOf(":"))
                        val iterator = inheritLibraryList.iterator()
                        while (iterator.hasNext()) {
                            val inheritLibrary = iterator.next()
                            val inheritLibName = inheritLibrary.name ?: continue
                            val inheritPrefix = inheritLibName.substring(0, inheritLibName.lastIndexOf(":"))
                            if (libPrefix == inheritPrefix) {
                                Logging.d(InfoDistributor.LAUNCHER_NAME, "Library $libName: Replaced version " +
                                        libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                        inheritPrefix.substring(inheritPrefix.lastIndexOf(":") + 1))
                                iterator.remove()
                                continue@outer_loop
                            }
                        }
                    }
                    inheritLibraryList.addAll(listOf(*customLibraries))
                    inheritsVer.libraries = inheritLibraryList.toTypedArray()
                    preProcessLibraries(inheritsVer.libraries)
                }

                if (inheritsVer.arguments != null && customVer.arguments != null) {
                    val inheritsArgs = inheritsVer.arguments!!
                    val customArgs = customVer.arguments!!
                    val inheritsGame = inheritsArgs.game
                    val customGame = customArgs.game
                    if (inheritsGame != null && customGame != null) {
                        val totalArgList = ArrayList(listOf(*inheritsGame))
                        var nskip = 0
                        for (i in customGame.indices) {
                            if (nskip > 0) {
                                nskip--
                                continue
                            }
                            var perCustomArg = customGame[i]
                            if (perCustomArg is String) {
                                var perCustomArgStr = perCustomArg
                                if (perCustomArgStr.startsWith("--") && totalArgList.contains(perCustomArgStr)) {
                                    perCustomArg = customGame[i + 1]
                                    if (perCustomArg is String) {
                                        perCustomArgStr = perCustomArg
                                        if (!perCustomArgStr.startsWith("--")) {
                                            nskip++
                                        }
                                    }
                                } else {
                                    totalArgList.add(perCustomArgStr)
                                }
                            } else if (!totalArgList.contains(perCustomArg)) {
                                totalArgList.add(perCustomArg)
                            }
                        }
                        inheritsArgs.game = totalArgList.toTypedArray()
                    }
                }
                return inheritsVer
            }
            val javaVersion = customVer.javaVersion
            if (javaVersion != null && javaVersion.majorVersion == 0) {
                javaVersion.majorVersion = javaVersion.version
            }
            return customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun insertSafety(targetVer: JMinecraftVersionList.Version, fromVer: JMinecraftVersionList.Version, vararg keyArr: String) {
        for (key in keyArr) {
            var value: Any? = null
            try {
                val fieldA = findAccessibleField(fromVer.javaClass, key) ?: continue
                value = fieldA.get(fromVer)
                if ((value is String && value.isNotEmpty()) || value != null) {
                    val fieldB = findAccessibleField(targetVer.javaClass, key) ?: continue
                    fieldB.set(targetVer, value)
                }
            } catch (th: Throwable) {
                Logging.w(InfoDistributor.LAUNCHER_NAME, "Unable to insert $key=$value", th)
            }
        }
    }

    private fun findAccessibleField(clazz: Class<*>, key: String): java.lang.reflect.Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                val field = current.getDeclaredField(key)
                field.isAccessible = true
                return field
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    // ============================== I/O ==============================

    @Throws(IOException::class)
    fun read(`is`: InputStream): String {
        val readResult = IOUtils.toString(`is`, StandardCharsets.UTF_8)
        `is`.close()
        return readResult
    }

    @Throws(IOException::class)
    fun read(path: String): String = read(FileInputStream(path))

    @Throws(IOException::class)
    fun read(path: File): String = read(FileInputStream(path))

    @Throws(IOException::class)
    @JvmStatic fun write(path: String, content: String) {
        val file = File(path)
        FileUtils.ensureParentDirectory(file)
        FileOutputStream(file).use { outStream ->
            IOUtils.write(content, outStream)
        }
    }

    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, fileName: String, output: String, overwrite: Boolean) {
        copyAssetFile(ctx, fileName, output, File(fileName).name, overwrite)
    }

    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, fileName: String, output: String, outputName: String, overwrite: Boolean) {
        val parentFolder = File(output)
        FileUtils.ensureDirectory(parentFolder)
        val destinationFile = File(output, outputName)
        if (!destinationFile.exists() || overwrite) {
            ctx.assets.open(fileName).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }

    // ============================== Error handling ==============================

    @JvmStatic fun printToString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    @JvmStatic fun showError(ctx: Context, e: Throwable) = showError(ctx, e, false)
    @JvmStatic fun showError(ctx: Context, e: Throwable, exitIfOk: Boolean) = showError(ctx, R.string.generic_error, null, e, exitIfOk, false)
    @JvmStatic fun showError(ctx: Context, rolledMessage: Int, e: Throwable) = showError(ctx, R.string.generic_error, ctx.getString(rolledMessage), e, false, false)
    @JvmStatic fun showError(ctx: Context, rolledMessage: String, e: Throwable) = showError(ctx, R.string.generic_error, rolledMessage, e, false, false)
    @JvmStatic fun showError(ctx: Context, rolledMessage: String, e: Throwable, exitIfOk: Boolean) = showError(ctx, R.string.generic_error, rolledMessage, e, exitIfOk, false)
    @JvmStatic fun showError(ctx: Context, titleId: Int, e: Throwable, exitIfOk: Boolean) = showError(ctx, titleId, null, e, exitIfOk, false)

    private fun showError(ctx: Context, titleId: Int, rolledMessage: String?, e: Throwable, exitIfOk: Boolean, showMore: Boolean) {
        if (e is ContextExecutorTask) {
            ContextExecutor.executeTask(e)
            return
        }
        Logging.e("ShowError", printToString(e))

        val runnable = Runnable {
            val errMsg = if (showMore) printToString(e) else rolledMessage ?: e.message
            val builder = AlertDialog.Builder(ctx, R.style.CustomAlertDialogTheme)
                .setTitle(titleId)
                .setMessage(errMsg)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            fullyExit()
                        } else if (ctx is Activity) {
                            (ctx as Activity).finish()
                        }
                    }
                }
                .setNegativeButton(
                    if (showMore) R.string.error_show_less else R.string.error_show_more
                ) { _, _ -> showError(ctx, titleId, rolledMessage, e, exitIfOk, !showMore) }
                .setNeutralButton(android.R.string.copy) { _, _ ->
                    StringUtils.copyText("error", printToString(e), ctx)
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            fullyExit()
                        } else {
                            (ctx as Activity).finish()
                        }
                    }
                }
                .setCancelable(!exitIfOk)
            try {
                builder.show()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
        if (ctx is Activity) {
            ctx.runOnUiThread(runnable)
        } else {
            runnable.run()
        }
    }

    fun showErrorRemote(e: Throwable) = showErrorRemote(null as String?, e)

    fun showErrorRemote(context: Context, rolledMessage: Int, e: Throwable) = showErrorRemote(context.getString(rolledMessage), e)

    fun showErrorRemote(rolledMessage: String?, e: Throwable) {
        ContextExecutor.executeTask(ShowErrorActivity.RemoteErrorTask(e, rolledMessage ?: ""))
    }

    fun dialogOnUiThread(activity: Activity, title: CharSequence, message: CharSequence) {
        activity.runOnUiThread { dialog(activity, title, message) }
    }

    fun dialog(context: Context, title: CharSequence, message: CharSequence) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ============================== SHA1 ==============================

    fun interface DownloaderFeedback {
        fun updateProgress(curr: Long, max: Long)
    }

    fun compareSHA1(f: File, sourceSHA: String?): Boolean {
        try {
            val sha1_dst: String
            FileInputStream(f).use { `is` ->
                sha1_dst = String(Hex.encodeHex(org.apache.commons.codec.digest.DigestUtils.sha1(`is`)))
            }
            return if (sourceSHA != null) sha1_dst.equals(sourceSHA, ignoreCase = true) else true
        } catch (e: IOException) {
            Logging.i("SHA1", "Fake-matching a hash due to a read error", e)
            return true
        }
    }

    // ============================== Memory ==============================

    fun getTotalDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / 1048576L).toInt()
    }

    fun getFreeDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / 1048576L).toInt()
    }

    @Throws(Exception::class)
    private fun internalGetMaxContinuousAddressSpaceSize(): Int {
        val memoryHoleFinder = MemoryHoleFinder()
        SelfMapsParser(memoryHoleFinder).run()
        val largestHole = memoryHoleFinder.getLargestHole()
        return if (largestHole == -1L) -1 else (largestHole / 1048576L).toInt()
    }

    fun getMaxContinuousAddressSpaceSize(): Int {
        return try {
            internalGetMaxContinuousAddressSpaceSize()
        } catch (e: Exception) {
            Logging.w("Tools", "Failed to find the largest uninterrupted address space")
            -1
        }
    }

    // ============================== URI / Content ==============================

    fun getFileName(ctx: Context, uri: Uri): String {
        val c: Cursor? = ctx.contentResolver.query(uri, null, null, null, null)
        if (c == null) return uri.lastPathSegment ?: ""
        c.use {
            it.moveToFirst()
            val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex == -1) return uri.lastPathSegment ?: ""
            return it.getString(columnIndex) ?: uri.lastPathSegment ?: ""
        }
    }

    // ============================== Fragments ==============================

    fun swapFragment(fragmentActivity: FragmentActivity, fragmentClass: Class<out Fragment>, fragmentTag: String?, bundle: Bundle?) {
        fragmentActivity.supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragmentClass.name)
            .replace(com.arata.yukarilauncher.R.id.container_fragment, fragmentClass, bundle, fragmentTag)
            .commit()
    }

    fun backToMainMenu(fragmentActivity: FragmentActivity) {
        val manager = fragmentActivity.supportFragmentManager
        if (manager.backStackEntryCount > 0) {
            manager.popBackStack(manager.getBackStackEntryAt(0).name, 0)
        }
    }

    fun removeCurrentFragment(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack()
    }

    // ============================== Mod / Runtime installation ==============================

    fun installMod(activity: Activity, customJavaArgs: Boolean) {
        if (MultiRTUtils.getExactJreName(8) == null) {
            Toast.makeText(activity, R.string.multirt_nojava8rt, Toast.LENGTH_LONG).show()
            return
        }
        if (!customJavaArgs) {
            if (activity !is LauncherActivity)
                throw IllegalStateException("Cannot start Mod Installer without LauncherActivity")
            (activity as LauncherActivity).modInstallerLauncher.launch("")
            return
        }
        EditTextDialog.Builder(activity)
            .setTitle(R.string.dialog_select_jar)
            .setHintText("-jar/-cp /path/to/file.jar ...")
            .setAsRequired()
            .setConfirmListener { editBox, _ ->
                val intent = Intent(activity, JavaGUILauncherActivity::class.java)
                intent.putExtra("javaArgs", editBox.text.toString())
                SelectRuntimeUtils.selectRuntime(activity, null) { jreName ->
                    intent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
                    activity.startActivity(intent)
                }
                true
            }.showDialog()
    }

    fun launchModInstaller(activity: Activity, @NonNull uri: Uri) {
        val intent = Intent(activity, JavaGUILauncherActivity::class.java)
        intent.putExtra("modUri", uri)
        SelectRuntimeUtils.selectRuntime(activity, null) { jreName ->
            LauncherProfiles.generateLauncherProfiles()
            intent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
            activity.startActivity(intent)
        }
    }

    fun installRuntimeFromUri(context: Context, uri: Uri) {
        Task.runTask(Callable {
            val name = getFileName(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) throw IOException("Cannot open input stream for $uri")
            MultiRTUtils.installRuntimeNamed(
                PathManager.DIR_NATIVE_LIB,
                inputStream,
                name
            )
            MultiRTUtils.postPrepare(name)
            null
        }).onThrowable { e -> showError(context, e) }
            .execute()
    }

    // ============================== Vulkan ==============================

    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        } else false
    }

    // ============================== Weak reference ==============================

    fun <T> getWeakReference(weakReference: WeakReference<T>?): T? {
        return weakReference?.get()
    }

    // ============================== Initialization (Yukari) ==============================

    fun initEarlyConstants(ctx: Context) {
        DIR_CACHE = ctx.cacheDir
        DIR_DATA = ctx.filesDir.parent!!
        DIR_ACCOUNT_NEW = "$DIR_DATA/accounts"
        NATIVE_LIB_DIR = ctx.applicationInfo.nativeLibraryDir
    }

    fun initStorageConstants(ctx: Context) {
        initEarlyConstants(ctx)
        DIR_GAME_HOME = getPojavStorageRoot(ctx)!!.absolutePath
        DIR_GAME_NEW = "$DIR_GAME_HOME/.minecraft"
        DIR_HOME_VERSION = "$DIR_GAME_NEW/versions"
        DIR_HOME_LIBRARY = "$DIR_GAME_NEW/libraries"
        DIR_HOME_CRASH = "$DIR_GAME_NEW/crash-reports"
        ASSETS_PATH = "$DIR_GAME_NEW/assets"
        OBSOLETE_RESOURCES_PATH = "$DIR_GAME_NEW/resources"
        CTRLMAP_PATH = "$DIR_GAME_HOME/controlmap"
        CTRLDEF_FILE = "$DIR_GAME_HOME/controlmap/default.json"
        GAME_PROFILES_FILE = "${DIR_GAME_NEW}/launcher_profiles.json"
        switchDemo(isDemoProfile(ctx))
    }

    fun switchDemo(isDemo: Boolean) {
        DIR_GAME_NEW = if (isDemo) "$DIR_DATA/demo/.minecraft" else "$DIR_GAME_HOME/.minecraft"
        DIR_HOME_VERSION = "$DIR_GAME_NEW/versions"
        DIR_HOME_LIBRARY = "$DIR_GAME_NEW/libraries"
        ASSETS_PATH = "$DIR_GAME_NEW/assets"
        OBSOLETE_RESOURCES_PATH = "$DIR_GAME_NEW/resources"
    }

    fun isDemoProfile(ctx: Context): Boolean {
        val currentProfile = PojavProfile.getCurrentProfileContent(ctx, null)
        return currentProfile != null && currentProfile.username.startsWith("Demo.")
    }

    fun isLocalProfile(ctx: Context): Boolean {
        val currentProfile = PojavProfile.getCurrentProfileContent(ctx, null)
        return currentProfile == null || currentProfile.username.startsWith("Local.")
    }

    // ============================== Misc (Yukari) ==============================

    fun openURL(act: Activity, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        act.startActivity(browserIntent)
    }

    @Throws(IOException::class)
    fun downloadFile(urlInput: String, nameOutput: String) {
        DownloadUtils.downloadFile(urlInput, File(nameOutput))
    }

    fun isAndroid8OrHigher(): Boolean = SDK_INT >= 26

    fun fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun printLauncherInfo(gameVersion: String, javaArguments: String) {
        Logger.appendToLog("Info: Launcher version: " + BuildConfig.VERSION_NAME)
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(DEVICE_ARCHITECTURE))
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL)
        Logger.appendToLog("Info: API version: " + SDK_INT)
        Logger.appendToLog("Info: Selected Minecraft version: " + gameVersion)
        Logger.appendToLog("Info: Custom Java arguments: \"" + javaArguments + "\"")
        val info = GLInfoUtils.getGlInfo()
        Logger.appendToLog("Info: Graphics device: " + info.vendor + " " + info.renderer + " (OpenGL ES " + info.glesMajorVersion + ")")
    }

    fun shareLog(context: Context) {
        openPath(context, File(DIR_GAME_HOME, "latestlog.txt"), true)
    }

    fun getMimeType(file: File): String {
        if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
        var mimeType: String? = null
        try {
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream)
                }
            }
        } catch (e: IOException) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e)
        }
        if (mimeType != null) return mimeType
        mimeType = URLConnection.guessContentTypeFromName(file.name)
        if (mimeType != null) return mimeType
        return "*/*"
    }

    fun openPath(context: Context, file: File, share: Boolean) {
        val contentUri = DocumentsContract.buildDocumentUri(
            context.getString(R.string.storageProviderAuthorities),
            file.absolutePath
        )
        val mimeType = getMimeType(file)
        val intent = Intent()
        if (share) {
            intent.action = Intent.ACTION_SEND
            intent.type = getMimeType(file)
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
        } else {
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(contentUri, mimeType)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, file.name))
    }

    fun mesureTextviewHeight(t: TextView): Int {
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(t.width, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        t.measure(widthMeasureSpec, heightMeasureSpec)
        return t.measuredHeight
    }

    fun deviceHasHangingLinker(): Boolean {
        if (SDK_INT >= Build.VERSION_CODES.O) return false
        return Build.MANUFACTURER.lowercase(Locale.ROOT).contains("huawei")
    }

    fun deviceSupportsGyro(context: Context): Boolean {
        return (context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager)
            .getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE) != null
    }

    fun dialogForceClose(ctx: Context) {
        AlertDialog.Builder(ctx)
            .setMessage(R.string.force_exit_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    fullyExit()
                } catch (th: Throwable) {
                    Logging.w(InfoDistributor.LAUNCHER_NAME, "Could not enable System.exit() method!", th)
                }
            }
            .show()
    }

    fun getWaitingDialog(ctx: Context, message: Int): android.app.ProgressDialog {
        val barrier = android.app.ProgressDialog(ctx)
        barrier.setMessage(ctx.getString(message))
        barrier.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER)
        barrier.setCancelable(false)
        barrier.show()
        return barrier
    }
}
