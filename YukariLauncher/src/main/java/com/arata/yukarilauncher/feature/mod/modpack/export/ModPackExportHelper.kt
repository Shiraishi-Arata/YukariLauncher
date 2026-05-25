package com.arata.yukarilauncher.feature.mod.modpack.export

import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.utils.file.FileTools
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.mod.modpack.api.ApiHandler
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModPackExportHelper {
    enum class ExportType {
        MODRINTH,
        CURSEFORGE
    }

    data class ExportOptions(
        val includePaths: Set<String> = emptySet(),
        val packName: String? = null,
        val packVersion: String? = null,
        val author: String? = null
    )

    companion object {
        private const val MODRINTH_API = "https://api.modrinth.com/v2"
        private const val CURSEFORGE_API = "https://api.curseforge.com/v1"

        private data class ResolvedProject(
            val projectId: Long? = null,
            val fileId: Long? = null,
            val downloadUrl: String? = null,
            val projectSlug: String? = null,
            val projectTitle: String? = null,
            val projectAuthor: String? = null
        )

        @JvmStatic
/**
 * exportする
 */
        fun export(version: Version, exportType: ExportType, options: ExportOptions = ExportOptions()): File {
            val gameDir = version.getGameDir()
            val exportDir = File(File(ProfilePathHome.getGameHome()).parentFile, "exported").apply { mkdirs() }
            val suffix = if (exportType == ExportType.MODRINTH) ".mrpack" else ".zip"
            val filenameVersion = (options.packVersion ?: version.getVersionName()).replace("/", "_")
            val exportFile = File(exportDir, "${version.getVersionName()}-$filenameVersion$suffix")
            val dependencies = buildDependencies(version)
            val includedFiles = collectIncludedFiles(gameDir, options)

            ZipOutputStream(FileOutputStream(exportFile)).use { zos ->
                when (exportType) {
                    ExportType.MODRINTH -> {
                        val resolvedProjects = resolveModrinthProjects(includedFiles)
                        val indexedFiles = includedFiles.filter { resolvedProjects.containsKey(it.first) }
                        val index = buildModrinthIndex(version, dependencies, options, indexedFiles, resolvedProjects)
                        writeJsonEntry(zos, "modrinth.index.json", index)
                        val overrideFiles = includedFiles.filterNot { filePair -> indexedFiles.any { it.first == filePair.first } }
                        zipOverrides(zos, overrideFiles)
                    }

                    ExportType.CURSEFORGE -> {
                        val manifestFiles = selectCurseManifestFiles(includedFiles)
                        val resolvedProjects = resolveCurseForgeProjects(manifestFiles)
                        val manifest = buildCurseManifest(version, dependencies, options, manifestFiles, resolvedProjects)
                        writeJsonEntry(zos, "manifest.json", manifest)
                        writeHtmlEntry(zos, "modlist.html", buildModListHtml(includedFiles, resolvedProjects))
                        val overrideFiles = includedFiles.filterNot { filePair -> manifestFiles.any { it.first == filePair.first } }
                        zipOverrides(zos, overrideFiles)
                    }
                }
            }

            return exportFile
        }

/**
 * shouldIncludeする
 */
        private fun shouldInclude(relativePath: String, options: ExportOptions): Boolean {
            if (relativePath.isBlank()) return false
            val normalizedPath = relativePath.trim('/')
            val include = options.includePaths
            if (include.isEmpty()) return false
            return include.any { normalizedPath == it || normalizedPath.startsWith("$it/") }
        }

/**
 * collectIncludedFilesする
 */
        private fun collectIncludedFiles(gameDir: File, options: ExportOptions): List<Pair<String, File>> {
            return gameDir.walkTopDown()
                .filter { it.isFile }
                .map { file ->
                    gameDir.toPath().relativize(file.toPath()).toString().replace('\\', '/') to file
                }
                .filter { (path, _) -> shouldInclude(path, options) }
                .toList()
        }

/**
 * zipOverridesする
 */
        private fun zipOverrides(zos: ZipOutputStream, overrideFiles: List<Pair<String, File>>) {
            overrideFiles.forEach { (path, file) ->
                FileTools.zipFile(file, "overrides/$path", zos)
            }
        }

/**
 * selectCurseManifestFilesする
 */
        private fun selectCurseManifestFiles(includedFiles: List<Pair<String, File>>): List<Pair<String, File>> {
            return includedFiles.filter { (path, file) ->
                path.startsWith("mods/") && file.extension.equals("jar", ignoreCase = true)
            }
        }

/**
 * buildDependenciesする
 */
        private fun buildDependencies(version: Version): MutableMap<String, String> {
            val dependencies = mutableMapOf<String, String>()
            version.getVersionInfo()?.let { info ->
                dependencies["minecraft"] = info.minecraftVersion
                info.loaderInfo?.forEach { loader ->
                    val key = when (loader.name.lowercase(Locale.ROOT)) {
                        "forge" -> "forge"
                        "neoforge" -> "neoforge"
                        "fabric" -> "fabric-loader"
                        "quilt" -> "quilt-loader"
                        else -> null
                    }
                    if (key != null && loader.version.isNotBlank()) dependencies[key] = loader.version
                }
            }
            return dependencies
        }

/**
 * buildModrinthIndexする
 */
        private fun buildModrinthIndex(
            version: Version,
            dependencies: Map<String, String>,
            options: ExportOptions,
            indexedFiles: List<Pair<String, File>>,
            resolvedProjects: Map<String, ResolvedProject>
        ): Map<String, Any> {
            val files = indexedFiles.map { (path, file) ->
                val project = resolvedProjects[path]
                mapOf(
                    "path" to path,
                    "hashes" to mapOf(
                        "sha1" to FileTools.calculateFileHash(file, "SHA-1"),
                        "sha512" to FileTools.calculateFileHash(file, "SHA-512")
                    ),
                    "env" to mapOf(
                        "client" to "required",
                        "server" to "required"
                    ),
                    "downloads" to listOf(project?.downloadUrl ?: file.toURI().toString()),
                    "fileSize" to file.length()
                )
            }
            return mapOf(
                "formatVersion" to 1,
                "game" to "minecraft",
                "versionId" to (options.packVersion ?: version.getVersionName()),
                "name" to (options.packName ?: version.getVersionName()),
                "files" to files,
                "dependencies" to dependencies
            )
        }

/**
 * buildCurseManifestする
 */
        private fun buildCurseManifest(
            versionObj: Version,
            dependencies: Map<String, String>,
            options: ExportOptions,
            manifestFiles: List<Pair<String, File>>,
            resolvedProjects: Map<String, ResolvedProject>
        ): Map<String, Any> {
            val curseFiles = manifestFiles
                .map { (path, file) ->
                    val parsedIds = parseCurseIds(file.name)
                    val project = resolvedProjects[path]
                    mapOf(
                        "projectID" to (project?.projectId ?: parsedIds.first),
                        "fileID" to (project?.fileId ?: parsedIds.second),
                        "required" to true,
                        "isLocked" to false
                    )
                }

            val modLoaders = dependencies.entries
                .filter { it.key != "minecraft" }
                .map {
                    val curseLoaderName = when (it.key) {
                        "fabric-loader" -> "fabric"
                        "quilt-loader" -> "quilt"
                        else -> it.key
                    }
                    mapOf("id" to "$curseLoaderName-${it.value}", "primary" to true)
                }

            return mapOf(
                "minecraft" to mapOf(
                    "version" to (dependencies["minecraft"] ?: versionObj.getVersionName()),
                    "modLoaders" to modLoaders
                ),
                "manifestType" to "minecraftModpack",
                "manifestVersion" to 1,
                "name" to (options.packName ?: versionObj.getVersionName()),
                "version" to (options.packVersion ?: "1.0.0"),
                "author" to (options.author ?: "YukariLauncher"),
                "files" to curseFiles
            )
        }

/**
 * writeJsonEntryする
 */
        private fun writeJsonEntry(zos: ZipOutputStream, entryName: String, obj: Any) {
            zos.putNextEntry(ZipEntry(entryName))
            zos.write(Tools.GLOBAL_GSON.toJson(obj).toByteArray())
            zos.closeEntry()
        }

/**
 * writeHtmlEntryする
 */
        private fun writeHtmlEntry(zos: ZipOutputStream, entryName: String, html: String) {
            zos.putNextEntry(ZipEntry(entryName))
            zos.write(html.toByteArray())
            zos.closeEntry()
        }

/**
 * parseCurseIdsする
 */
        private fun parseCurseIds(fileName: String): Pair<Long, Long> {
            val numbers = Regex("(\\d+)").findAll(fileName).map { it.value.toLong() }.toList()
            return if (numbers.size >= 2) numbers[numbers.size - 2] to numbers.last() else 0L to 0L
        }

/**
 * buildModListHtmlする
 */
        private fun buildModListHtml(
            includedFiles: List<Pair<String, File>>,
            resolvedProjects: Map<String, ResolvedProject>
        ): String {
            val mods = includedFiles.filter { (path, _) -> path.startsWith("mods/") }
            val listItems = mods.joinToString("\n") { (path, file) ->
                val project = resolvedProjects[path]
                val modName = project?.projectTitle ?: file.nameWithoutExtension
                val author = project?.projectAuthor ?: "Unknown"
                val slug = project?.projectSlug
                    ?: modName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
                """<li><a href="https://www.curseforge.com/minecraft/mc-mods/$slug">$modName (by $author)</a></li>"""
            }
            return "<ul>\n$listItems\n</ul>"
        }

/**
 * resolveModrinthProjectsする
 */
        private fun resolveModrinthProjects(candidateFiles: List<Pair<String, File>>): Map<String, ResolvedProject> {
            if (candidateFiles.isEmpty()) return emptyMap()
            return runCatching {
                val hashToPath = candidateFiles.associate { FileTools.calculateFileHash(it.second, "SHA-1") to it.first }
                val payload = JsonObject().apply {
                    addProperty("algorithm", "sha1")
                    add("hashes", JsonArray().apply { hashToPath.keys.forEach { add(it) } })
                }
                val response = ApiHandler.postRaw("$MODRINTH_API/version_files", payload.toString()) ?: return emptyMap()
                val jsonObject = Tools.GLOBAL_GSON.fromJson(response, JsonObject::class.java)

                val results = mutableMapOf<String, ResolvedProject>()
                hashToPath.forEach { (sha1, path) ->
                    val versionObject = jsonObject[sha1]?.asJsonObject ?: return@forEach
                    val projectId = versionObject["project_id"]?.asString
                    val fileArray = versionObject["files"]?.asJsonArray
                    val downloadUrl = fileArray?.firstOrNull()?.asJsonObject?.get("url")?.asString
                    if (projectId.isNullOrBlank() || downloadUrl.isNullOrBlank()) return@forEach
                    results[path] = ResolvedProject(
                        downloadUrl = downloadUrl,
                        projectSlug = projectId
                    )
                }
                results
            }.getOrDefault(emptyMap())
        }

/**
 * resolveCurseForgeProjectsする
 */
        private fun resolveCurseForgeProjects(manifestFiles: List<Pair<String, File>>): Map<String, ResolvedProject> {
            if (manifestFiles.isEmpty()) return emptyMap()
            if (InfoDistributor.CURSEFORGE_API_KEY.isBlank()) return emptyMap()

            return runCatching {
                val fingerprints = manifestFiles.associate { calcCurseFingerprint(it.second) to it.first }
                val payload = JsonObject().apply {
                    add("fingerprints", JsonArray().apply { fingerprints.keys.forEach { add(it) } })
                }
                val headers = mapOf("x-api-key" to InfoDistributor.CURSEFORGE_API_KEY)
                val response = ApiHandler.postRaw(headers, "$CURSEFORGE_API/fingerprints", payload.toString())
                    ?: return emptyMap()
                val jsonObject = Tools.GLOBAL_GSON.fromJson(response, JsonObject::class.java)
                val data = jsonObject["data"]?.asJsonObject ?: return emptyMap()
                val exactMatches = data["exactMatches"]?.asJsonArray ?: return emptyMap()
                val modInfoCache = mutableMapOf<Long, ResolvedProject>()

                val results = mutableMapOf<String, ResolvedProject>()
                exactMatches.forEach { element ->
                    val fileObj = element.asJsonObject["file"]?.asJsonObject ?: return@forEach
                    val fingerprint = fileObj["packageFingerprint"]?.asLong ?: return@forEach
                    val path = fingerprints[fingerprint] ?: return@forEach
                    val modId = fileObj["modId"]?.asLong ?: return@forEach
                    val modInfo = modInfoCache.getOrPut(modId) {
                        resolveCurseForgeModInfo(headers, modId)
                    }
                    results[path] = ResolvedProject(
                        projectId = modId,
                        fileId = fileObj["id"]?.asLong,
                        projectSlug = modInfo.projectSlug,
                        projectTitle = modInfo.projectTitle,
                        projectAuthor = modInfo.projectAuthor
                    )
                }
                results
            }.getOrDefault(emptyMap())
        }

/**
 * resolveCurseForgeModInfoする
 */
        private fun resolveCurseForgeModInfo(headers: Map<String, String>, modId: Long): ResolvedProject {
            return runCatching {
                val response = ApiHandler.getRaw(headers, "$CURSEFORGE_API/mods/$modId") ?: return ResolvedProject()
                val jsonObject = Tools.GLOBAL_GSON.fromJson(response, JsonObject::class.java)
                val data = jsonObject["data"]?.asJsonObject ?: return ResolvedProject()
                val firstAuthor = data["authors"]?.asJsonArray?.firstOrNull()?.asJsonObject?.get("name")?.asString
                ResolvedProject(
                    projectSlug = data["slug"]?.asString,
                    projectTitle = data["name"]?.asString,
                    projectAuthor = firstAuthor
                )
            }.getOrDefault(ResolvedProject())
        }

/**
 * calcCurseFingerprintする
 */
        private fun calcCurseFingerprint(file: File): Long {
            val hash = murmur2(file.readBytes(), 1)
            return hash.toLong() and 0xffffffffL
        }

/**
 * murmur2する
 */
        private fun murmur2(data: ByteArray, seed: Int): Int {
            val m = 0x5bd1e995
            val r = 24
            var h = seed xor data.size
            var len = data.size
            var i = 0
            while (len >= 4) {
                var k = (data[i].toInt() and 0xff) or
                    ((data[i + 1].toInt() and 0xff) shl 8) or
                    ((data[i + 2].toInt() and 0xff) shl 16) or
                    ((data[i + 3].toInt() and 0xff) shl 24)
                k *= m
                k = k xor (k ushr r)
                k *= m
                h *= m
                h = h xor k
                i += 4
                len -= 4
            }
            when (len) {
                3 -> h = h xor ((data[i + 2].toInt() and 0xff) shl 16)
                2 -> h = h xor ((data[i + 1].toInt() and 0xff) shl 8)
                1 -> {
                    h = h xor (data[i].toInt() and 0xff)
                    h *= m
                }
            }
            h = h xor (h ushr 13)
            h *= m
            h = h xor (h ushr 15)
            return h
        }
    }
}
