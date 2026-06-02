package com.arata.yukarilauncher.feature.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.arata.yukarilauncher.utils.path.PathManager
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class PlayitManager(private val context: Context) {

    companion object {
        private const val TAG = "PlayitManager"
        private const val PREFS_NAME = "playit_prefs"
        private const val KEY_TUNNEL_ADDRESS = "tunnel_address"
        private const val RELEASE_BASE_URL = "https://github.com/playit-cloud/playit-agent/releases/download/v0.17.1"
    }

    private data class PlayitBinary(val androidAbi: String, val fileName: String)

    private val playitBinary: PlayitBinary by lazy { resolvePlayitBinary() }

    private var process: Process? = null
    private val isRunning = AtomicBoolean(false)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var cachedAddress: String?
        get() = prefs.getString(KEY_TUNNEL_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_TUNNEL_ADDRESS, value).apply()

    var onLog: ((String) -> Unit)? = null
    var onAuthRequired: ((String) -> Unit)? = null
    var onStopped: (() -> Unit)? = null
    var onTunnelReady: ((String) -> Unit)? = null
    var onTunnelRunning: (() -> Unit)? = null
    var onDownloadProgress: ((Int) -> Unit)? = null  // percent 0-100

    /**
     * Prepares the binary (downloads if missing) and starts the tunnel.
     */
/**
 * startする
 */
    fun start(localPort: Int = 25565) {
        if (isRunning.get()) {
            Log.w(TAG, "Already running")
            return
        }

        scope.launch {
            try {
                val binary = ensureBinary()
                startProcess(binary, localPort)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start playit", e)
                onLog?.invoke("Error: ${e.message}")
                onStopped?.invoke()
            }
        }
    }

    /**
     * Ensures the playit binary exists. Downloads if necessary.
     */
/**
 * ensureBinaryする
 */
    private suspend fun ensureBinary(): File = withContext(Dispatchers.IO) {
        val file = File(PathManager.DIR_DATA, playitBinary.fileName)
        if (file.exists() && file.canExecute()) {
            Log.d(TAG, "Binary already exists: ${file.absolutePath}")
            return@withContext file
        }

        val downloadUrl = "$RELEASE_BASE_URL/${playitBinary.fileName}"
        Log.d(TAG, "Binary not found for ABI ${playitBinary.androidAbi}. Downloading from $downloadUrl")
        onLog?.invoke("Downloading playit-agent...")
        downloadFile(downloadUrl, file)

        if (!file.setExecutable(true)) {
            throw RuntimeException("Failed to make binary executable")
        }
        Log.d(TAG, "Download complete: ${file.absolutePath}")
        file
    }

    /**
     * Downloads a file with progress tracking.
     */
/**
 * downloadFileする
 */
    private suspend fun downloadFile(urlString: String, destination: File) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error: ${connection.responseCode}")
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destination)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val progress = (totalBytesRead * 100 / fileLength).toInt()
                    onDownloadProgress?.invoke(progress)
                }
            }
            output.close()
            input.close()
        } catch (e: Exception) {
            destination.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Launches the playit process.
     */
/**
 * startProcessする
 */
    private fun startProcess(binary: File, localPort: Int) {
        val dir = File(PathManager.DIR_DATA)

        process = ProcessBuilder(binary.absolutePath, "--stdout")
            .directory(dir)
            .apply {
                environment()["TERM"] = "dumb"
            }
            .start()

        process!!.outputStream.close()
        isRunning.set(true)

        Thread { readStream(process!!.inputStream, "STDOUT") }.apply {
            name = "PlayitStdoutReader"
            isDaemon = true
            start()
        }
        Thread { readStream(process!!.errorStream, "STDERR") }.apply {
            name = "PlayitStderrReader"
            isDaemon = true
            start()
        }
        Thread {
            val exitCode = process!!.waitFor()
            Log.d(TAG, "Process exited with code $exitCode")
            isRunning.set(false)
            onStopped?.invoke()
        }.apply {
            name = "PlayitExitWatcher"
            isDaemon = true
            start()
        }
    }

/**
 * readStreamする
 */
    private fun readStream(inputStream: InputStream, label: String) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null) {
                line?.let { processLine(it, label) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$label] read error", e)
        } finally {
            if (label == "STDOUT") {
                isRunning.set(false)
                onStopped?.invoke()
                Log.d(TAG, "Playit stopped")
            }
        }
    }

/**
 * processLineする
 */
    private fun processLine(line: String, label: String) {
        Log.d("PLAYIT_RAW", "[$label] $line")
        // Claim URL (do not print to in-app console log)
        val claimUrl = Regex("https://playit\\.gg/claim/\\S+").find(line)?.value
        if (claimUrl != null) {
            onAuthRequired?.invoke(claimUrl)
        } else {
            onLog?.invoke(line)
        }

        // Tunnel address patterns
        val addressPatterns = listOf(
            Regex("([a-zA-Z0-9.-]+\\.gl\\.joinmc\\.link:\\d+)"),
            Regex("([a-zA-Z0-9.-]+\\.playit\\.gg:\\d+)"),
            Regex("([a-zA-Z0-9.-]+\\.[a-zA-Z0-9.-]+:\\d+)")
        )

        val matchedAddress = addressPatterns
            .asSequence()
            .mapNotNull { pattern -> pattern.find(line)?.groupValues?.get(1) }
            .firstOrNull()

        matchedAddress?.let { address ->
            cachedAddress = address
            onTunnelReady?.invoke(address)
        }

        // Tunnel running event
        if (line.contains("tunnel running", ignoreCase = true)) {
            onTunnelRunning?.invoke()
            cachedAddress?.let { onTunnelReady?.invoke(it) }
        }
    }

/**
 * stopする
 */
    fun stop() {
        try {
            Log.d(TAG, "Stopping playit...")
            process?.destroy()
            scope.launch {
                delay(2000)
                if (process?.isAlive == true) {
                    Log.w(TAG, "Process still alive, force destroying")
                    process?.destroyForcibly()
                }
                process = null
                isRunning.set(false)
                onStopped?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playit", e)
            process = null
            isRunning.set(false)
        }
    }

/**
 * isRunningする
 */
    fun isRunning(): Boolean = isRunning.get()

/**
 * resolvePlayitBinaryする
 */
    private fun resolvePlayitBinary(): PlayitBinary {
        val supportedAbis = Build.SUPPORTED_ABIS
        for (abi in supportedAbis) {
            when (abi) {
                "arm64-v8a" -> return PlayitBinary(abi, "playit-linux-aarch64")
                "armeabi-v7a", "armeabi" -> return PlayitBinary(abi, "playit-linux-armv7")
                "x86" -> return PlayitBinary(abi, "playit-linux-i686")
                "x86_64" -> return PlayitBinary(abi, "playit-linux-amd64")
            }
        }

        throw UnsupportedOperationException(
            "Unsupported ABI(s): ${supportedAbis.joinToString()}. Supported: arm64-v8a, armeabi-v7a, x86, x86_64"
        )
    }
}