package com.arata.yukarilauncher.feature.discord

import com.arata.yukarilauncher.InfoDistributor
import com.arata.yukarilauncher.feature.discord.gateway.Activity
import com.arata.yukarilauncher.feature.discord.gateway.Assets
import com.arata.yukarilauncher.feature.discord.gateway.DiscordWebSocket
import com.arata.yukarilauncher.feature.discord.gateway.DiscordWebSocketImpl
import com.arata.yukarilauncher.feature.discord.gateway.Timestamps
import com.arata.yukarilauncher.feature.log.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Discord RPCの接続管理とプレゼンス更新を行うシングルトン。
 * :launcherプロセスで動作し、WebSocketを介してDiscord Gatewayと通信します。
 * :gameプロセスからのRPC更新要求はブロードキャスト経由で受け取ります。
 */
object DiscordRpcManager {
    /** Discord Application ID（Kizzy互換） */
    private const val APPLICATION_ID = "1369911585576845362"

    /** 各種Modローダーの画像URL（Imgurにホスティング） */
    private val MOD_LOADER_ICONS = mapOf(
        "fabric" to "https://i.imgur.com/Ol7HksG.png",
        "forge" to "https://i.imgur.com/5RkOo2h.png",
        "neoforge" to "https://i.imgur.com/wNQaYRp.png",
        "quilt" to "https://i.imgur.com/PzX0QbV.png"
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** WebSocketインスタンス（@Volatileでスレッド間可視性を保証） */
    @Volatile
    private var webSocket: DiscordWebSocket? = null

    /** Discord Gatewayへの接続状態 */
    @Volatile
    private var connected = false

    /** ランチャー画面のアクティビティ開始時刻 */
    private var launcherActivityStartTime: Long = 0L

    /**
     * 画像プロキシキャッシュ。
     * key: 画像識別子（"launcher", "fabric", "version_icon::<version>"）
     * value: Discordのexternal_asset_path（"mp:..."）
     */
    private var imagePaths: Map<String, String> = emptyMap()

    /** ゲームプレイ中かどうか（@VolatileでUIスレッドからの読み取りに対応） */
    @Volatile
    private var inGame = false

    /** 最後に起動したゲームの情報（プレゼンス再送用） */
    private var lastGameVersion: String = ""
    private var lastGameModLoader: String = ""
    private var lastGameMcVersion: String = ""
    private var lastGameMods: Int = 0

    /**
     * Discord Gatewayに接続し、認証・画像キャッシュ・プレゼンス設定を行います。
     * RPCが無効、またはアカウント未選択の場合は何も行いません。
     */
    fun connect() {
        if (connected) return
        if (!DiscordPrefs.isRpcEnabled()) return
        val accountId = DiscordPrefs.getSelectedAccountId() ?: return
        val account = DiscordPrefs.getAccounts().find { it.id == accountId } ?: return

        launcherActivityStartTime = System.currentTimeMillis()
        connected = true

        imagePaths = DiscordPrefs.getImagePaths()

        val ws = DiscordWebSocketImpl()
        ws.onReconnected = { resendCurrentPresence() }
        webSocket = ws

        scope.launch {
            try {
                ws.connect(account.token)
            } catch (e: Exception) {
                Logging.e("DiscordRPC", "Connection error: ${e.message}")
                connected = false
            }
        }

        // 接続完了を待機してから画像キャッシュとプレゼンスを初期化
        scope.launch {
            delay(1000)
            var retries = 0
            while (retries < 20 && !ws.isConnected()) {
                delay(500)
                retries++
            }
            if (ws.isConnected()) {
                Logging.i("DiscordRPC", "Connected to Discord gateway")
                refreshImageCache()
                if (!inGame) updateLauncherPresence()
            } else {
                Logging.e("DiscordRPC", "Timed out waiting for connection")
                connected = false
            }
        }
    }

    /**
     * 画像プロキシキャッシュを最新化します。
     * ランチャーアイコンとModローダーアイコンをImgur→Discord external-assets経由で取得します。
     */
    private fun refreshImageCache() {
        if (imagePaths["launcher"] == null) {
            DiscordAccountManager.proxyLauncherIcon { path ->
                if (path != null) {
                    imagePaths = imagePaths + ("launcher" to path)
                    DiscordPrefs.saveImagePaths(imagePaths)
                    if (connected) resendCurrentPresence()
                }
            }
        }

        val missingModUrls = MOD_LOADER_ICONS
            .filterKeys { imagePaths[it] == null }
            .values.toList()
        if (missingModUrls.isNotEmpty()) {
            DiscordAccountManager.proxyExternalAssets(missingModUrls) { paths ->
                if (paths.isNotEmpty()) {
                    val merged = imagePaths.toMutableMap()
                    MOD_LOADER_ICONS.forEach { (key, url) ->
                        paths[url]?.let { merged[key] = it }
                    }
                    imagePaths = merged
                    DiscordPrefs.saveImagePaths(imagePaths)
                    if (connected) resendCurrentPresence()
                }
            }
        }
    }

    /** 現在の状態に応じてプレゼンスを再送します（WebSocket再接続時のコールバック用）。 */
    private fun resendCurrentPresence() {
        if (inGame) {
            sendGamePresence()
        } else {
            updateLauncherPresence()
        }
    }

    /** 画像プロキシキャッシュから指定されたキーの画像パスを取得します。 */
    private fun cachedImage(key: String): String? = imagePaths[key]

    /**
     * Discord Gatewayから切断し、リソースをクリーンアップします。
     */
    fun disconnect() {
        inGame = false
        connected = false
        scope.launch {
            webSocket?.disconnect()
            webSocket = null
        }
    }

    /**
     * WebSocketを再接続します。既存の接続を切断後、500ms待機して再接続を試みます。
     */
    fun reconnect() {
        scope.launch {
            connected = false
            try {
                webSocket?.disconnect()
            } catch (e: Exception) {
                Logging.e("DiscordRPC", "Disconnect error: ${e.message}")
            }
            webSocket = null
            delay(500)
            connect()
        }
    }

    /**
     * ランチャー画面のRich PresenceをDiscordに送信します。
     * 5分以上操作がない場合はステータスを"Idle"に設定します。
     * このメソッドはブロードキャストレシーバーからも呼び出されます。
     */
    fun updateLauncherPresence() {
        if (!connected || webSocket == null) {
            Logging.w("DiscordRPC", "updateLauncherPresence: skipped (connected=$connected, webSocket=$webSocket)")
            return
        }
        Logging.i("DiscordRPC", "updateLauncherPresence: sending presence")
        inGame = false
        launcherActivityStartTime = System.currentTimeMillis()
        val lastActivity = DiscordPrefs.getLastActivityTime()
        val idleThreshold = 5 * 60 * 1000L
        val isIdle = lastActivity > 0 && (System.currentTimeMillis() - lastActivity) > idleThreshold
        val largeImg = cachedImage("launcher")
        val useAssets = largeImg != null

        val impl = webSocket as? DiscordWebSocketImpl
        if (impl == null) {
            Logging.w("DiscordRPC", "updateLauncherPresence: webSocket is not DiscordWebSocketImpl!")
            return
        }
        Logging.i("DiscordRPC", "updateLauncherPresence: sending via sendPresenceImmediate")
        impl.sendPresenceImmediate(
            activity = Activity(
                name = InfoDistributor.APP_NAME,
                type = 0,
                state = if (isIdle) "Idle" else "In Launcher",
                timestamps = Timestamps(start = launcherActivityStartTime),
                applicationId = if (useAssets) APPLICATION_ID else null,
                assets = largeImg?.let { Assets(large_image = it, large_text = InfoDistributor.APP_NAME) }
            ),
            status = if (isIdle) "idle" else "online"
        )
        Logging.i("DiscordRPC", "updateLauncherPresence: sendPresenceImmediate completed")
    }

    /**
     * ゲームプレイ中のRich PresenceをDiscordに送信します。
     * バージョンアイコン、Modローダーアイコンを画像プロキシ経由で設定します。
     * @param versionName ゲームバージョン名（例："1.20.1-Fabric-0.15.11"）
     * @param modLoader Modローダー名
     * @param mcVersion Minecraftバージョン
     * @param enabledMods 有効なMod数
     * @param iconBytes バージョンアイコンのバイトデータ（任意）
     */
    fun updateGamePresence(versionName: String, modLoader: String, mcVersion: String, enabledMods: Int = 0, iconBytes: ByteArray? = null) {
        if (!connected || webSocket == null) return
        inGame = true
        lastGameVersion = versionName
        lastGameModLoader = modLoader
        lastGameMcVersion = mcVersion
        lastGameMods = enabledMods

        val versionIconKey = "version_icon::$versionName"
        if (iconBytes != null && imagePaths[versionIconKey] == null) {
            DiscordAccountManager.proxyImageBytes(iconBytes, "version_${versionName}.png") { path ->
                if (path != null) {
                    imagePaths = imagePaths + (versionIconKey to path)
                    DiscordPrefs.saveImagePaths(imagePaths)
                    if (connected) sendGamePresence()
                }
            }
        }

        sendGamePresence()
    }

    /**
     * 現在のゲーム状態を元にプレゼンスを構築して送信します。
     * large_image: バージョンアイコン優先、なければModローダーアイコン
     * small_image: ランチャーアイコン
     * applicationIdはアセット使用時のみ設定（Kizzyのフォールバックアイコンを回避）
     */
    private fun sendGamePresence() {
        val state = buildString {
            append(lastGameModLoader.ifEmpty { "Vanilla" })
            if (lastGameMods > 0) append(" | $lastGameMods mods")
        }
        val details = lastGameVersion.ifEmpty { lastGameMcVersion.ifEmpty { "Playing Minecraft" } }
        val gameStartTime = System.currentTimeMillis()

        val versionIconKey = "version_icon::${lastGameVersion}"
        val largeImg = cachedImage(versionIconKey) ?: when {
            lastGameModLoader.contains("Fabric", ignoreCase = true) -> cachedImage("fabric")
            lastGameModLoader.contains("Forge", ignoreCase = true) -> cachedImage("forge")
            lastGameModLoader.contains("NeoForge", ignoreCase = true) -> cachedImage("neoforge")
            lastGameModLoader.contains("Quilt", ignoreCase = true) -> cachedImage("quilt")
            else -> null
        }
        val smallImg = cachedImage("launcher")
        val useAssets = largeImg != null || smallImg != null

        Logging.i("DiscordRPC", "sendGamePresence: inGame=$inGame, connected=$connected, webSocket!=null=${webSocket!=null}")
        val impl = webSocket as? DiscordWebSocketImpl
        if (impl == null) {
            Logging.w("DiscordRPC", "sendGamePresence: webSocket is not DiscordWebSocketImpl!")
            return
        }
        impl.sendPresenceImmediate(
            activity = Activity(
                name = InfoDistributor.APP_NAME,
                type = 0,
                state = state,
                details = details,
                timestamps = Timestamps(start = gameStartTime),
                applicationId = if (useAssets) APPLICATION_ID else null,
                assets = Assets(
                    large_image = largeImg,
                    large_text = details,
                    small_image = smallImg,
                    small_text = InfoDistributor.APP_NAME
                ).takeIf { useAssets }
            )
        )
        Logging.i("DiscordRPC", "sendGamePresence: completed")
    }

    /**
     * ユーザーの最終アクティビティ時刻を現在時刻で更新します。
     * Idle判定に使用されます。
     */
    fun touchActivity() {
        DiscordPrefs.setLastActivityTime(System.currentTimeMillis())
    }

    /** 全リソースを解放します。アプリ終了時に呼び出されます。 */
    fun destroy() {
        disconnect()
        scope.cancel()
    }

    /** Discord Gatewayに接続されているかどうかを返します。 */
    fun isConnected(): Boolean = connected
}
