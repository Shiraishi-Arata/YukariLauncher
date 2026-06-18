package com.arata.yukarilauncher.feature.discord.gateway

/** Discord GatewayとのWebSocket接続を抽象化したインターフェース。 */
interface DiscordWebSocket {
    /** 指定されたトークンでGatewayに接続します。 */
    suspend fun connect(token: String)
    /** 切断します。 */
    suspend fun disconnect()
    /** プレゼンスを送信します。 */
    suspend fun sendPresence(activity: Activity?, status: String = "online", afk: Boolean = false)
    /** 接続状態を返します。 */
    fun isConnected(): Boolean
}
