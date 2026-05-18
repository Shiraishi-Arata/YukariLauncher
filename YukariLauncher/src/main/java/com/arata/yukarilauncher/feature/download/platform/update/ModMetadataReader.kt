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
        val sha1: String? = null   // SHA-1 hash of the JAR for reliable Modrinth lookup
    )

    /**
     * Computes the SHA-1 hex digest of [file].
     * Returns null if the file cannot be read.
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

        // Prefer Forge/NeoForge metadata when present so dual-loader mods
        // containing both fabric.mod.json and neoforge.mods.toml resolve
        // correctly for Forge-like instances.
        val primary = forgeInfo ?: quiltInfo ?: fabricInfo ?: return null

        return primary.copy(
            supportedLoaders = supportedLoaders,
            // Attach SHA-1 so update helpers can do a reliable hash-based lookup
            sha1 = computeSha1(jar)
        )
    }
}
