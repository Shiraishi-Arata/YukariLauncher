package com.arata.yukarilauncher.setting

import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.setting.unit.BooleanSettingUnit
import com.arata.yukarilauncher.setting.unit.IntSettingUnit
import com.arata.yukarilauncher.setting.unit.LongSettingUnit
import com.arata.yukarilauncher.setting.unit.StringSettingUnit
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.setting.LauncherPreferences

class AllSettings {
    companion object {
        // ビデオ設定
        @JvmStatic
        val renderer = StringSettingUnit("renderer", "opengles2")

        @JvmStatic
        val driver = StringSettingUnit("driver", "Turnip")

        @JvmStatic
        val gameGraphicsApi = StringSettingUnit("gameGraphicsApi", "auto")

        @JvmStatic
        val ignoreNotch = BooleanSettingUnit("ignoreNotch", true)

        @JvmStatic
        val ignoreNotchLauncher = BooleanSettingUnit("ignoreNotchLauncher", true)

        @JvmStatic
        val resolutionRatio = IntSettingUnit("resolutionRatio", 100)

        @JvmStatic
        val sustainedPerformance = BooleanSettingUnit("sustainedPerformance", false)

        @JvmStatic
        val alternateSurface = BooleanSettingUnit("alternate_surface", false)

        @JvmStatic
        val forceVsync = BooleanSettingUnit("force_vsync", false)

        @JvmStatic
        val vsyncInZink = BooleanSettingUnit("vsync_in_zink", false)

        @JvmStatic
        val zinkPreferSystemDriver = BooleanSettingUnit("zinkPreferSystemDriver", false)

        // MobileGlues 設定
        @JvmStatic
        val mgAngle = StringSettingUnit("mg_angle", "1")
        // 0=DisableIfPossible, 1=EnableIfPossible, 2=ForceDisable, 3=ForceEnable

        @JvmStatic
        val mgNoError = StringSettingUnit("mg_no_error", "0")
        // 0=Auto, 1=Disable, 2=Level1, 3=Level2

        @JvmStatic
        val mgExtTimerQuery = BooleanSettingUnit("mg_ext_timer_query", false)
        // true=推奨のtimer_query拡張を無効化, false=有効化（UIは反転）

        @JvmStatic
        val mgExtComputeShader = BooleanSettingUnit("mg_ext_compute_shader", false)
        // 不完全なARB_compute_shader拡張を有効化

        @JvmStatic
        val mgExtDirectStateAccess = BooleanSettingUnit("mg_ext_direct_state_access", false)
        // 実験的なdirect_state_access拡張を有効化

        @JvmStatic
        val mgGlslCacheSize = StringSettingUnit("mg_glsl_cache_size", "32")
        // GLSLキャッシュサイズ（MB）、-1で無効化

        @JvmStatic
        val mgMultidrawMode = StringSettingUnit("mg_multidraw_mode", "0")
        // 0=Auto, 1=PreferIndirect, 2=PreferBaseVertex, 3=PreferMultidrawIndirect, 4=ForceDrawElements, 5=PreferCompute

        @JvmStatic
        val mgAngleDepthClearFixMode = StringSettingUnit("mg_angle_depth_clear_fix", "0")
        // 0=Disable, 1=Mode1（ANGLE深度クリアの回避策）

        @JvmStatic
        val mgCustomGLVersion = StringSettingUnit("mg_custom_gl_version", "0")
        // 0=無効, 32=3.2, 33=3.3, 40=4.0, 41=4.1, 42=4.2, 43=4.3, 44=4.4, 45=4.5, 46=4.6

        @JvmStatic
        val mgFsr1 = StringSettingUnit("mg_fsr1", "0")
        // FSR1（FidelityFX Super Resolution）品質設定: 0=無効, 1=Performance, 2=Balanced, 3=Quality, 4=UltraQuality

        @JvmStatic
        val mgHideMG = BooleanSettingUnit("mg_hide_mg", false)
        // F3画面からMobileGlues情報を隠す

        @JvmStatic
        val mgFrameGeneration = BooleanSettingUnit("mg_frame_generation", false)
        // フレーム生成（FG）でFPSを向上

        // コントロール設定
        @JvmStatic
        val disableGestures = BooleanSettingUnit("disableGestures", false)

        @JvmStatic
        val disableDoubleTap = BooleanSettingUnit("disableDoubleTap", false)

        @JvmStatic
        val forceGuiInput = BooleanSettingUnit("forceGuiInput", false)

        @JvmStatic
        val timeLongPressTrigger = IntSettingUnit("timeLongPressTrigger", 300)

        @JvmStatic
        val buttonScale = IntSettingUnit("buttonscale", 100)

        @JvmStatic
        val buttonAllCaps = BooleanSettingUnit("buttonAllCaps", false)

        @JvmStatic
        val mouseScale = IntSettingUnit("mousescale", 100)

        @JvmStatic
        val mouseSpeed = IntSettingUnit("mousespeed", 100)

        @JvmStatic
        val virtualMouseStart = BooleanSettingUnit("mouse_start", true)

        @JvmStatic
        val customMouse = StringSettingUnit("custom_mouse", "")

        @JvmStatic
        val enableGyro = BooleanSettingUnit("enableGyro", false)

        @JvmStatic
        val gyroSensitivity = IntSettingUnit("gyroSensitivity", 100)

        @JvmStatic
        val gyroSampleRate = IntSettingUnit("gyroSampleRate", 16)

        @JvmStatic
        val gyroSmoothing = BooleanSettingUnit("gyroSmoothing", true)

        @JvmStatic
        val gyroInvertX = BooleanSettingUnit("gyroInvertX", false)

        @JvmStatic
        val gyroInvertY = BooleanSettingUnit("gyroInvertY", false)

        @JvmStatic
        val deadZoneScale = IntSettingUnit("gamepad_deadzone_scale", 100)

        // ゲーム設定
        @JvmStatic
        val versionIsolation = BooleanSettingUnit("versionIsolation", true)

        @JvmStatic
        val versionCustomInfo = StringSettingUnit("versionCustomInfo", "${InfoDistributor.LAUNCHER_NAME}[zl_version]")

        @JvmStatic
        val autoSetGameLanguage = BooleanSettingUnit("autoSetGameLanguage", true)

        @JvmStatic
        val gameLanguageOverridden = BooleanSettingUnit("gameLanguageOverridden", false)

        @JvmStatic
        val setGameLanguage = StringSettingUnit("setGameLanguage", "system")

        @JvmStatic
        val selectRuntimeMode = StringSettingUnit("selectRuntimeMode", "auto")

        @JvmStatic
        val javaArgs = StringSettingUnit("javaArgs", "")

        @JvmStatic
        val ramAllocation = lazy {
            // Contextの初期化が必要なため遅延ロード
            IntSettingUnit("allocation", LauncherPreferences.findBestRAMAllocation(ContextExecutor.getApplication()))
        }

        @JvmStatic
        val javaSandbox = BooleanSettingUnit("java_sandbox", true)

        @JvmStatic
        val lwjglVersion = StringSettingUnit("lwjglVersion", "3.3.6")

        @JvmStatic
        val gameMenuShowMemory = BooleanSettingUnit("gameMenuShowMemory", false)

        @JvmStatic
        val gameMenuShowFPS = BooleanSettingUnit("gameMenuShowFPS", false)

        @JvmStatic
        val gameMenuMemoryText = StringSettingUnit("gameMenuMemoryText", "M:")

        @JvmStatic
        val gameMenuLocation = StringSettingUnit("gameMenuLocation", "center")

        @JvmStatic
        val gameMenuInfoRefreshRate = IntSettingUnit("gameMenuInfoRefreshRate", 1000)

        @JvmStatic
        val gameMenuAlpha = IntSettingUnit("gameMenuAlpha", 100)

        // ランチャー設定
        @JvmStatic
        val checkLibraries = BooleanSettingUnit("checkLibraries", true)

        @JvmStatic
        val verifyManifest = BooleanSettingUnit("verifyManifest", true)

        @JvmStatic
        val resourceImageCache = BooleanSettingUnit("resourceImageCache", false)

        @JvmStatic
        val addFullResourceName = BooleanSettingUnit("addFullResourceName", true)

        @JvmStatic
        val downloadSource = StringSettingUnit("downloadSource", "default")

        @JvmStatic
        val maxDownloadThreads = IntSettingUnit("maxDownloadThreads", 64)


        @JvmStatic
        val customBackgroundBlur = IntSettingUnit("customBackgroundBlur", 25)

        @JvmStatic
        val animation = BooleanSettingUnit("animation", true)

        @JvmStatic
        val animationSpeed = IntSettingUnit("animationSpeed", 600)

        @JvmStatic
        val pageOpacity = IntSettingUnit("pageOpacity", 100)

        @JvmStatic
        val enableLogOutput = BooleanSettingUnit("enableLogOutput", false)

        @JvmStatic
        val quitLauncher = BooleanSettingUnit("quitLauncher", true)

        @JvmStatic
        val acceptPreReleaseUpdates = BooleanSettingUnit("acceptPreReleaseUpdates", false)

        // 実験的設定
        @JvmStatic
        val dumpShaders = BooleanSettingUnit("dump_shaders", false)

        @JvmStatic
        val bigCoreAffinity = BooleanSettingUnit("bigCoreAffinity", false)

        @JvmStatic
        val tcVibrateDuration = IntSettingUnit("tcVibrateDuration", 100)

        // その他の設定
        @JvmStatic
        val currentAccount = StringSettingUnit("currentAccount", "")

        @JvmStatic
        val launcherProfile = StringSettingUnit("launcherProfile", "default")

        @JvmStatic
        val defaultCtrl = StringSettingUnit("defaultCtrl", PathManager.FILE_CTRLDEF_FILE)

        @JvmStatic
        val defaultRuntime = StringSettingUnit("defaultRuntime", "")

        @JvmStatic
        val notificationPermissionRequest = BooleanSettingUnit("notification_permission_request", false)

        @JvmStatic
        val skipNotificationPermissionCheck = BooleanSettingUnit("skipNotificationPermissionCheck", false)

        @JvmStatic
        val localAccountReminders = BooleanSettingUnit("localAccountReminders", true)

        @JvmStatic
        val updateCheck = LongSettingUnit("updateCheck", 0L)

        @JvmStatic
        val ignoreUpdate = StringSettingUnit("ignoreUpdate", "")

        @JvmStatic
        val noticeCheck = LongSettingUnit("noticeCheck", 0L)

        @JvmStatic
        val noticeNumbering = IntSettingUnit("noticeNumbering", 0)

        @JvmStatic
        val noticeDefault = BooleanSettingUnit("noticeDefault", false)

        @JvmStatic
        val buttonSnapping = BooleanSettingUnit("buttonSnapping", true)

        @JvmStatic
        val buttonSnappingDistance = IntSettingUnit("buttonSnappingDistance", 8)

        @JvmStatic
        val hotbarType = StringSettingUnit("hotbarType", "auto")

        @JvmStatic
        val hotbarWidth = lazy {
            IntSettingUnit("hotbarWidth", Tools.currentDisplayMetrics.widthPixels / 3)
        }

        @JvmStatic
        val hotbarHeight = lazy {
            IntSettingUnit("hotbarHeight", Tools.currentDisplayMetrics.heightPixels / 4)
        }
    }
}