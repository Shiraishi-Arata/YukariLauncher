package com.arata.yukarilauncher.feature.download.platform.modrinth.update

import com.arata.yukarilauncher.feature.download.platform.update.ModUpdate
import com.arata.yukarilauncher.feature.log.Logging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object ModrinthUpdateHelper {
    private val client = OkHttpClient()

    // ---------- helpers ----------

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

    private fun hasFileName(version: JSONObject, fileName: String): Boolean {
        val files = version.optJSONArray("files") ?: return false
        for (i in 0 until files.length()) {
            if (fileName.equals(files.getJSONObject(i).optString("filename"), ignoreCase = true))
                return true
        }
        return false
    }

    /** Filter a raw versions JSONArray to those compatible with [minecraftVersion] and optionally [loader]. */
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

    private fun buildModUpdate(
        modId: String,
        modName: String,
        currentVersion: String,
        latest: JSONObject,
        currentVersionId: String?     // null → fall back to string comparison
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

    // ---------- public API ----------

    /**
     * PRIMARY path: identify the installed file via its SHA-1 hash, then check
     * whether a newer compatible version exists on Modrinth.
     *
     * This sidesteps the modId ↔ Modrinth-slug mismatch problem entirely.
     * Returns null if the hash is not recognised by Modrinth (e.g. CurseForge-only mod).
     */
    fun checkUpdateByHash(
        sha1: String,
        fallbackModName: String,
        minecraftVersion: String,
        loader: String?
    ): ModUpdate? {
        // Step 1 – resolve the installed file to a Modrinth version
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

        // Step 2 – fetch all versions for this project and pick the latest compatible one
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
     * FALLBACK path: use the mod's internal ID / known Modrinth slug directly.
     * Less reliable because the fabric.mod.json id and the Modrinth project slug
     * often differ, but kept as a best-effort fallback.
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

                // Try to pin the installed version by filename first, then fall back to
                // loose version-string comparison (with build-metadata stripped).
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