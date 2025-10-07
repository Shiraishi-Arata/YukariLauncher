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

    fun checkUpdate(modId: String, currentVersion: String): ModUpdate? {
        val request = Request.Builder()
            .url("$BASE_URL/mods/$modId/files")
            .addHeader("x-api-key", API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val data = JSONObject(response.body?.string() ?: return null).getJSONArray("data")
            if (data.length() == 0) return null

            val latest = data.getJSONObject(0)
            val latestVersion = latest.getString("displayName")
            val downloadUrl = latest.getString("downloadUrl")

            return ModUpdate(
                modId = modId,
                modName = latest.getString("fileName"),
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                needsUpdate = currentVersion != latestVersion
            )
        }
    }
}
