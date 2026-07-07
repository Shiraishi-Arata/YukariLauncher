package com.arata.yukarilauncher.feature.version.install.forge

import android.content.Context
import android.content.Intent
import com.arata.yukarilauncher.feature.customprofilepath.ProfilePathHome
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.install.HeadlessInstaller
import com.arata.yukarilauncher.task.ProgressKeeper
import com.arata.yukarilauncher.ui.activity.InstallerActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

private const val TAG = "ForgeInstallTask"

object ForgeInstallTask {

    /**
     * Forge/NeoForge インストーラーを HMCL 方式で実行する。
     * インストーラーJARを直接実行する代わりに、install_profile.json を解析し、
     * プロセッサーを個別に実行する。
     *
     * @param context Context
     * @param installerJar インストーラーJARファイル
     * @param loaderName ローダー名（例: "Forge 47.2.0"）
     * @param customVersionName カスタムバージョン名
     * @param mcVersion Minecraft バージョン
     * @param jreName JRE名（null で自動選択）
     * @return true なら成功
     */
    @Throws(Throwable::class)
    fun install(
        context: Context,
        installerJar: File,
        loaderName: String,
        customVersionName: String,
        mcVersion: String,
        jreName: String? = null
    ): Boolean {
        val minecraftDir = File(ProfilePathHome.getGameHome())
        val librariesDir = File(ProfilePathHome.getLibrariesHome())
        val versionsDir = File(ProfilePathHome.getVersionsHome())
        val targetVersionDir = File(versionsDir, customVersionName)
        val targetVersionJson = File(targetVersionDir, "$customVersionName.json")
        val vanillaJar = File(versionsDir, "$mcVersion/$mcVersion.jar")

        // Download vanilla client JAR if not present
        if (!vanillaJar.isFile) {
            downloadVanillaJar(mcVersion, vanillaJar, versionsDir)
        }

        val tempDir = File(minecraftDir, ".temp/forge_installer_cache").also { it.mkdirs() }

        Logging.i(TAG, "Starting HMCL-style Forge install for $loaderName")
        Logging.i(TAG, "Installer: ${installerJar.absolutePath}")
        Logging.i(TAG, "Minecraft dir: ${minecraftDir.absolutePath}")

        ZipFile(installerJar).use { zip ->
            val installProfileJson = JsonParser.parseString(
                zip.getInputStream(zip.getEntry("install_profile.json")).reader().readText()
            ).asJsonObject

            val isNewFormat = installProfileJson.has("spec")

            if (isNewFormat) {
                // --- New format (spec-based) ---
                targetVersionDir.mkdirs()

                // Extract version.json and set its id to customVersionName
                val versionJsonStr = zip.getInputStream(
                    zip.getEntry("version.json")
                        ?: throw IOException("version.json not found in installer")
                ).reader().readText()
                val versionObj = JsonParser.parseString(versionJsonStr).asJsonObject
                versionObj.addProperty("id", customVersionName)
                targetVersionJson.writeText(
                    GsonBuilder().setPrettyPrinting().create().toJson(versionObj)
                )

                // Extract everything from maven/ directory
                Logging.i(TAG, "Extracting libraries from maven/ directory")
                val extracted = mutableSetOf<String>()
                val mavenPrefix = "maven/"
                val zipEntries = zip.entries()
                while (zipEntries.hasMoreElements()) {
                    val entry = zipEntries.nextElement()
                    val name = entry.name
                    if (name.startsWith("maven/") && !entry.isDirectory) {
                        val relPath = name.removePrefix(mavenPrefix)
                        val dest = File(librariesDir, relPath)
                        dest.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        extracted.add(relPath)
                    }
                }
                Logging.i(TAG, "Extracted ${extracted.size} files from maven/")

                // Parse data mappings — process in order to resolve interleaved variables
                Logging.i(TAG, "Parsing data mappings")
                val vars = mutableMapOf<String, String>()
                installProfileJson.getAsJsonObject("data")?.entrySet()?.forEach { (key, value) ->
                    if (value.isJsonObject) {
                        val client = value.asJsonObject.get("client")
                        if (client != null && client.isJsonPrimitive) {
                            val clientStr = replaceTokens(vars, client.asString)
                            val result = when {
                                // Literal '...' — try to extract from ZIP, else use as-is
                                clientStr.startsWith("'") && clientStr.endsWith("'") -> {
                                    val raw = clientStr.substring(1, clientStr.length - 1)
                                    val path = raw.removePrefix("\\").removePrefix("/").replace("\\", "/")
                                    val dest = File.createTempFile("forge_data_${key}_", ".dat", tempDir)
                                    try {
                                        extractEntryToFile(zip, path, dest)
                                        Logging.i(TAG, "Extracted data $key from $path")
                                        dest.absolutePath
                                    } catch (_: IllegalArgumentException) {
                                        raw
                                    }
                                }
                                // Maven artifact [...] — resolve from libraries/ZIP, download if missing
                                clientStr.startsWith("[") && clientStr.endsWith("]") -> {
                                    val lib = fromDescriptor(clientStr.substring(1, clientStr.length - 1))
                                    val file = File(librariesDir, lib.toPath())

                                    // Pre-download parent ZIP (e.g. mcp_config-...zip) so processors
                                    // like MCP_DATA can read it as --input. This runs even when the
                                    // classified file already exists in maven/, because the processor
                                    // needs the parent archive, not the classified stub file.
                                    if (!lib.classifier.isNullOrEmpty()) {
                                        for (parentExt in listOf("zip")) {
                                            val parentLib = lib.copy(classifier = null, extension = parentExt)
                                            val parentFile = File(librariesDir, parentLib.toPath())
                                            if (!parentFile.isFile) {
                                                Logging.i(TAG, "Downloading parent $parentLib")
                                                downloadFromMaven(parentLib, parentFile)
                                            }
                                        }
                                    }

                                    if (!file.isFile) {
                                        // The artifact may be inside a parent ZIP (e.g. mcp_config ships as .zip)
                                        // Try to locate or download the parent ZIP and extract the entry
                                        for (parentExt in listOf("zip", "jar")) {
                                            val parentLib = lib.copy(classifier = null, extension = parentExt)
                                            val parentFile = File(librariesDir, parentLib.toPath())
                                            if (!parentFile.isFile) {
                                                Logging.i(TAG, "Downloading parent $parentLib")
                                                downloadFromMaven(parentLib, parentFile)
                                            }
                                            if (parentFile.isFile) {
                                                val tried = mutableListOf<String>().apply {
                                                    add("config/${lib.classifier}.${lib.extension}")
                                                    add("configs/${lib.classifier}.${lib.extension}")
                                                    add("${lib.classifier}.${lib.extension}")
                                                    add("META-INF/${lib.classifier}.${lib.extension}")
                                                }
                                                try {
                                                    java.util.zip.ZipFile(parentFile).use { pz ->
                                                        for (entryName in tried) {
                                                            val entry = pz.getEntry(entryName)
                                                            if (entry != null) {
                                                                file.parentFile?.mkdirs()
                                                                pz.getInputStream(entry).use { input ->
                                                                    file.outputStream().use { output ->
                                                                        input.copyTo(output)
                                                                    }
                                                                }
                                                                Logging.i(TAG, "Extracted $entryName from ${parentFile.name}")
                                                                break
                                                            }
                                                        }
                                                        // Broader: search the parent ZIP by filename
                                                        if (!file.isFile) {
                                                            val fileName = "${lib.classifier}.${lib.extension}"
                                                            val en = pz.entries()
                                                            while (en.hasMoreElements()) {
                                                                val e = en.nextElement()
                                                                if (e.isDirectory) continue
                                                                if (e.name.substringAfterLast('/') == fileName) {
                                                                    file.parentFile?.mkdirs()
                                                                    pz.getInputStream(e).use { input ->
                                                                        file.outputStream().use { output ->
                                                                            input.copyTo(output)
                                                                        }
                                                                    }
                                                                    Logging.i(TAG, "Extracted $fileName at ${e.name} from ${parentFile.name}")
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                    Logging.w(TAG, "Failed to extract from ${parentFile.name}")
                                                }
                                                if (file.isFile) break
                                            }
                                        }
                                        // Search the installer JAR by filename
                                        if (!file.isFile) {
                                            val fileName = file.name
                                            val zipIter = zip.entries()
                                            while (zipIter.hasMoreElements()) {
                                                val entry = zipIter.nextElement()
                                                if (entry.isDirectory) continue
                                                if (entry.name.substringAfterLast('/') == fileName) {
                                                    file.parentFile?.mkdirs()
                                                    zip.getInputStream(entry).use { input ->
                                                        file.outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    Logging.i(TAG, "Extracted $fileName from ${entry.name}")
                                                    break
                                                }
                                            }
                                        }
                                        // Direct Maven download as last resort
                                        if (!file.isFile) {
                                            Logging.i(TAG, "Downloading artifact $lib directly from Maven")
                                            downloadFromMaven(lib, file)
                                        }
                                    }
                                    if (!file.isFile) {
                                        Logging.w(TAG, "Data artifact not found: ${file.absolutePath}")
                                    }
                                    file.absolutePath
                                }
                                // Variable {...} — resolve from vars
                                clientStr.startsWith("{") && clientStr.endsWith("}") -> {
                                    vars[clientStr.substring(1, clientStr.length - 1)] ?: clientStr
                                }
                                // Plain text — extract from ZIP if looks like a path, else use as-is
                                else -> {
                                    if (clientStr.startsWith("/") || clientStr.startsWith("\\")) {
                                        val dest = File.createTempFile("forge_mapping", ".dat", tempDir)
                                        val item = clientStr.removePrefix("\\").removePrefix("/").replace("\\", "/")
                                        try {
                                            extractEntryToFile(zip, item, dest)
                                        } catch (_: Exception) {
                                            val fileName = item.substringAfterLast('/')
                                            val zipEntries = zip.entries()
                                            var found = false
                                            while (zipEntries.hasMoreElements()) {
                                                val entry = zipEntries.nextElement()
                                                if (entry.isDirectory) continue
                                                if (entry.name.substringAfterLast('/') == fileName) {
                                                    dest.parentFile?.mkdirs()
                                                    zip.getInputStream(entry).use { input ->
                                                        dest.outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    found = true
                                                    Logging.i(TAG, "Data mapping $key extracted $fileName from ${entry.name}")
                                                    break
                                                }
                                            }
                                            if (!found) throw IllegalArgumentException("ZIP entry does not exist: $item")
                                        }
                                        dest.absolutePath
                                    } else {
                                        clientStr
                                    }
                                }
                            }
                            vars[key] = result
                            Logging.i(TAG, "Mapping $key -> $result")
                        }
                    }
                }

                vars["SIDE"] = "client"
                vars["MINECRAFT_JAR"] = vanillaJar.absolutePath
                vars["MINECRAFT_VERSION"] = mcVersion
                vars["ROOT"] = minecraftDir.absolutePath
                vars["INSTALLER"] = installerJar.absolutePath
                vars["LIBRARY_DIR"] = librariesDir.absolutePath

                // Parse processors
                val processors: List<ForgeLikeInstallProcessor> = installProfileJson.getAsJsonArray("processors")
                    ?.let { Gson().fromJson(it, object : TypeToken<List<ForgeLikeInstallProcessor>>() {}.type) }
                    ?: throw IOException("No processors found in install_profile.json")

                // Build command list with display labels
                val commandList = processors.mapNotNull { processor ->
                    val options = parseOptions(minecraftDir, processor.getArgs(), vars)
                    if (!processor.isSide("client")) return@mapNotNull null

                    val label = options["task"] ?: processor.getJar().artifactId

                    val outputs = processor.getOutputs().mapKeys { (k, _) ->
                        parseLiteral(minecraftDir, k, vars) ?: throw IOException("Invalid output key: $k")
                    }.mapValues { (_, v) ->
                        parseLiteral(minecraftDir, v, vars) ?: throw IOException("Invalid output value: $v")
                    }

                    val anyMissing = outputs.any { (key, expectedHash) ->
                        val artifact = File(minecraftDir, key)
                        if (!artifact.exists()) return@any true
                        val actualHash = calculateSha1(artifact)
                        if (actualHash != expectedHash) {
                            artifact.delete()
                            true
                        } else false
                    }

                    if (outputs.isNotEmpty() && !anyMissing) return@mapNotNull null

                    val jarLib = processor.getJar()
                    val jarPath = File(librariesDir, jarLib.toPath())

                    // If the JAR wasn't extracted from maven/, search the ZIP for it
                    if (!jarPath.isFile) {
                        val jarFileName = jarPath.name
                        val zipEntries = zip.entries()
                        var found = false
                        while (zipEntries.hasMoreElements()) {
                            val entry = zipEntries.nextElement()
                            if (entry.isDirectory) continue
                            val fileName = entry.name.substringAfterLast('/')
                            if (fileName == jarFileName) {
                                jarPath.parentFile?.mkdirs()
                                zip.getInputStream(entry).use { input ->
                                    jarPath.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                found = true
                                Logging.i(TAG, "Extracted $jarFileName from ${entry.name}")
                                break
                            }
                        }
                        if (!found) {
                            Logging.i(TAG, "$jarFileName not in ZIP, trying Maven download")
                            require(downloadFromMaven(jarLib, jarPath)) {
                                "Processor JAR not found and could not be downloaded: ${jarPath.absolutePath}"
                            }
                        }
                    }

                    val mainClass = java.util.jar.JarFile(jarPath).use {
                        it.manifest?.mainAttributes?.getValue(java.util.jar.Attributes.Name.MAIN_CLASS)
                    }?.takeIf { it.isNotBlank() }
                        ?: throw IOException("Main-Class not found in $jarPath")

                    val classpath = processor.getClasspath().map { lib ->
                        val libFile = File(librariesDir, lib.toPath())
                        if (!libFile.isFile) {
                            val libFileName = libFile.name
                            val zipEntries = zip.entries()
                            var found = false
                            while (zipEntries.hasMoreElements()) {
                                val entry = zipEntries.nextElement()
                                if (entry.isDirectory) continue
                                val fileName = entry.name.substringAfterLast('/')
                                if (fileName == libFileName) {
                                    libFile.parentFile?.mkdirs()
                                    zip.getInputStream(entry).use { input ->
                                        libFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    found = true
                                    Logging.i(TAG, "Extracted $libFileName from ${entry.name}")
                                    break
                                }
                            }
                            if (!found) {
                                Logging.i(TAG, "$libFileName not in ZIP, trying Maven download")
                                require(downloadFromMaven(lib, libFile)) {
                                    "Missing dependency: ${libFile.absolutePath}"
                                }
                            }
                        }
                        libFile.absolutePath
                    } + jarPath.absolutePath

                    val jvmArgs = buildList {
                        add("-cp")
                        add(classpath.joinToString(File.pathSeparator))
                        add(mainClass)
                        addAll(
                            processor.getArgs().map { arg ->
                                parseLiteral(minecraftDir, arg, vars)
                                    ?: throw IOException("Invalid argument: $arg")
                            }
                        )
                    }.joinToString(" ")

                    Triple(processor, jvmArgs, outputs.map { (key, value) -> File(key) to value })
                }

                // Build progress keys for each processor
                val progressLabels = commandList.mapIndexed { i, (processor, _, _) ->
                    val opts = parseOptions(minecraftDir, processor.getArgs(), vars)
                    val raw = opts["task"] ?: processor.getJar().artifactId
                    raw.replace('_', ' ')
                        .split(' ')
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                }

                // Register all steps in progress popup
                progressLabels.forEach { title ->
                    ProgressKeeper.submitProgress(title, 0, com.arata.yukarilauncher.R.string.generic_waiting)
                }

                try {
                    // Run each processor sequentially
                    Logging.i(TAG, "Running ${commandList.size} processors sequentially")
                    commandList.forEachIndexed { index, (processor, jvmArgs, outputs) ->
                        val step = index + 1
                        val progressTitle = progressLabels[index]

                        Logging.i(TAG, "Processor $step/${commandList.size}: $progressTitle")

                        ProgressKeeper.submitProgress(progressTitle, 50, com.arata.yukarilauncher.R.string.generic_waiting)

                        runProcessorJvm(context, jvmArgs, jreName)

                        // Verify output files
                        for ((artifact, expectedHash) in outputs) {
                            if (!artifact.isFile) throw FileNotFoundException("Output file missing: ${artifact.absolutePath}")

                            val actualHash = calculateSha1(artifact)
                            if (actualHash != expectedHash) {
                                artifact.delete()
                                throw IOException("SHA-1 mismatch for ${artifact.name}: expected $expectedHash, got $actualHash")
                            }
                            Logging.i(TAG, "Verified: ${artifact.name} -> $actualHash")
                        }

                        ProgressKeeper.submitProgress(progressTitle, 100, com.arata.yukarilauncher.R.string.generic_waiting)
                    }
                } finally {
                    progressLabels.forEach { title ->
                        ProgressKeeper.submitProgress(title, -1, -1)
                    }
                }

                // Apply progressIgnoreList fix for bootstraplauncher
                progressIgnoreList(targetVersionJson)

                Logging.i(TAG, "Forge installation completed successfully")
                return true

            } else {
                // --- Old format (legacy Forge) ---
                Logging.i(TAG, "Old format Forge installer detected, using legacy method")
                return installOldForge(
                    context = context,
                    zip = zip,
                    installProfileJson = installProfileJson,
                    minecraftDir = minecraftDir,
                    librariesDir = librariesDir,
                    versionsDir = versionsDir,
                    targetVersionDir = targetVersionDir,
                    targetVersionJson = targetVersionJson,
                    customVersionName = customVersionName,
                    mcVersion = mcVersion,
                    jreName = jreName
                )
            }
        }
    }

    /**
     * プロセッサーJVMを別プロセス（:installer）で実行し、完了を待つ。
     */
    private fun runProcessorJvm(context: Context, jvmArgs: String, jreName: String?) {
        val doneLatch = CountDownLatch(1)

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                doneLatch.countDown()
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter(HeadlessInstaller.ACTION_INSTALL_DONE))

        context.startActivity(
            Intent(context, InstallerActivity::class.java).apply {
                putExtra("javaArgs", jvmArgs)
                jreName?.let { putExtra(HeadlessInstaller.EXTRA_JRE_NAME, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        if (!doneLatch.await(5, TimeUnit.MINUTES)) {
            Logging.w(TAG, "Processor JVM timed out after 5 minutes")
        }

        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    private fun installOldForge(
        context: Context,
        zip: ZipFile,
        installProfileJson: com.google.gson.JsonObject,
        minecraftDir: File,
        librariesDir: File,
        versionsDir: File,
        targetVersionDir: File,
        targetVersionJson: File,
        customVersionName: String,
        mcVersion: String,
        jreName: String?
    ): Boolean {
        targetVersionDir.mkdirs()

        if (!installProfileJson.has("install")) {
            // Method A: extract JSON and maven libraries
            Logging.i(TAG, "Old Forge method A")
            val jsonPath = installProfileJson["json"].asString.trimStart('/')
            val versionJsonStr = zip.getInputStream(zip.getEntry(jsonPath)).reader().readText()
            val versionObj = JsonParser.parseString(versionJsonStr).asJsonObject
            versionObj.addProperty("id", customVersionName)
            targetVersionJson.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(versionObj))

            // Extract maven libraries
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.startsWith("maven/") && !entry.isDirectory) {
                    val relPath = name.removePrefix("maven/")
                    val dest = File(librariesDir, relPath)
                    dest.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } else {
            // Method B: extract JAR + versionInfo
            Logging.i(TAG, "Old Forge method B")
            val install = installProfileJson.getAsJsonObject("install")
            val artifact = install["path"].asString
            val jarPath = getLibraryPath(artifact)
            val jarFile = File(librariesDir, jarPath)
            jarFile.parentFile?.mkdirs()
            jarFile.delete()

            val filePath = install["filePath"].asString
            extractEntryToFile(zip, filePath, jarFile)

            val versionInfo = installProfileJson.getAsJsonObject("versionInfo")
            versionInfo.addProperty("id", customVersionName)
            if (!versionInfo.has("inheritsFrom")) {
                versionInfo.addProperty("inheritsFrom", mcVersion)
            }
            targetVersionJson.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(versionInfo))
        }

        Logging.i(TAG, "Old Forge installation completed")
        return true
    }

    private val MAVEN_REPOS = listOf(
        "https://maven.minecraftforge.net",
        "https://maven.neoforged.net/releases",
        "https://repo1.maven.org/maven2",
        "https://maven.fabricmc.net"
    )

    private fun downloadFromMaven(lib: LibraryComponents, dest: File): Boolean {
        val path = lib.toPath()
        for (repo in MAVEN_REPOS) {
            val url = "$repo/$path"
            try {
                Logging.i(TAG, "Downloading $url")
                val conn = URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                if (conn.responseCode == 200) {
                    dest.parentFile?.mkdirs()
                    conn.inputStream.use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Logging.i(TAG, "Downloaded ${dest.name}")
                    return true
                }
                Logging.w(TAG, "Download $url returned ${conn.responseCode}")
                conn.disconnect()
            } catch (_: Exception) {
                Logging.w(TAG, "Failed to download from $repo")
            }
        }
        return false
    }

    private fun downloadVanillaJar(mcVersion: String, vanillaJar: File, versionsDir: File) {
        var clientUrl: String? = null
        var clientSha1: String? = null
        val versionJsonFile = File(versionsDir, "$mcVersion/$mcVersion.json")

        if (versionJsonFile.isFile) {
            Logging.i(TAG, "Parsing local version JSON for client download URL")
            try {
                val versionObj = JsonParser.parseString(versionJsonFile.readText()).asJsonObject
                val client = versionObj.getAsJsonObject("downloads")?.getAsJsonObject("client")
                if (client != null) {
                    clientUrl = client.get("url")?.asString
                    clientSha1 = client.get("sha1")?.asString
                }
            } catch (_: Exception) {
                Logging.w(TAG, "Failed to parse local version JSON")
            }
        }

        if (clientUrl == null) {
            Logging.i(TAG, "Fetching version manifest for $mcVersion")
            try {
                val manifestStr = URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                    .readText(Charsets.UTF_8)
                val versions = JsonParser.parseString(manifestStr).asJsonObject
                    .getAsJsonArray("versions")

                for (ver in versions) {
                    val verObj = ver.asJsonObject
                    if (verObj.get("id")?.asString == mcVersion) {
                        val verJsonUrl = verObj.get("url")?.asString
                            ?: throw IOException("No URL for version $mcVersion")

                        Logging.i(TAG, "Downloading version JSON from $verJsonUrl")
                        val verJsonStr = URL(verJsonUrl).readText(Charsets.UTF_8)
                        versionJsonFile.parentFile?.mkdirs()
                        versionJsonFile.writeText(verJsonStr)

                        val client = JsonParser.parseString(verJsonStr).asJsonObject
                            .getAsJsonObject("downloads")?.getAsJsonObject("client")
                        if (client != null) {
                            clientUrl = client.get("url")?.asString
                            clientSha1 = client.get("sha1")?.asString
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                throw IOException("Failed to get client download info for $mcVersion", e)
            }
        }

        if (clientUrl == null) {
            throw IOException("Cannot find client download URL for Minecraft $mcVersion")
        }

        Logging.i(TAG, "Downloading vanilla client JAR for $mcVersion")
        vanillaJar.parentFile?.mkdirs()

        val conn = URL(clientUrl).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        if (conn.responseCode != 200) {
            throw IOException("Failed to download client JAR: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { input ->
            val digest = java.security.MessageDigest.getInstance("SHA-1")
            vanillaJar.outputStream().use { output ->
                val buf = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1) {
                    output.write(buf, 0, bytesRead)
                    digest.update(buf, 0, bytesRead)
                }
            }

            if (clientSha1 != null) {
                val actualSha1 = digest.digest().joinToString("") { "%02x".format(it) }
                if (actualSha1 != clientSha1) {
                    vanillaJar.delete()
                    throw IOException("SHA-1 mismatch for client JAR: expected $clientSha1, got $actualSha1")
                }
                Logging.i(TAG, "Client JAR SHA-1 verified: $actualSha1")
            }
        }

        Logging.i(TAG, "Downloaded vanilla client JAR: ${vanillaJar.absolutePath}")
    }

    private fun progressIgnoreList(versionJson: File) {
        try {
            val jsonObject = JsonParser.parseString(versionJson.readText()).asJsonObject

            val libraries = jsonObject.getAsJsonArray("libraries") ?: return

            val hasNewBootstrap = libraries.any { je ->
                val obj = je.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
                val name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: return@any false
                val comps = fromDescriptor(name)
                comps.groupId == "cpw.mods" && comps.artifactId == "bootstraplauncher"
            }

            if (!hasNewBootstrap) return

            val jvmArgs = jsonObject.getAsJsonObject("arguments")
                ?.getAsJsonArray("jvm") ?: return

            val ignoreListIndex = jvmArgs.indexOfLast {
                it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.startsWith("-DignoreList=")
            }.takeIf { it != -1 } ?: return

            val originalArg = jvmArgs[ignoreListIndex].asString
            jvmArgs[ignoreListIndex] = com.google.gson.Gson().toJsonTree("$originalArg,\${primary_jar_name}")

            versionJson.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(jsonObject))
        } catch (e: Exception) {
            Logging.w(TAG, "Failed to apply progressIgnoreList fix", e)
        }
    }
}
