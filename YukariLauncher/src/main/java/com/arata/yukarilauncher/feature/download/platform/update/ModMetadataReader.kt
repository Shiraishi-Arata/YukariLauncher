package com.arata.yukarilauncher.feature.download.platform.update

import com.arata.yukarilauncher.feature.log.Logging
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile

object ModMetadataReader {

    enum class ModLoaderType(val apiName: String) {
        FABRIC("fabric"),
        QUILT("quilt"),
        FORGE("forge"),
        NEOFORGE("neoforge")
    }

    data class ModInfo(
        val modId: String,
        val modName: String,
        val version: String,
        val loader: String,
        val supportedLoaders: Set<String>,
        val file: File,
        val sha1: String? = null   // 信頼性の高いModrinth検索のためのJARのSHA-1ハッシュ
    )

    /**
     * ファイルのSHA-1ハッシュを計算する
     * ファイルが読み取れない場合はnullを返す
     * @param file ハッシュを計算するファイル
     * @return SHA-1ハッシュの16進文字列。失敗時はnull
     */
/**
 * computeSha1する
 */
    private fun computeSha1(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Logging.w("ModMetadata", "Failed to compute SHA-1 for ${file.name}: ${e.message}")
            null
        }
    }

    /**
     * Fabric Modのメタデータを解析する
     * @param jar 解析するJARファイル
     * @return 解析されたModInfo。Fabric Modでない場合はnull
     */
/**
 * parseFabricする
 */
    private fun parseFabric(jar: File): ModInfo? {
        return try {
            JarFile(jar).use { jarFile ->
                val entry = jarFile.getJarEntry("fabric.mod.json") ?: return null
                val json = JSONObject(jarFile.getInputStream(entry).bufferedReader().readText())
                val supportedLoaders = mutableSetOf(ModLoaderType.FABRIC.apiName)

                val depends = json.optJSONObject("depends")
                if (depends?.has("quilt_loader") == true) {
                    supportedLoaders.add(ModLoaderType.QUILT.apiName)
                }

                ModInfo(
                    modId = json.getString("id"),
                    modName = json.optString("name", json.getString("id")),
                    version = json.optString("version", "unknown"),
                    loader = ModLoaderType.FABRIC.apiName,
                    supportedLoaders = supportedLoaders,
                    file = jar
                )
            }
        } catch (e: Exception) {
            Logging.e("ModMetadata", "Failed to parse Fabric mod ${jar.name}", e)
            null
        }
    }

    /**
     * Quilt Modのメタデータを解析する
     * @param jar 解析するJARファイル
     * @return 解析されたModInfo。Quilt Modでない場合はnull
     */
/**
 * parseQuiltする
 */
    private fun parseQuilt(jar: File): ModInfo? {
        return try {
            JarFile(jar).use { jarFile ->
                val entry = jarFile.getJarEntry("quilt.mod.json") ?: return null
                val json = JSONObject(jarFile.getInputStream(entry).bufferedReader().readText())
                val quiltLoader = json.optJSONObject("quilt_loader")
                val metadata = quiltLoader?.optJSONObject("metadata")
                val id = quiltLoader?.optString("id")
                    ?: metadata?.optString("id")
                    ?: return null
                val name = metadata?.optString("name", id) ?: id
                val version = quiltLoader?.optString("version", "unknown")
                    ?: metadata?.optString("version", "unknown")
                    ?: "unknown"

                ModInfo(
                    modId = id,
                    modName = name,
                    version = version,
                    loader = ModLoaderType.QUILT.apiName,
                    supportedLoaders = setOf(ModLoaderType.QUILT.apiName),
                    file = jar
                )
            }
        } catch (e: Exception) {
            Logging.e("ModMetadata", "Failed to parse Quilt mod ${jar.name}", e)
            null
        }
    }

    /**
     * Forge/NeoForge Modのメタデータを解析する
     * @param jar 解析するJARファイル
     * @return 解析されたModInfo。Forge Modでない場合はnull
     */
/**
 * parseForgeする
 */
    private fun parseForge(jar: File): ModInfo? {
        return try {
            JarFile(jar).use { jarFile ->
                val neoForgeEntry = jarFile.getJarEntry("META-INF/neoforge.mods.toml")
                val forgeEntry = jarFile.getJarEntry("META-INF/mods.toml")
                val entry = neoForgeEntry ?: forgeEntry ?: return null
                val text = jarFile.getInputStream(entry).bufferedReader().readText()

                val modId = Regex("""modId\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: return null
                val version = Regex("""version\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: "unknown"
                val displayName = Regex("""displayName\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: modId
                val loader = if (neoForgeEntry != null) {
                    ModLoaderType.NEOFORGE.apiName
                } else {
                    ModLoaderType.FORGE.apiName
                }

                ModInfo(
                    modId = modId,
                    modName = displayName,
                    version = version,
                    loader = loader,
                    supportedLoaders = setOf(loader),
                    file = jar
                )
            }
        } catch (e: Exception) {
            Logging.e("ModMetadata", "Failed to parse Forge mod ${jar.name}", e)
            null
        }
    }

    /**
     * JARファイルからModのメタデータを解析する
     * Fabric/Quilt/Forgeの各パーサーを順に試行し、最初に成功した結果を使用する
     * @param jar 解析するJARファイル
     * @return 解析されたModInfo。どのModタイプにも該当しない場合はnull
     */
/**
 * parseModする
 */
    fun parseMod(jar: File): ModInfo? {
        val quiltInfo = parseQuilt(jar)
        val fabricInfo = parseFabric(jar)
        val forgeInfo = parseForge(jar)

        if (quiltInfo == null && fabricInfo == null && forgeInfo == null) {
            return null
        }

        val supportedLoaders = linkedSetOf<String>().apply {
            quiltInfo?.supportedLoaders?.let(::addAll)
            fabricInfo?.supportedLoaders?.let(::addAll)
            forgeInfo?.supportedLoaders?.let(::addAll)
        }

        // Forge/NeoForgeのメタデータを優先する
        // これにより、fabric.mod.jsonとneoforge.mods.tomlの両方を含む
        // デュアルローダーModがForge系インスタンスで正しく解決される
        val primary = forgeInfo ?: quiltInfo ?: fabricInfo ?: return null

        return primary.copy(
            supportedLoaders = supportedLoaders,
            // アップデートヘルパーが信頼性の高いハッシュ検索を行えるようにSHA-1を付加
            sha1 = computeSha1(jar)
        )
    }
}
