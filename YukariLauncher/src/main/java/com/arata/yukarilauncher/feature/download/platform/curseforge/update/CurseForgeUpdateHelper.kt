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
     * バージョン文字列を正規化して大まかな等価性をチェックできるようにする
     * 先頭の"v"、スペース、ビルドメタデータ（"+..." サフィックス）を除去する
     * 例: "v1.40.0+mc1.21.1" → "1.40.0"
     * @param v 元のバージョン文字列
     * @return 正規化されたバージョン文字列
     */
/**
 * normalizeVersionする
 */
    private fun normalizeVersion(v: String): String =
        v.trim()
            .lowercase()
            .removePrefix("v")
            .replace(" ", "")
            .substringBefore("+")   // "+mc1.21.1" のようなビルドメタデータを削除

    /**
     * 現在のバージョンと最新バージョンが等価かどうかを判定する
     * @param current 現在のバージョン
     * @param latest 最新バージョン
     * @return 等価の場合はtrue
     */
/**
 * versionsEquivalentする
 */
    private fun versionsEquivalent(current: String, latest: String): Boolean {
        val nc = normalizeVersion(current)
        val nl = normalizeVersion(latest)
        return nc.isNotBlank() &&
            nc != "unknown" &&
            !nc.contains("\${") &&
            nc == nl
    }

    /**
     * CurseForge APIを使用してModのアップデートを確認する
     * @param modId ModのID
     * @param currentVersion 現在のバージョン
     * @param minecraftVersion Minecraftのバージョン
     * @param currentFileName 現在のファイル名（オプション）
     * @return アップデート情報。見つからない場合はnull
     */
/**
 * checkUpdateする
 */
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

            // 結果リストからインストール済みファイルを特定する
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