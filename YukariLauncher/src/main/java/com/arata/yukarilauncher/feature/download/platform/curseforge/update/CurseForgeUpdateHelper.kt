package com.arata.yukarilauncher.feature.download.platform.curseforge.update

import com.arata.yukarilauncher.feature.download.platform.update.ModUpdate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object CurseForgeUpdateHelper {
    private val client = OkHttpClient()
    private const val BASE_URL = "https://api.curseforge.com/v1"

    private val API_KEY: String by lazy {
        val file = File("curseforge_key.txt")
        if (file.exists()) file.readText().trim() else ""
    }

    /**
     * Normalise a version string for loose equality checking.
     * Strips leading "v", spaces, and build-metadata (the "+..." suffix).
     * Example: "v1.40.0+mc1.21.1" → "1.40.0"
     */
    private fun normalizeVersion(v: String): String =
        v.trim()
            .lowercase()
            .removePrefix("v")
            .replace(" ", "")
            .substringBefore("+")   // drop build-metadata like "+mc1.21.1"

    private fun versionsEquivalent(current: String, latest: String): Boolean {
        val nc = normalizeVersion(current)
        val nl = normalizeVersion(latest)
        return nc.isNotBlank() &&
            nc != "unknown" &&
            !nc.contains("\${") &&
            nc == nl
    }

    fun checkUpdate(
        modId: String,
        currentVersion: String,
        minecraftVersion: String,
        currentFileName: String? = null
    ): ModUpdate? {
        val url = "$BASE_URL/mods/$modId/files?gameVersion=$minecraftVersion"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val data = JSONObject(response.body?.string() ?: return null).getJSONArray("data")
            if (data.length() == 0) return null

            val latest = data.getJSONObject(0)
            val latestVersion = latest.getString("displayName")
            val downloadUrl = latest.getString("downloadUrl")
            val fileName = latest.getString("fileName")

            // Try to find the installed file in the results list
            val installedIndex = currentFileName?.let { installed ->
                (0 until data.length()).firstOrNull { i ->
                    installed.equals(data.getJSONObject(i).optString("fileName"), ignoreCase = true)
                }
            }

            val needsUpdate = when {
                installedIndex != null -> installedIndex != 0
                else -> !versionsEquivalent(currentVersion, latestVersion)
            }
            val resolvedCurrentVersion = if (installedIndex != null) {
                data.getJSONObject(installedIndex).optString("displayName", currentVersion)
            } else {
                currentVersion
            }

            return ModUpdate(
                modId = modId,
                modName = latest.getString("fileName"),
                currentVersion = resolvedCurrentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                fileName = fileName,
                needsUpdate = needsUpdate
            )
        }
    }
}