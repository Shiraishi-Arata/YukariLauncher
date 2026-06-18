package com.arata.yukarilauncher.feature.discord.gateway

import com.arata.yukarilauncher.feature.log.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OkHttpネイティブWebSocketを使用したDiscord Gateway実装。
 * 自動再接続、ハートビート、Identify/Resume処理を内蔵しています。
 * sendPresenceImmediate()：suspend関数を経由せず同期的にプレゼンスを送信します。
 */
class DiscordWebSocketImpl : DiscordWebSocket {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /** 現在のWebSocket接続 */
    @Volatile
    private var webSocket: WebSocket? = null

    /** GatewayからREADYを受信したかどうか */
    @Volatile
    private var ready = false

    /** 再接続用セッションID */
    private var sessionId: String? = null

    /** メッセージシーケンス番号（Resume用） */
    private var sequence: Int? = null

    /** ハートビートジョブ */
    private var heartbeatJob: Job? = null

    /** 認証トークン */
    private var token: String? = null

    /** ハートビート間隔（ミリ秒） */
    private var heartbeatInterval: Long = 41250L

    /** 意図的な切断かどうか（trueの場合は自動再接続をスキップ） */
    private var intentionalDisconnect = false

    /** 現在の再接続試行回数 */
    private var reconnectAttempts = 0

    /** 最大再接続試行回数 */
    private val maxReconnectAttempts = 5

    /** 再接続成功時に呼ばれるコールバック */
    var onReconnected: (() -> Unit)? = null

    /** READY (Dispatch)受信時に呼ばれるコールバック（初回接続と再接続の両方） */
    var onReady: (() -> Unit)? = null

    /** 最大再接続試行回数に達した時に呼ばれるコールバック */
    var onDisconnected: (() -> Unit)? = null

    /** カスタムステータス更新時のコールバック（テキスト、またはnull=解除） */
    var onCustomStatusUpdate: ((String?) -> Unit)? = null

    /**
     * Discord Gatewayに接続します。既存の接続があれば切断してから新規接続します。
     * @param token Discord認証トークン
     */
    override suspend fun connect(token: String) {
        this.token = token
        intentionalDisconnect = false
        reconnectAttempts = 0
        disconnect()
        try {
            val request = Request.Builder()
                .url("wss://gateway.discord.gg/?v=10&encoding=json")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Logging.i("DiscordWS", "Connected to gateway")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleFrame(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Logging.e("DiscordWS", "WebSocket failure: ${t.message}")
                    if (!intentionalDisconnect) scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Logging.i("DiscordWS", "WebSocket closed: $code $reason")
                    if (!intentionalDisconnect) scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Logging.e("DiscordWS", "WebSocket error: ${e.message}")
            if (!intentionalDisconnect) scheduleReconnect()
        }
    }

    /**
     * 意図的に切断します。自動再接続は行われません。
     */
    override suspend fun disconnect() {
        intentionalDisconnect = true
        ready = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        sessionId = null
        sequence = null
        reconnectAttempts = 0
    }

    /** 一定時間後に再接続をスケジュールします（最大5回、間隔は2秒ずつ増加）。 */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Logging.e("DiscordWS", "Max reconnect attempts reached")
            webSocket = null
            ready = false
            onDisconnected?.invoke()
            return
        }
        val delay = (reconnectAttempts + 1) * 2000L
        reconnectAttempts++
        Logging.i("DiscordWS", "Reconnecting in ${delay}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
        scope.launch {
            delay(delay)
            if (!intentionalDisconnect) doReconnect()
        }
    }

    /**
     * 実際の再接続処理を実行します。
     * READY受信を最大10秒待機し、成功時にonReconnectedコールバックを呼び出します。
     */
    private suspend fun doReconnect() {
        Logging.i("DiscordWS", "Attempting reconnect")
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        ready = false
        try {
            val request = Request.Builder()
                .url("wss://gateway.discord.gg/?v=10&encoding=json")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Logging.i("DiscordWS", "Reconnected to gateway")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleFrame(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Logging.e("DiscordWS", "Reconnect failure: ${t.message}")
                    if (!intentionalDisconnect) scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Logging.i("DiscordWS", "Reconnect closed: $code $reason")
                    if (!intentionalDisconnect) scheduleReconnect()
                }
            })

            // READYイベントを待機
            var retries = 0
            while (retries < 10 && !ready && !intentionalDisconnect) {
                delay(1000)
                retries++
            }
            if (ready) {
                Logging.i("DiscordWS", "Reconnect successful")
                reconnectAttempts = 0
                onReconnected?.invoke()
            }
        } catch (e: Exception) {
            Logging.e("DiscordWS", "Reconnect error: ${e.message}")
            if (!intentionalDisconnect) scheduleReconnect()
        }
    }

    /** DiscordWebSocketインターフェースの実装：suspend経由でプレゼンス送信。 */
    override suspend fun sendPresence(activity: Activity?, status: String, afk: Boolean) {
        sendPresenceImmediate(activity, status, afk)
    }

    /**
     * 同期的にプレゼンスを送信します（非suspend）。
     * runBlocking不要でメインスレッドから直接呼び出せます。
     */
    fun sendPresenceImmediate(activity: Activity?, status: String = "online", afk: Boolean = false) {
        Logging.i("DiscordWS", "sendPresenceImmediate: ready=$ready, webSocket=$webSocket")
        val payload = buildPresencePayload(activity, status, afk)
        Logging.i("DiscordWS", "sendPresenceImmediate: payload=$payload")
        sendPayload(OpCode.PRESENCE_UPDATE.code, payload)
    }

    /** Gatewayに接続され、READY状態であるかどうかを返します。 */
    override fun isConnected(): Boolean {
        return ready && webSocket != null
    }

    /**
     * Gatewayからのフレームを処理します。
     * オペレーションコードに応じてHello/Heartbeat/Identify/Resume等を振り分けます。
     */
    private fun handleFrame(text: String) {
        try {
            val json = JSONObject(text)
            val op = json.optInt("op", -1)

            when (op) {
                OpCode.HELLO.code -> handleHello(json)
                OpCode.DISPATCH.code -> handleDispatch(json)
                OpCode.HEARTBEAT.code -> sendHeartbeat()
                OpCode.HEARTBEAT_ACK.code -> { /* ハートビートACK：特に処理不要 */ }
                OpCode.RECONNECT.code -> reconnect()
                OpCode.INVALID_SESSION.code -> {
                    val resumeable = json.optBoolean("d", false)
                    if (resumeable) {
                        attemptResume()
                    } else {
                        identify(token!!)
                    }
                }
            }

            // シーケンス番号を更新（Resume用）
            val s = json.optInt("s", -1)
            if (s != -1) sequence = s
        } catch (e: Exception) {
            Logging.e("DiscordWS", "Failed to handle frame: ${e.message}")
        }
    }

    /** Helloフレーム：ハートビート間隔を設定し、IdentifyまたはResumeを送信します。 */
    private fun handleHello(json: JSONObject) {
        val data = json.optJSONObject("d")
        heartbeatInterval = data?.optLong("heartbeat_interval", 41250L) ?: 41250L
        startHeartbeat()
        if (sessionId != null && !intentionalDisconnect) {
            attemptResume()
        } else {
            identify(token!!)
        }
    }

    /** Dispatchフレーム：READYイベントを受信したらセッションIDを保存します。 */
    private fun handleDispatch(json: JSONObject) {
        val t = json.optString("t", "")
        val data = json.optJSONObject("d")
        if (t == "READY" && data != null) {
            sessionId = data.optString("session_id")
            ready = true
            reconnectAttempts = 0
            Logging.i("DiscordWS", "Ready - session_id: $sessionId")
            // カスタムステータスをREADYイベントのpresencesから抽出
            parseCustomStatusFromPresences(data)
            onReady?.invoke()
        }
        if (t == "RESUMED") {
            ready = true
            reconnectAttempts = 0
            Logging.i("DiscordWS", "Session resumed")
            onReconnected?.invoke()
            onReady?.invoke()
        }
        if (t == "PRESENCE_UPDATE" && data != null) {
            // 自身のプレゼンス更新からカスタムステータスを抽出
            parseCustomStatusFromPresence(data)
        }
    }

    private var currentUserId: String? = null

    /** READYイベントのpresences配列から自分自身のカスタムステータスを抽出します。 */
    private fun parseCustomStatusFromPresences(readyData: JSONObject) {
        currentUserId = readyData.optJSONObject("user")?.optString("id") ?: return
        val presences = readyData.optJSONArray("presences") ?: return
        for (i in 0 until presences.length()) {
            val presence = presences.optJSONObject(i) ?: continue
            val presenceUser = presence.optJSONObject("user") ?: continue
            if (presenceUser.optString("id") == currentUserId) {
                parseCustomStatusFromPresence(presence)
                return
            }
        }
    }

    /** 単一のpresenceオブジェクトからカスタムステータスを抽出します。 */
    private fun parseCustomStatusFromPresence(presence: JSONObject) {
        val presenceUser = presence.optJSONObject("user")
        if (presenceUser != null && currentUserId != null) {
            if (presenceUser.optString("id") != currentUserId) return
        }
        val activities = presence.optJSONArray("activities") ?: kotlin.run {
            onCustomStatusUpdate?.invoke(null)
            return
        }
        for (i in 0 until activities.length()) {
            val activity = activities.optJSONObject(i) ?: continue
            if (activity.optInt("type", -1) == 4) {
                val status = activity.optString("state", null)?.ifEmpty { null }
                Logging.i("DiscordWS", "Custom status: $status")
                onCustomStatusUpdate?.invoke(status)
                return
            }
        }
        onCustomStatusUpdate?.invoke(null)
    }

    /** 定期的なハートビート送信を開始します。 */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatInterval)
                sendHeartbeat()
            }
        }
    }

    /** ハートビート（Opcode 1）を送信します。 */
    private fun sendHeartbeat() {
        sendPayload(OpCode.HEARTBEAT.code, sequence)
    }

    /**
     * Identifyペイロードを送信します。
     * capabilities=65, browser="Discord Client", os="Windows"（Kizzy互換）
     */
    private fun identify(token: String) {
        val identify = JSONObject().apply {
            put("token", token)
            put("capabilities", 65)
            put("properties", JSONObject().apply {
                put("os", "Windows")
                put("browser", "Discord Client")
                put("device", "")
            })
            put("compress", false)
            put("large_threshold", 100)
        }
        sendPayload(OpCode.IDENTIFY.code, identify)
        Logging.i("DiscordWS", "Sent identify")
    }

    /** セッション復元（Resume）を試みます。セッションIDがない場合はIdentifyにフォールバック。 */
    private fun attemptResume() {
        if (sessionId == null || sequence == null) {
            identify(token!!)
            return
        }
        val resume = JSONObject().apply {
            put("token", token)
            put("session_id", sessionId)
            put("seq", sequence)
        }
        sendPayload(OpCode.RESUME.code, resume)
    }

    /** GatewayからのReconnect要求に応じて再接続します。 */
    private fun reconnect() {
        runBlocking {
            disconnect()
            delay(1000)
            token?.let { connect(it) }
        }
    }

    /**
     * ActivityオブジェクトをDiscord Gateway用のJSONペイロードに変換します。
     * 各フィールドはnull非許容の場合のみ追加されます。
     */
    private fun buildPresencePayload(activity: Activity?, status: String, afk: Boolean): JSONObject {
        return JSONObject().apply {
            put("since", 0)
            put("status", status)
            put("afk", afk)
            put("activities", JSONArray().apply {
                if (activity != null) {
                    put(JSONObject().apply {
                        put("name", activity.name)
                        put("type", activity.type)
                        activity.url?.let { put("url", it) }
                        activity.state?.let { put("state", it) }
                        activity.details?.let { put("details", it) }
                        activity.timestamps?.let { ts ->
                            put("timestamps", JSONObject().apply {
                                ts.start?.let { put("start", it) }
                                ts.end?.let { put("end", it) }
                            })
                        }
                        activity.applicationId?.let { put("application_id", it) }
                        activity.assets?.let { a ->
                            put("assets", JSONObject().apply {
                                a.large_image?.let { put("large_image", it) }
                                a.large_text?.let { put("large_text", it) }
                                a.small_image?.let { put("small_image", it) }
                                a.small_text?.let { put("small_text", it) }
                            })
                        }
                        activity.party?.let { p ->
                            put("party", JSONObject().apply {
                                p.id?.let { put("id", it) }
                                p.size?.let { put("size", JSONArray(it)) }
                            })
                        }
                        activity.buttons?.let { btns ->
                            put("buttons", JSONArray().apply {
                                btns.forEach { put(it) }
                            })
                        }
                        activity.metadata?.let { m ->
                            m.buttonUrls?.let { urls ->
                                put("metadata", JSONObject().apply {
                                    put("button_urls", JSONArray().apply {
                                        urls.forEach { put(it) }
                                    })
                                })
                            }
                        }
                    })
                }
            })
        }
    }

    /**
     * WebSocket経由でペイロードを送信します。
     * @param op オペレーションコード
     * @param data ペイロードデータ（nullの場合はJSONObject.NULLを使用）
     */
    private fun sendPayload(op: Int, data: Any?) {
        try {
            val payload = JSONObject().apply {
                put("op", op)
                if (data != null) {
                    put("d", data)
                } else {
                    put("d", JSONObject.NULL)
                }
            }
            val msg = payload.toString()
            val sent = webSocket?.send(msg)
            Logging.i("DiscordWS", "sendPayload: op=$op, sent=$sent, ws=$webSocket, len=${msg.length}")
        } catch (e: Exception) {
            Logging.e("DiscordWS", "Failed to send payload: ${e.message}")
        }
    }

    /** 全リソースを解放します。 */
    fun destroy() {
        runBlocking {
            disconnect()
        }
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
