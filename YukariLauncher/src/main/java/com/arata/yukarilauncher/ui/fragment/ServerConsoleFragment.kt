package com.arata.yukarilauncher.ui.fragment

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StatFs
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentServerConsoleBinding
import com.arata.yukarilauncher.feature.network.HostServerService
import com.arata.yukarilauncher.feature.network.McServerConfig
import com.arata.yukarilauncher.feature.network.McServerManager
import com.arata.yukarilauncher.feature.network.McServerType
import com.arata.yukarilauncher.task.TaskExecutors
import java.io.BufferedReader
import java.io.FileReader

class ServerConsoleFragment : FragmentWithAnim(R.layout.fragment_server_console) {

    companion object {
        const val TAG = "ServerConsoleFragment"
        private const val MAX_LOG_LINES = 800
        private const val STATS_INTERVAL_MS = 2000L
    }

    private lateinit var binding: FragmentServerConsoleBinding
    private lateinit var serverManager: McServerManager
    private var selectedType: McServerType = McServerType.PAPER

    // Tunnel service (for tunnel address lines in log)
    private var tunnelService: HostServerService? = null
    private var tunnelBound = false
    private val tunnelConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            tunnelService = (binder as HostServerService.LocalBinder).getService()
            tunnelBound = true
            observeTunnel()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            tunnelService = null; tunnelBound = false
        }
    }

    // Log
    private val logLines = ArrayDeque<String>()

    // CPU
    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    // Stats
    private val statsHandler = Handler(Looper.getMainLooper())
    private val statsRunnable = object : Runnable {
        override fun run() {
            if (isAdded) { updateStats(); statsHandler.postDelayed(this, STATS_INTERVAL_MS) }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentServerConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serverManager = McServerManager(requireContext())
        wireCallbacks()
        wireUI()

        readCpuCounters()?.let { (i, t) -> lastCpuIdle = i; lastCpuTotal = t }
        statsHandler.postDelayed(statsRunnable, STATS_INTERVAL_MS)

        Intent(requireContext(), HostServerService::class.java).also {
            requireContext().bindService(it, tunnelConnection, Context.BIND_AUTO_CREATE)
        }

        setTab(McServerType.VANILLA)
        sysLog("[INFO] Console ready — configure and press START.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statsHandler.removeCallbacks(statsRunnable)
        if (tunnelBound) { requireContext().unbindService(tunnelConnection); tunnelBound = false }
    }

    // ── UI wiring ─────────────────────────────────────────────────────────────

    private fun wireUI() {
        binding.consoleBackButton.setOnClickListener { forceBack() }
        binding.consoleClearButton.setOnClickListener { clearLog() }

        // Type tabs
        binding.consoleTabVanilla.setOnClickListener { setTab(McServerType.VANILLA) }
        binding.consoleTabPaper.setOnClickListener   { setTab(McServerType.PAPER)   }
        binding.consoleTabFabric.setOnClickListener  { setTab(McServerType.FABRIC)  }

        // Fetch versions button
        binding.consoleVersionFetchBtn.setOnClickListener { fetchVersions() }

        // Control buttons
        binding.consoleStartButton.setOnClickListener { startServer() }
        binding.consoleRestartButton.setOnClickListener { restartServer() }
        binding.consoleStopButton.setOnClickListener { stopServer() }

        // Command input — send on keyboard action or button tap
        binding.consoleCommandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendCommand()
                true
            } else false
        }
        binding.consoleSendButton.setOnClickListener { sendCommand() }
    }

    // ── Tab selection ─────────────────────────────────────────────────────────

    private fun setTab(type: McServerType) {
        selectedType = type

        // Reset all tabs to unselected look
        listOf(binding.consoleTabVanilla, binding.consoleTabPaper, binding.consoleTabFabric)
            .forEach {
                it.setBackgroundResource(android.R.color.transparent)
                it.setTextColor(0xFFAAAAAA.toInt())
                it.typeface = android.graphics.Typeface.DEFAULT
            }

        // Highlight selected
        val active = when (type) {
            McServerType.VANILLA -> binding.consoleTabVanilla
            McServerType.PAPER   -> binding.consoleTabPaper
            McServerType.FABRIC  -> binding.consoleTabFabric
        }
        active.setBackgroundResource(R.drawable.console_tab_selected)
        active.setTextColor(0xFF8D42EB.toInt())
        active.typeface = android.graphics.Typeface.DEFAULT_BOLD

        // Clear version list when switching type
        binding.consoleVersionListContainer.removeAllViews()
        binding.consoleVersionListContainer.visibility = View.GONE
    }

    // ── Version fetch ─────────────────────────────────────────────────────────

    private fun fetchVersions() {
        binding.consoleVersionFetchBtn.text = getString(R.string.console_fetching)
        binding.consoleVersionFetchBtn.isEnabled = false
        binding.consoleVersionListContainer.removeAllViews()

        TaskExecutors.getDefault().execute {
            val versions: List<String> = try {
                when (selectedType) {
                    McServerType.VANILLA -> fetchVanillaVersions()
                    McServerType.PAPER   -> fetchPaperVersions()
                    McServerType.FABRIC  -> fetchFabricVersions()
                }
            } catch (e: Exception) {
                sysLog("[ERROR] Failed to fetch versions: ${e.message}")
                emptyList()
            }

            TaskExecutors.runInUIThread {
                if (!isAdded) return@runInUIThread
                binding.consoleVersionFetchBtn.text = getString(R.string.console_fetch_versions)
                binding.consoleVersionFetchBtn.isEnabled = true

                if (versions.isEmpty()) return@runInUIThread

                binding.consoleVersionListContainer.removeAllViews()
                // Show max 12 versions as tappable chips
                versions.take(12).forEach { ver ->
                    val chip = TextView(requireContext()).apply {
                        text = ver
                        textSize = 9f
                        setTextColor(0xFFBBBBCC.toInt())
                        typeface = android.graphics.Typeface.MONOSPACE
                        setPadding(
                            resources.getDimensionPixelSize(R.dimen._6sdp), 0,
                            resources.getDimensionPixelSize(R.dimen._6sdp), 0
                        )
                        setBackgroundResource(R.drawable.background_item)
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            resources.getDimensionPixelSize(R.dimen._28sdp)
                        ).apply {
                            bottomMargin = resources.getDimensionPixelSize(R.dimen._3sdp)
                        }
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setOnClickListener {
                            binding.consoleVersionInput.setText(ver)
                            binding.consoleVersionListContainer.visibility = View.GONE
                        }
                    }
                    binding.consoleVersionListContainer.addView(chip)
                }
                binding.consoleVersionListContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchVanillaVersions(): List<String> {
        val json = java.net.URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").readText()
        val arr = org.json.JSONObject(json).getJSONArray("versions")
        return (0 until minOf(arr.length(), 30))
            .map { arr.getJSONObject(it) }
            .filter { it.getString("type") == "release" }
            .map { it.getString("id") }
    }

    private fun fetchPaperVersions(): List<String> {
        val json = java.net.URL("https://api.papermc.io/v2/projects/paper").readText()
        val arr = org.json.JSONObject(json).getJSONArray("versions")
        return (arr.length() - 1 downTo maxOf(0, arr.length() - 20))
            .map { arr.getString(it) }
    }

    private fun fetchFabricVersions(): List<String> {
        val json = java.net.URL("https://meta.fabricmc.net/v2/versions/game").readText()
        val arr = org.json.JSONArray(json)
        return (0 until minOf(arr.length(), 20))
            .map { arr.getJSONObject(it) }
            .filter { it.getBoolean("stable") }
            .map { it.getString("version") }
    }

    // ── Server control ────────────────────────────────────────────────────────

    private fun startServer() {
        val config = buildConfig() ?: return
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Starting ${config.type.displayName} ${config.version} on port ${config.port}…")
        serverManager.start(config)
    }

    private fun restartServer() {
        val config = buildConfig() ?: return
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Restarting…")
        serverManager.restart(config)
    }
    
    private fun stopServer() {
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Stopping server…")
        serverManager.stop()
    }

    private fun buildConfig(): McServerConfig? {
        val version = binding.consoleVersionInput.text.toString().trim()
        if (version.isBlank()) { sysLog("[ERROR] Enter a Minecraft version."); return null }
        val port = binding.consolePortInput.text.toString().toIntOrNull() ?: 25565
        val jvm  = binding.consoleJvmArgsInput.text.toString().trim().ifBlank { "-Xmx1G -Xms512M" }
        return McServerConfig(type = selectedType, version = version, jvmArgs = jvm, port = port)
    }

    // ── Command input ─────────────────────────────────────────────────────────

    private fun sendCommand() {
        val cmd = binding.consoleCommandInput.text.toString().trim()
        if (cmd.isBlank()) return
        if (!serverManager.isRunning()) {
            sysLog("[WARN] Server is not running.")
            return
        }
        serverManager.sendCommand(cmd)
        binding.consoleCommandInput.setText("")
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.consoleCommandInput.windowToken, 0)
    }

    // ── Button states ─────────────────────────────────────────────────────────

    private fun setButtonState(running: Boolean, transitioning: Boolean = false) {
        requireActivity().runOnUiThread {
            if (!isAdded) return@runOnUiThread
            val busy = transitioning
            binding.consoleStartButton.isEnabled   = !running && !busy
            binding.consoleRestartButton.isEnabled =  running && !busy
            binding.consoleStopButton.isEnabled    =  running && !busy
            // Lock setup fields while running
            binding.consoleVersionInput.isEnabled  = !running
            binding.consolePortInput.isEnabled     = !running
            binding.consoleJvmArgsInput.isEnabled  = !running
            binding.consoleTabVanilla.isEnabled    = !running
            binding.consoleTabPaper.isEnabled      = !running
            binding.consoleTabFabric.isEnabled     = !running

            // Status dot
            when {
                running      -> {
                    binding.consoleStatusDot.setBackgroundResource(R.drawable.bg_status_dot_running)
                    binding.consoleStatusLabel.text = getString(R.string.console_status_running)
                }
                transitioning -> {
                    binding.consoleStatusDot.setBackgroundResource(R.drawable.bg_status_dot_connecting)
                    binding.consoleStatusLabel.text = getString(R.string.console_status_connecting)
                }
                else -> {
                    binding.consoleStatusDot.setBackgroundResource(R.drawable.bg_status_dot_idle)
                    binding.consoleStatusLabel.text = getString(R.string.console_status_idle)
                }
            }
        }
    }

    // ── McServerManager callbacks ─────────────────────────────────────────────

    private fun wireCallbacks() {
        serverManager.onLog     = { line -> appendLine(line) }
        serverManager.onStarted = { setButtonState(running = true) }
        serverManager.onStopped = {
            setButtonState(running = false)
            sysLog("[INFO] Server stopped.")
            requireActivity().runOnUiThread {
                if (isAdded) binding.consoleProgressBarLayout.visibility = View.GONE
            }
        }
        serverManager.onError   = { msg -> sysLog("[ERROR] $msg"); setButtonState(running = false) }
        serverManager.onProgress = { pct, label ->
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (pct < 0) {
                    binding.consoleProgressBarLayout.visibility = View.GONE
                } else {
                    binding.consoleProgressBarLayout.visibility = View.VISIBLE
                    binding.consoleProgressLabel.text = label
                    binding.consoleDownloadProgress.progress = pct
                    binding.consoleProgressPercent.text = "$pct%"
                }
            }
        }
    }

    // ── Tunnel observation ────────────────────────────────────────────────────

    private fun observeTunnel() {
        tunnelService?.tunnelAddress?.observe(viewLifecycleOwner) { addr ->
            if (!addr.isNullOrEmpty()) sysLog("[TUNNEL] Public: $addr")
        }
        tunnelService?.authUrl?.observe(viewLifecycleOwner) { url ->
            if (url != null) sysLog("[TUNNEL] Claim: $url")
        }
    }

    // ── Log helpers ───────────────────────────────────────────────────────────

    private fun appendLine(raw: String) {
        if (!isAdded) return
        requireActivity().runOnUiThread {
            if (!isAdded) return@runOnUiThread
            logLines.addLast(tag(raw))
            if (logLines.size > MAX_LOG_LINES) logLines.removeFirst()
            binding.consoleLogText.text = logLines.joinToString("\n")
            binding.consoleLogScroll.post {
                if (isAdded) binding.consoleLogScroll.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun sysLog(msg: String) = appendLine(msg)

    private fun clearLog() {
        logLines.clear()
        binding.consoleLogText.text = ""
    }

    private fun tag(line: String) = when {
        line.contains("ERROR", true) || line.contains("Exception") ||
        line.contains("FATAL", true)  -> "✗ $line"
        line.contains("WARN", true)   -> "⚠ $line"
        line.startsWith(">")          -> line           // echoed command
        line.contains("[TUNNEL]") || line.contains("[INSTALL]") -> "► $line"
        else                          -> "  $line"
    }

    // ── System stats ──────────────────────────────────────────────────────────

    private fun updateStats() {
        if (!isAdded) return
        // RAM
        val am   = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi   = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val tot  = mi.totalMem / (1024 * 1024)
        val used = tot - mi.availMem / (1024 * 1024)
        binding.consoleRamText.text    = "${fmt(used)}/${fmt(tot)}"
        binding.consoleRamBar.progress = if (tot > 0) (used * 100 / tot).toInt() else 0
        // CPU
        readCpuCounters()?.let { (idle, total) ->
            val dt = total - lastCpuTotal; val di = idle - lastCpuIdle
            lastCpuIdle = idle; lastCpuTotal = total
            if (dt > 0) {
                val pct = ((dt - di) * 100 / dt).toInt().coerceIn(0, 100)
                binding.consoleCpuText.text    = "$pct%"
                binding.consoleCpuBar.progress = pct
            }
        }
        // Disk
        try {
            val st   = StatFs(Environment.getDataDirectory().path)
            val dtot = st.totalBytes / (1024 * 1024)
            val duse = dtot - st.availableBytes / (1024 * 1024)
            binding.consoleStorageText.text    = "${fmt(duse)}/${fmt(dtot)}"
            binding.consoleStorageBar.progress = if (dtot > 0) (duse * 100 / dtot).toInt() else 0
        } catch (_: Exception) {}
    }

    private fun readCpuCounters(): Pair<Long, Long>? {
        return try {
            val line  = BufferedReader(FileReader("/proc/stat")).readLine()?.trim() ?: return null
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 5) return null
            val idle  = parts[4].toLong()
            val total = parts.drop(1).sumOf { it.toLongOrNull() ?: 0L }
            Pair(idle, total)
        } catch (_: Exception) { null }
    }

    private fun fmt(mb: Long) = if (mb >= 1024) "${"%.1f".format(mb / 1024.0)}G" else "${mb}M"

    // ── Animations ────────────────────────────────────────────────────────────

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.consoleRoot, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.consoleRoot, Animations.FadeOutUp))
    }
}