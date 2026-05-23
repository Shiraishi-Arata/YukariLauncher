package com.arata.yukarilauncher.feature.download.platform.modrinth.update

import com.arata.yukarilauncher.feature.download.platform.update.ModUpdate
import com.arata.yukarilauncher.feature.log.Logging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object ModrinthUpdateHelper {
    private val client = OkHttpClient()

    // ---------- ヘルパー ----------

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
     * 指定されたバージョンが特定のファイル名を持つかどうかを確認する
     * @param version 確認するバージョンJSONオブジェクト
     * @param fileName 確認するファイル名
     * @return ファイル名が存在する場合はtrue
     */
/**
 * hasFileNameする
 */
    private fun hasFileName(version: JSONObject, fileName: String): Boolean {
        val files = version.optJSONArray("files") ?: return false
        for (i in 0 until files.length()) {
            if (fileName.equals(files.getJSONObject(i).optString("filename"), ignoreCase = true))
                return true
        }
        return false
    }

    /**
     * バージョンのJSON配列を、指定されたMinecraftバージョンとローダーでフィルタリングする
     * @param versions バージョンのJSON配列
     * @param minecraftVersion 互換性のあるMinecraftバージョン
     * @param loader 互換性のあるローダー（オプション）
     * @return フィルタリングされたJSONObjectのリスト
     */
/**
 * filterCompatibleする
 */
    private fun filterCompatible(
        versions: JSONArray,
        minecraftVersion: String,
        loader: String?
    ): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        for (i in 0 until versions.length()) {
            val v = versions.getJSONObject(i)

            val gameVersions = v.getJSONArray("game_versions")
            val gameOk = (0 until gameVersions.length()).any {
                gameVersions.getString(it) == minecraftVersion
            }
            if (!gameOk) continue

            if (loader != null) {
                val loaders = v.getJSONArray("loaders")
                val loaderOk = (0 until loaders.length()).any {
                    loaders.getString(it).equals(loader, ignoreCase = true)
                }
                if (!loaderOk) continue
            }

            result.add(v)
        }
        return result
    }

    /**
     * 最新バージョン情報からModUpdateを構築する
     * @param modId ModのID
     * @param modName Modの名前
     * @param currentVersion 現在のバージョン
     * @param latest 最新バージョンのJSONオブジェクト
     * @param currentVersionId 現在のバージョンID（nullの場合は文字列比較にフォールバック）
     * @return 構築されたModUpdate
     */
/**
 * buildModUpdateする
 */
    private fun buildModUpdate(
        modId: String,
        modName: String,
        currentVersion: String,
        latest: JSONObject,
        currentVersionId: String?     // null → 文字列比較にフォールバック
    ): ModUpdate {
        val latestVersionId = latest.getString("id")
        val latestVersionNumber = latest.getString("version_number")
        val files = latest.getJSONArray("files")
        val firstFile = files.getJSONObject(0)

        val needsUpdate = when {
            currentVersionId != null -> currentVersionId != latestVersionId
            else -> !versionsEquivalent(currentVersion, latestVersionNumber)
        }

        return ModUpdate(
            modId = modId,
            modName = modName,
            currentVersion = currentVersion,
            latestVersion = latestVersionNumber,
            downloadUrl = firstFile.getString("url"),
            fileName = firstFile.getString("filename"),
            needsUpdate = needsUpdate
        )
    }

    // ---------- 公開API ----------

    /**
     * プライマリパス：インストール済みファイルをSHA-1ハッシュで特定し、
     * Modrinthに新しい互換バージョンが存在するか確認する
     *
     * この方法はmodIdとModrinthスラグの不一致問題を完全に回避する
     * ハッシュがModrinthで認識されない場合（例：CurseForge専用Mod）はnullを返す
     *
     * @param sha1 ファイルのSHA-1ハッシュ
     * @param fallbackModName フォールバック用のMod名
     * @param minecraftVersion Minecraftのバージョン
     * @param loader ローダー名（オプション）
     * @return アップデート情報。見つからない場合はnull
     */
/**
 * checkUpdateByHashする
 */
    fun checkUpdateByHash(
        sha1: String,
        fallbackModName: String,
        minecraftVersion: String,
        loader: String?
    ): ModUpdate? {
        // Step 1 – インストール済みファイルをModrinthのバージョンに解決
        val hashRequest = Request.Builder()
            .url("https://api.modrinth.com/v2/version_file/$sha1?algorithm=sha1")
            .build()

        val installedVersion: JSONObject
        try {
            client.newCall(hashRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Logging.i("ModUpdate", "Hash $sha1 not found on Modrinth (HTTP ${response.code})")
                    return null
                }
                installedVersion = JSONObject(response.body?.string() ?: return null)
            }
        } catch (e: Exception) {
            Logging.e("ModUpdate", "Hash lookup failed for $fallbackModName", e)
            return null
        }

        val projectId = installedVersion.getString("project_id")
        val installedVersionId = installedVersion.getString("id")
        val installedVersionNumber = installedVersion.getString("version_number")

        Logging.i("ModUpdate", "Hash resolved: $fallbackModName → project $projectId @ $installedVersionNumber")

        // Step 2 – このプロジェクトの全バージョンを取得し、互換性のある最新バージョンを選択
        val listRequest = Request.Builder()
            .url("https://api.modrinth.com/v2/project/$projectId/version")
            .build()

        return try {
            client.newCall(listRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                val versions = JSONArray(response.body?.string() ?: return null)
                val compatible = filterCompatible(versions, minecraftVersion, loader)
                if (compatible.isEmpty()) return null

                buildModUpdate(
                    modId = projectId,
                    modName = fallbackModName,
                    currentVersion = installedVersionNumber,
                    latest = compatible[0],
                    currentVersionId = installedVersionId
                )
            }
        } catch (e: Exception) {
            Logging.e("ModUpdate", "Version list fetch failed for $fallbackModName", e)
            null
        }
    }

    /**
     * フォールバックパス：Modの内部ID/既知のModrinthスラグを直接使用する
     * fabric.mod.jsonのidとModrinthのプロジェクトスラグは
     * しばしば異なるため信頼性は低いが、ベストエフォートのフォールバックとして維持
     *
     * @param projectIdOrSlug ModrinthのプロジェクトIDまたはスラグ
     * @param currentVersion 現在のバージョン
     * @param minecraftVersion Minecraftのバージョン
     * @param loader ローダー名（オプション）
     * @param currentFileName 現在のファイル名（オプション）
     * @return アップデート情報。見つからない場合はnull
     */
/**
 * checkUpdateする
 */
    fun checkUpdate(
        projectIdOrSlug: String,
        currentVersion: String,
        minecraftVersion: String,
        loader: String? = null,
        currentFileName: String? = null
    ): ModUpdate? {
        val request = Request.Builder()
            .url("https://api.modrinth.com/v2/project/$projectIdOrSlug/version")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val versions = JSONArray(response.body?.string() ?: return null)
                val compatible = filterCompatible(versions, minecraftVersion, loader)
                if (compatible.isEmpty()) return null

                val latest = compatible[0]
                val latestVersionId = latest.getString("id")

                // まずファイル名でインストール済みバージョンを特定し、
                // 見つからない場合はビルドメタデータを除去した文字列比較にフォールバック
                val installedByFile = currentFileName?.let { name ->
                    compatible.firstOrNull { hasFileName(it, name) }
                }
                val installedVersionId = installedByFile?.getString("id")
                val resolvedCurrentVersion =
                    installedByFile?.optString("version_number", currentVersion) ?: currentVersion

                buildModUpdate(
                    modId = projectIdOrSlug,
                    modName = latest.optString("name", projectIdOrSlug),
                    currentVersion = resolvedCurrentVersion,
                    latest = latest,
                    currentVersionId = installedVersionId
                )
            }
        } catch (e: Exception) {
            Logging.e("ModUpdate", "checkUpdate failed for $projectIdOrSlug", e)
            null
        }
    }
}
