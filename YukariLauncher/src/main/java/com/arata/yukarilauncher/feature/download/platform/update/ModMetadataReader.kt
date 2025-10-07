package com.arata.yukarilauncher.feature.download.platform.update

import org.json.JSONObject
import java.io.File
import java.util.jar.JarFile

object ModMetadataReader {

    data class ModInfo(
        val modId: String,
        val modName: String,
        val version: String,
        val loader: String
    )

    /** Parse Fabric mod metadata (fabric.mod.json) */
    private fun parseFabric(jar: File): ModInfo? {
        return try {
            JarFile(jar).use { jarFile ->
                val entry = jarFile.getJarEntry("fabric.mod.json") ?: return null
                val json = JSONObject(jarFile.getInputStream(entry).bufferedReader().readText())
                ModInfo(
                    modId = json.getString("id"),
                    modName = json.optString("name", json.getString("id")),
                    version = json.optString("version", "unknown"),
                    loader = "fabric"
                )
            }
        } catch (e: Exception) {
            // Prevent crash if mod JAR is invalid or has duplicate entries
            println("⚠️ Failed to parse Fabric mod ${jar.name}: ${e.message}")
            null
        }
    }

    /** Parse Forge or NeoForge mod metadata (META-INF/mods.toml) */
    private fun parseForge(jar: File): ModInfo? {
        return try {
            JarFile(jar).use { jarFile ->
                val entry = jarFile.getJarEntry("META-INF/mods.toml") ?: return null
                val text = jarFile.getInputStream(entry).bufferedReader().readText()

                val modId = Regex("""modId\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: return null
                val version = Regex("""version\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: "unknown"
                val displayName = Regex("""displayName\s*=\s*"([^"]+)"""").find(text)?.groupValues?.get(1) ?: modId

                ModInfo(
                    modId = modId,
                    modName = displayName,
                    version = version,
                    loader = "forge"
                )
            }
        } catch (e: Exception) {
            println("⚠️ Failed to parse Forge mod ${jar.name}: ${e.message}")
            null
        }
    }

    /** Parse both Fabric, Forge, and NeoForge mods safely */
    fun parseMod(jar: File): ModInfo? {
        // Try Fabric first, then Forge/NeoForge
        return parseFabric(jar) ?: parseForge(jar)
    }
}
