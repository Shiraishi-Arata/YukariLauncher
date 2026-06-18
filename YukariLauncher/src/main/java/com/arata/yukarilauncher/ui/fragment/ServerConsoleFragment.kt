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

/**
 * サーバーコンソールフラグメント
 */
class ServerConsoleFragment : FragmentWithAnim(R.layout.fragment_server_console) {

    companion object {
        const val TAG = "ServerConsoleFragment"
        private const val MAX_LOG_LINES = 800
        private const val STATS_INTERVAL_MS = 2000L
    }

    private lateinit var binding: FragmentServerConsoleBinding
    private lateinit var serverManager: McServerManager
    private var selectedType: McServerType = McServerType.PAPER

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

    private val logLines = ArrayDeque<String>()

    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    private val statsHandler = Handler(Looper.getMainLooper())
    private val statsRunnable = object : Runnable {
        override fun run() {
            if (isAdded) { updateStats(); statsHandler.postDelayed(this, STATS_INTERVAL_MS) }
        }
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentServerConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
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

    /**
     * ビュー破棄時にハンドラーコールバックとサービス接続をクリーンアップします。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        statsHandler.removeCallbacks(statsRunnable)
        if (tunnelBound) { requireContext().unbindService(tunnelConnection); tunnelBound = false }
    }

    /**
     * UIコールバックを設定する
     */
    private fun wireUI() {
        binding.consoleBackButton.setOnClickListener { forceBack() }
        binding.consoleClearButton.setOnClickListener { clearLog() }

        binding.consoleTabVanilla.setOnClickListener { setTab(McServerType.VANILLA) }
        binding.consoleTabPaper.setOnClickListener   { setTab(McServerType.PAPER)   }
        binding.consoleTabFabric.setOnClickListener  { setTab(McServerType.FABRIC)  }

        binding.consoleVersionFetchBtn.setOnClickListener { fetchVersions() }

        binding.consoleStartButton.setOnClickListener { startServer() }
        binding.consoleRestartButton.setOnClickListener { restartServer() }
        binding.consoleStopButton.setOnClickListener { stopServer() }

        binding.consoleCommandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendCommand()
                true
            } else false
        }
        binding.consoleSendButton.setOnClickListener { sendCommand() }
    }

    /**
     * サーバー種別タブを設定する
     */
    private fun setTab(type: McServerType) {
        selectedType = type

        listOf(binding.consoleTabVanilla, binding.consoleTabPaper, binding.consoleTabFabric)
            .forEach {
                it.setBackgroundResource(android.R.color.transparent)
                it.setTextColor(0xFFAAAAAA.toInt())
                it.typeface = android.graphics.Typeface.DEFAULT
            }

        val active = when (type) {
            McServerType.VANILLA -> binding.consoleTabVanilla
            McServerType.PAPER   -> binding.consoleTabPaper
            McServerType.FABRIC  -> binding.consoleTabFabric
        }
        active.setBackgroundResource(R.drawable.console_tab_selected)
        active.setTextColor(0xFF8D42EB.toInt())
        active.typeface = android.graphics.Typeface.DEFAULT_BOLD

        binding.consoleVersionListContainer.removeAllViews()
        binding.consoleVersionListContainer.visibility = View.GONE
    }

    /**
     * バージョン一覧を取得する
     */
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

    /**
     * Vanillaバージョン一覧を取得する
     */
    private fun fetchVanillaVersions(): List<String> {
        val json = java.net.URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").readText()
        val arr = org.json.JSONObject(json).getJSONArray("versions")
        return (0 until minOf(arr.length(), 30))
            .map { arr.getJSONObject(it) }
            .filter { it.getString("type") == "release" }
            .map { it.getString("id") }
    }

    /**
     * Paperバージョン一覧を取得する
     */
    private fun fetchPaperVersions(): List<String> {
        val json = java.net.URL("https://api.papermc.io/v2/projects/paper").readText()
        val arr = org.json.JSONObject(json).getJSONArray("versions")
        return (arr.length() - 1 downTo maxOf(0, arr.length() - 20))
            .map { arr.getString(it) }
    }

    /**
     * Fabricバージョン一覧を取得する
     */
    private fun fetchFabricVersions(): List<String> {
        val json = java.net.URL("https://meta.fabricmc.net/v2/versions/game").readText()
        val arr = org.json.JSONArray(json)
        return (0 until minOf(arr.length(), 20))
            .map { arr.getJSONObject(it) }
            .filter { it.getBoolean("stable") }
            .map { it.getString("version") }
    }

    /**
     * サーバーを起動する
     */
    private fun startServer() {
        val config = buildConfig() ?: return
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Starting ${config.type.displayName} ${config.version} on port ${config.port}…")
        serverManager.start(config)
    }

    /**
     * サーバーを再起動する
     */
    private fun restartServer() {
        val config = buildConfig() ?: return
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Restarting…")
        serverManager.restart(config)
    }

    /**
     * サーバーを停止する
     */
    private fun stopServer() {
        setButtonState(running = false, transitioning = true)
        sysLog("[INFO] Stopping server…")
        serverManager.stop()
    }

    /**
     * サーバー設定を構築する
     */
    private fun buildConfig(): McServerConfig? {
        val version = binding.consoleVersionInput.text.toString().trim()
        if (version.isBlank()) { sysLog("[ERROR] Enter a Minecraft version."); return null }
        val port = binding.consolePortInput.text.toString().toIntOrNull() ?: 25565
        val jvm  = binding.consoleJvmArgsInput.text.toString().trim().ifBlank { "-Xmx1G -Xms512M" }
        return McServerConfig(type = selectedType, version = version, jvmArgs = jvm, port = port)
    }

    /**
     * コマンドを送信する
     */
    private fun sendCommand() {
        val cmd = binding.consoleCommandInput.text.toString().trim()
        if (cmd.isBlank()) return
        if (!serverManager.isRunning()) {
            sysLog("[WARN] Server is not running.")
            return
        }
        serverManager.sendCommand(cmd)
        binding.consoleCommandInput.setText("")
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.consoleCommandInput.windowToken, 0)
    }

    /**
     * ボタンの状態を更新する
     */
    private fun setButtonState(running: Boolean, transitioning: Boolean = false) {
        requireActivity().runOnUiThread {
            if (!isAdded) return@runOnUiThread
            val busy = transitioning
            binding.consoleStartButton.isEnabled   = !running && !busy
            binding.consoleRestartButton.isEnabled =  running && !busy
            binding.consoleStopButton.isEnabled    =  running && !busy
            binding.consoleVersionInput.isEnabled  = !running
            binding.consolePortInput.isEnabled     = !running
            binding.consoleJvmArgsInput.isEnabled  = !running
            binding.consoleTabVanilla.isEnabled    = !running
            binding.consoleTabPaper.isEnabled      = !running
            binding.consoleTabFabric.isEnabled     = !running

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

    /**
     * McServerManagerのコールバックを設定する
     */
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

    /**
     * トンネル情報を監視する
     */
    private fun observeTunnel() {
        tunnelService?.tunnelAddress?.observe(viewLifecycleOwner) { addr ->
            if (!addr.isNullOrEmpty()) sysLog("[TUNNEL] Public: $addr")
        }
        tunnelService?.authUrl?.observe(viewLifecycleOwner) { url ->
            if (url != null) sysLog("[TUNNEL] Claim: $url")
        }
    }

    /**
     * ログに行を追加する
     */
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

    /**
     * システムログメッセージを追加します。
     */
    private fun sysLog(msg: String) = appendLine(msg)

    /**
     * ログをクリアする
     */
    private fun clearLog() {
        logLines.clear()
        binding.consoleLogText.text = ""
    }

    /**
     * ログ行にタグを付ける
     */
    private fun tag(line: String) = when {
        line.contains("ERROR", true) || line.contains("Exception") ||
        line.contains("FATAL", true)  -> "✗ $line"
        line.contains("WARN", true)   -> "⚠ $line"
        line.startsWith(">")          -> line
        line.contains("[TUNNEL]") || line.contains("[INSTALL]") -> "► $line"
        else                          -> "  $line"
    }

    /**
     * システム統計情報を更新する
     */
    private fun updateStats() {
        if (!isAdded) return
        val am   = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi   = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val tot  = mi.totalMem / (1024 * 1024)
        val used = tot - mi.availMem / (1024 * 1024)
        binding.consoleRamText.text    = "${fmt(used)}/${fmt(tot)}"
        binding.consoleRamBar.progress = if (tot > 0) (used * 100 / tot).toInt() else 0
        readCpuCounters()?.let { (idle, total) ->
            val dt = total - lastCpuTotal; val di = idle - lastCpuIdle
            lastCpuIdle = idle; lastCpuTotal = total
            if (dt > 0) {
                val pct = ((dt - di) * 100 / dt).toInt().coerceIn(0, 100)
                binding.consoleCpuText.text    = "$pct%"
                binding.consoleCpuBar.progress = pct
            }
        }
        try {
            val st   = StatFs(Environment.getDataDirectory().path)
            val dtot = st.totalBytes / (1024 * 1024)
            val duse = dtot - st.availableBytes / (1024 * 1024)
            binding.consoleStorageText.text    = "${fmt(duse)}/${fmt(dtot)}"
            binding.consoleStorageBar.progress = if (dtot > 0) (duse * 100 / dtot).toInt() else 0
        } catch (_: Exception) {}
    }

    /**
     * CPUカウンターを読み取る
     */
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

    /**
     * メモリサイズを見やすくフォーマットする
     */
    private fun fmt(mb: Long) = if (mb >= 1024) "${"%.1f".format(mb / 1024.0)}G" else "${mb}M"

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.consoleRoot, Animations.BounceInDown))
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.consoleRoot, Animations.FadeOutUp))
    }
}