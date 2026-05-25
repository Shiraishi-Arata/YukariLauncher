package com.arata.yukarilauncher.feature.graphics

import android.content.Context
import android.content.pm.PackageManager
import com.arata.yukarilauncher.feature.MCOptions
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.setting.AllSettings
import com.google.gson.JsonParser
import com.arata.yukarilauncher.Tools
import java.io.File

object GameGraphicsApiHelper {
    private const val TAG = "GameGraphicsApi"
    private const val VULKAN_1_2_VERSION = 0x00402000
    const val SETTING_AUTO = "auto"
    const val SETTING_OPENGL = "opengl"
    const val SETTING_VULKAN = "vulkan"
    private const val VULKAN_1_3_VERSION = 0x00403000
    private const val VULKAN_1_1_VERSION = 0x00401000
    private const val VULKAN_1_0_VERSION = 0x00400000

    data class VulkanStatus(
        val version: String,
        val vulkan12Supported: Boolean,
        val dynamicRenderingSupported: Boolean,
        val pushDescriptorSupported: Boolean
    )

    @JvmStatic
/**
 * applyPreferredGraphicsBackendする
 */
    fun applyPreferredGraphicsBackend(context: Context, currentVersionName: String?) {
        when (AllSettings.gameGraphicsApi.getValue()) {
            SETTING_OPENGL -> MCOptions.set("preferredGraphicsBackend", SETTING_OPENGL)
            SETTING_VULKAN -> {
                if (isVulkan12Supported(context)) {
                    MCOptions.set("preferredGraphicsBackend", SETTING_VULKAN)
                } else {
                    Logging.w(TAG, "Vulkan forced by user but Vulkan 1.2 is unavailable, falling back to OpenGL")
                    MCOptions.set("preferredGraphicsBackend", SETTING_OPENGL)
                }
            }

            else -> applyAutoPreferredGraphicsBackend(context, currentVersionName)
        }
    }

    /**
     * Returns true if the device reports Vulkan 1.2 or higher support.
     * Uses the PackageManager hardware feature flag introduced in API 26.
     * The version value is encoded as (major << 22) | (minor << 12) | patch,
     * so Vulkan 1.2.0 = 0x00402000.
     */
    @JvmStatic
/**
 * isVulkan12Supportedする
 */
    fun isVulkan12Supported(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_1_2_VERSION)
        } catch (t: Throwable) {
            Logging.w(TAG, "Vulkan version check failed, assuming no Vulkan 1.2", t)
            false
        }
    }

/**
 * applyAutoPreferredGraphicsBackendする
 */
    /**
     * 自動設定で優先グラフィックスバックエンドを選択する
     * Minecraftバージョンが1.26.2以降でVulkan 1.2対応の場合はVulkanを選択する
     * @param context コンテキスト
     * @param currentVersionName 現在のMinecraftバージョン名
     */
    private fun applyAutoPreferredGraphicsBackend(context: Context, currentVersionName: String?) {
        try {
            var versionName = currentVersionName
            if (versionName.isNullOrEmpty()) {
                versionName = try {
                    VersionsManager.currentGameInfo.version
                } catch (_: Throwable) {
                    return
                }
            }
            if (versionName.isNullOrEmpty()) return

            val versionFolder = File(ProfilePathHome.getVersionsHome(), versionName)
            val versionInfoFile =
                File(VersionsManager.getYukariVersionPath(versionFolder), "VersionInfo.json")
            if (!versionInfoFile.exists()) return

            val jsonContent = Tools.read(versionInfoFile)
            val obj = JsonParser.parseString(jsonContent).asJsonObject
            if (!obj.has("minecraftVersion")) return

            val minecraftVersion = obj.get("minecraftVersion").asString
            val numericPart = minecraftVersion.substringBefore('-')
            val parts = numericPart.split(".")
            if (parts.size < 2) return

            val major = parts[0].toInt()
            val minor = parts[1].toInt()

            if (major > 26 || (major == 26 && minor >= 2)) {
                if (isVulkan12Supported(context)) {
                    MCOptions.set("preferredGraphicsBackend", SETTING_VULKAN)
                } else {
                    MCOptions.set("preferredGraphicsBackend", SETTING_OPENGL)
                }
            }
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
/**
 * getVulkanStatusする
 */
    fun getVulkanStatus(context: Context): VulkanStatus {
        val version = getDetectedVulkanVersion(context)
        val isVulkan12 = isVulkan12Supported(context)
        val dynamicRenderingSupported = isVulkan13Supported(context)
        // There is no reliable Android framework API to query this extension directly.
        // Keep conservative behavior in the warning dialog.
        val pushDescriptorSupported = false
        return VulkanStatus(
            version = version,
            vulkan12Supported = isVulkan12,
            dynamicRenderingSupported = dynamicRenderingSupported,
            pushDescriptorSupported = pushDescriptorSupported
        )
    }

/**
 * getDetectedVulkanVersionする
 */
    /**
     * デバイスがサポートするVulkanバージョンを検出する
     * @param context コンテキスト
     * @return 検出されたVulkanバージョン文字列
     */
    private fun getDetectedVulkanVersion(context: Context): String {
        return when {
            isFeatureVersionSupported(context, VULKAN_1_3_VERSION) -> "1.3"
            isFeatureVersionSupported(context, VULKAN_1_2_VERSION) -> "1.2"
            isFeatureVersionSupported(context, VULKAN_1_1_VERSION) -> "1.1"
            isFeatureVersionSupported(context, VULKAN_1_0_VERSION) -> "1.0"
            else -> "Not Available"
        }
    }

/**
 * isVulkan13Supportedする
 */
    /**
     * Vulkan 1.3がサポートされているかを確認する
     * @param context コンテキスト
     * @return サポートされている場合はtrue
     */
    private fun isVulkan13Supported(context: Context): Boolean =
        isFeatureVersionSupported(context, VULKAN_1_3_VERSION)

    /**
     * 指定されたVulkanバージョンがサポートされているかを確認する
     * @param context コンテキスト
     * @param version 確認するバージョンコード
     * @return サポートされている場合はtrue
     */
    private fun isFeatureVersionSupported(context: Context, version: Int): Boolean {
        return try {
            context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
                version
            )
        } catch (_: Throwable) {
            false
        }
    }
}
