package com.arata.yukarilauncher.feature.mod.modloader

import com.arata.yukarilauncher.feature.mod.modloader.ForgeVersionListHandler
import com.arata.yukarilauncher.utils.http.DownloadUtils
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class NeoForgeUtils {
    companion object {
        private const val NEOFORGE_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
        private const val NEOFORGE_INSTALLER_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1\$s/neoforge-%1\$s-installer.jar"
        private const val NEOFORGED_FORGE_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml"
        private const val NEOFORGED_FORGE_INSTALLER_URL =
            "https://maven.neoforged.net/releases/net/neoforged/forge/%1\$s/forge-%1\$s-installer.jar"

        @Throws(Exception::class)
/**
 * downloadVersionsする
 */
        private fun downloadVersions(metaDataUrl: String, name: String, force: Boolean): List<String> {
            val parserFactory = SAXParserFactory.newInstance()
            val saxParser = parserFactory.newSAXParser()

            return DownloadUtils.downloadStringCached<List<String>>(
                metaDataUrl,
                name,
                force,
            ) { input: String ->
                try {
                    val handler = ForgeVersionListHandler()
                    saxParser.parse(InputSource(StringReader(input)), handler)
                    handler.versions
                } catch (e: SAXException) {
                    throw DownloadUtils.ParseException(e)
                } catch (e: IOException) {
                    throw DownloadUtils.ParseException(e)
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
/**
 * downloadNeoForgeVersionsする
 */
        fun downloadNeoForgeVersions(force: Boolean): List<String> {
            return downloadVersions(NEOFORGE_METADATA_URL, "neoforge_versions", force)
        }

        @JvmStatic
        @Throws(Exception::class)
/**
 * downloadNeoForgedForgeVersionsする
 */
        fun downloadNeoForgedForgeVersions(force: Boolean): List<String> {
            return downloadVersions(NEOFORGED_FORGE_METADATA_URL, "neoforged_forge_versions", force)
        }

        @JvmStatic
/**
 * getNeoForgeInstallerUrlする
 */
        fun getNeoForgeInstallerUrl(version: String?): String {
            return String.format(NEOFORGE_INSTALLER_URL, version)
        }

        @JvmStatic
/**
 * getNeoForgedForgeInstallerUrlする
 */
        fun getNeoForgedForgeInstallerUrl(version: String?): String {
            return String.format(NEOFORGED_FORGE_INSTALLER_URL, version)
        }

        @JvmStatic
/**
 * formatGameVersionする
 */
        fun formatGameVersion(neoForgeVersion: String): String {
            val result = when {
                neoForgeVersion.contains("1.20.1") -> {
                    "1.20.1"
                }
                neoForgeVersion.startsWith("0.") -> {
                    val versionPart = neoForgeVersion.replace("0.", "").substringBefore("-")
                    val formatted = versionPart.substringBeforeLast(".")
                    formatted
                }
                else -> {
                    val fullVersion = neoForgeVersion.substringBefore("-")
                    val parts = fullVersion.split(".")
                    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    if (major >= 26) {
                        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val patch = parts.getOrNull(2)?.toIntOrNull()
                        val formatted = if (patch != null && patch != 0) {
                            "$major.$minor.$patch"
                        } else {
                            "$major.$minor"
                        }
                        formatted
                    } else {
                        val version = ForgeBuildVersion.parse(fullVersion)
                        val formatted = buildString {
                            append("1.").append(version.major)
                            if (version.minor != 0) append(".").append(version.minor)
                        }
                        formatted
                    }
                }
            }
            return result
        }
    }
}