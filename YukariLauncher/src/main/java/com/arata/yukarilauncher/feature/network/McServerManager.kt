package com.arata.yukarilauncher.feature.network

import android.content.Context
import android.util.Log
import com.arata.yukarilauncher.utils.path.PathManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class McServerManager(private val context: Context) {

    companion object {
        private const val TAG = "McServerManager"
        private const val MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        private const val PAPER_API = "https://api.papermc.io/v2/projects/paper"
        private const val FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader"
        private const val FABRIC_INSTALLER = "https://meta.fabricmc.net/v2/versions/installer"

/**
 * SERVER_DIRする
 */
        val SERVER_DIR get() = File(PathManager.DIR_GAME_HOME, "server")
    }

    private var process: Process? = null
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val nativeLibDir: String
        get() = context.applicationInfo.nativeLibraryDir

    var onLog: ((String) -> Unit)? = null
    var onStarted: (() -> Unit)? = null
    var onStopped: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onProgress: ((Int, String) -> Unit)? = null

/**
 * startする
 */
    fun start(config: McServerConfig) {
        if (running.get()) {
            log("[WARN] Server is already running")
            return
        }
        scope.launch {
            try {
                prepare(config)
                launchProcess(config)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
                onError?.invoke(e.message ?: "Unknown error")
                onStopped?.invoke()
            }
        }
    }

/**
 * stopする
 */
    fun stop() {
        scope.launch {
            try {
                log("[INFO] Stopping server…")
                process?.outputStream?.let {
                    it.write("stop\n".toByteArray())
                    it.flush()
                }
                delay(3000)
                process?.destroy()
            } catch (e: Exception) {
                process?.destroy()
            } finally {
                process = null
                running.set(false)
                onStopped?.invoke()
            }
        }
    }

/**
 * restartする
 */
    fun restart(config: McServerConfig) {
        scope.launch {
            stop()
            delay(1500)
            start(config)
        }
    }

/**
 * sendCommandする
 */
    fun sendCommand(cmd: String) {
        try {
            process?.outputStream?.let {
                it.write("$cmd\n".toByteArray())
                it.flush()
                onLog?.invoke("> $cmd")
            } ?: onLog?.invoke("[WARN] Server is not running")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: $cmd", e)
        }
    }

/**
 * isRunningする
 */
    fun isRunning() = running.get()

/**
 * prepareする
 */
    private suspend fun prepare(config: McServerConfig) {
        SERVER_DIR.mkdirs()
        val jar = serverJarFile(config)
        if (!jar.exists()) {
            log("[INSTALL] Downloading ${config.type.displayName} ${config.version}…")
            when (config.type) {
                McServerType.VANILLA -> downloadVanilla(config.version, jar)
                McServerType.PAPER   -> downloadPaper(config.version, jar)
                McServerType.FABRIC  -> downloadFabric(config.version, jar)
            }
        } else {
            log("[INFO] Server jar found: ${jar.name}")
        }
        val eula = File(SERVER_DIR, "eula.txt")
        if (!eula.exists() || !eula.readText().contains("eula=true")) {
            log("[INFO] Accepting EULA…")
            eula.writeText("eula=true\n")
        }
    }

/**
 * serverJarFileする
 */
    private fun serverJarFile(config: McServerConfig) =
        File(SERVER_DIR, "server-${config.type.name.lowercase()}-${config.version}.jar")

/**
 * downloadVanillaする
 */
    private suspend fun downloadVanilla(version: String, dest: File) {
        log("[INSTALL] Fetching Vanilla manifest…")
        val manifest = fetchJson(MANIFEST_URL)
        val versions = manifest.getJSONArray("versions")
        var versionUrl: String? = null
        for (i in 0 until versions.length()) {
            val v = versions.getJSONObject(i)
            if (v.getString("id") == version) {
                versionUrl = v.getString("url")
                break
            }
        }
        requireNotNull(versionUrl) { "Vanilla version $version not found in manifest" }
        log("[INSTALL] Fetching version metadata…")
        val versionMeta = fetchJson(versionUrl)
        val serverUrl = versionMeta.getJSONObject("downloads").getJSONObject("server").getString("url")
        log("[INSTALL] Downloading Vanilla $version server jar…")
        downloadFile(serverUrl, dest, label = "Vanilla $version")
    }

/**
 * downloadPaperする
 */
    private suspend fun downloadPaper(version: String, dest: File) {
        log("[INSTALL] Fetching Paper builds for $version…")
        val buildsJson = fetchJson("$PAPER_API/versions/$version/builds")
        val builds = buildsJson.getJSONArray("builds")
        val latest = builds.getJSONObject(builds.length() - 1)
        val build = latest.getInt("build")
        val jarName = latest.getJSONObject("downloads").getJSONObject("application").getString("name")
        val url = "$PAPER_API/versions/$version/builds/$build/downloads/$jarName"
        log("[INSTALL] Downloading Paper $version build $build…")
        downloadFile(url, dest, label = "Paper $version")
    }

/**
 * downloadFabricする
 */
    private suspend fun downloadFabric(version: String, dest: File) {
        log("[INSTALL] Fetching Fabric loader versions…")
        val loaders = fetchJsonArray(FABRIC_META)
        val loaderVer = loaders.getJSONObject(0).getString("version")
        log("[INSTALL] Fetching Fabric installer versions…")
        val installers = fetchJsonArray(FABRIC_INSTALLER)
        val installerVer = installers.getJSONObject(0).getString("version")
        val url = "https://meta.fabricmc.net/v2/versions/loader/$version/$loaderVer/$installerVer/server/jar"
        log("[INSTALL] Downloading Fabric $version (loader $loaderVer)…")
        downloadFile(url, dest, label = "Fabric $version")
    }

/**
 * launchProcessする
 */
    private fun launchProcess(config: McServerConfig) {
        val jar = serverJarFile(config)
        val (javaPath, jreLibDir) = resolveJavaBinary(config.javaVersion)

        log("[INFO] Java: $javaPath")
        log("[INFO] JRE library dir: ${jreLibDir ?: "none"}")
        log("[INFO] JVM args: ${config.jvmArgs}")
        log("[INFO] Starting server on port ${config.port}…")

        val jvmArgList = config.jvmArgs.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val cmd = mutableListOf(javaPath) + jvmArgList + listOf("-jar", jar.absolutePath, "--nogui", "--port", config.port.toString())
        log("[INFO] Command: ${cmd.joinToString(" ")}")

        val processBuilder = ProcessBuilder(cmd)
            .directory(SERVER_DIR)
            .redirectErrorStream(true)

        val env = processBuilder.environment()
        // Build LD_LIBRARY_PATH: JRE lib dir (if any) + app native lib dir + existing
        val libPaths = mutableListOf<String>()
        if (jreLibDir != null && File(jreLibDir).exists()) {
            libPaths.add(jreLibDir)
        }
        libPaths.add(nativeLibDir)
        val currentLdPath = env["LD_LIBRARY_PATH"]
        if (currentLdPath != null && currentLdPath.isNotEmpty()) {
            libPaths.add(currentLdPath)
        }
        val newLdPath = libPaths.joinToString(":")
        env["LD_LIBRARY_PATH"] = newLdPath
        log("[INFO] Set LD_LIBRARY_PATH = $newLdPath")

        process = processBuilder.start()
        running.set(true)
        onStarted?.invoke()

        val reader = BufferedReader(InputStreamReader(process!!.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                log(line!!)
            }
        } catch (_: Exception) {}

        val exitCode = process!!.waitFor()
        log("[INFO] Server process exited (code $exitCode)")
        running.set(false)
        process = null
        onStopped?.invoke()
    }

/**
 * resolveJavaBinaryする
 */
    private fun resolveJavaBinary(preferredMajor: Int): Pair<String, String?> {
        val runtimesDir = File(PathManager.DIR_MULTIRT_HOME)
        log("[DEBUG] Looking for runtimes in: ${runtimesDir.absolutePath}")

        if (!runtimesDir.exists()) {
            log("[WARN] Runtimes directory does not exist: ${runtimesDir.absolutePath}")
            return "java" to null
        }

/**
 * getJreLibDirする
 */
        fun getJreLibDir(javaBinPath: String): String? {
            val binDir = File(javaBinPath).parentFile ?: return null
            val jreRoot = binDir.parentFile ?: return null
            val libDir = File(jreRoot, "lib")
            return if (libDir.exists() && libDir.isDirectory) libDir.absolutePath else null
        }

        val java25 = File(runtimesDir, "Internal-25/bin/java")
        if (java25.exists()) {
            ensureExecutable(java25)
            if (java25.canExecute()) {
                log("[INFO] Using explicitly requested Java 25 at ${java25.absolutePath}")
                val libDir = getJreLibDir(java25.absolutePath)
                return java25.absolutePath to libDir
            }
        }

        val candidates = runtimesDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("Internal-") }
            ?.mapNotNull { dir ->
                val bin = File(dir, "bin/java")
                if (bin.exists()) {
                    ensureExecutable(bin)
                    if (bin.canExecute()) {
                        val version = dir.name.substringAfter("Internal-").toIntOrNull() ?: 0
                        version to bin.absolutePath
                    } else null
                } else null
            }
            ?.sortedByDescending { it.first }
            ?: emptyList()

        if (candidates.isNotEmpty()) {
            val exact = candidates.firstOrNull { it.first == preferredMajor }
            val higher = candidates.filter { it.first >= preferredMajor }.minByOrNull { it.first }
            val any = candidates.firstOrNull()
            val chosen = exact ?: higher ?: any
            if (chosen != null) {
                log("[INFO] Using bundled Java ${chosen.first} at ${chosen.second}")
                val libDir = getJreLibDir(chosen.second)
                return chosen.second to libDir
            }
        }

        log("[WARN] No usable bundled JRE found, falling back to system java")
        return "java" to null
    }

/**
 * ensureExecutableする
 */
    private fun ensureExecutable(file: File) {
        if (!file.exists()) return
        if (file.canExecute()) return
        val success = file.setExecutable(true, false)
        if (success) {
            log("[INFO] Set executable permission on ${file.absolutePath}")
        } else {
            log("[WARN] Failed to set executable permission on ${file.absolutePath}")
        }
        val parent = file.parentFile
        if (parent != null && !parent.canExecute()) {
            parent.setExecutable(true, false)
            log("[INFO] Set executable permission on directory ${parent.absolutePath}")
        }
    }

/**
 * logする
 */
    private fun log(line: String) {
        Log.d(TAG, line)
        onLog?.invoke(line)
    }

/**
 * fetchJsonする
 */
    private suspend fun fetchJson(urlStr: String): JSONObject =
        withContext(Dispatchers.IO) { JSONObject(URL(urlStr).readText()) }

/**
 * fetchJsonArrayする
 */
    private suspend fun fetchJsonArray(urlStr: String) =
        withContext(Dispatchers.IO) { org.json.JSONArray(URL(urlStr).readText()) }

/**
 * downloadFileする
 */
    private suspend fun downloadFile(urlStr: String, dest: File, label: String) =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.connect()
                if (conn.responseCode != HttpURLConnection.HTTP_OK)
                    throw IOException("HTTP ${conn.responseCode} for $urlStr")
                val total = conn.contentLength.toLong()
                var loaded = 0L
                val buf = ByteArray(8192)
                BufferedOutputStream(FileOutputStream(dest)).use { out ->
                    conn.inputStream.use { ins ->
                        var n: Int
                        while (ins.read(buf).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            loaded += n
                            if (total > 0) {
                                val pct = (loaded * 100 / total).toInt()
                                onProgress?.invoke(pct, label)
                            }
                        }
                    }
                }
                onProgress?.invoke(-1, label)
                log("[INSTALL] $label download complete (${loaded / 1024} KB)")
            } catch (e: Exception) {
                dest.delete()
                throw e
            } finally {
                conn?.disconnect()
            }
        }
}