package com.arata.yukarilauncher.feature.download.platform.modrinth.update

import com.arata.yukarilauncher.feature.download.platform.update.ModUpdate
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object ModrinthUpdateHelper {
    private val client = OkHttpClient()

    fun checkUpdate(projectIdOrSlug: String, currentVersion: String): ModUpdate? {
        val request = Request.Builder()
            .url("https://api.modrinth.com/v2/project/$projectIdOrSlug/version")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val versions = JSONArray(response.body?.string() ?: return null)
            if (versions.length() == 0) return null

            val latest = versions.getJSONObject(0)
            val latestVersion = latest.getString("version_number")
            val downloadUrl = latest.getJSONArray("files").getJSONObject(0).getString("url")

            return ModUpdate(
                modId = projectIdOrSlug,
                modName = latest.getString("name"),
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                needsUpdate = currentVersion != latestVersion
            )
        }
    }
}
