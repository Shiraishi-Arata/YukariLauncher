package com.arata.yukarilauncher.feature.discord.gateway

/** Discord Gatewayのオペレーションコード一覧。 */
enum class OpCode(val code: Int) {
    /** イベントディスパッチ */
    DISPATCH(0),
    /** ハートビート */
    HEARTBEAT(1),
    /** 認証 */
    IDENTIFY(2),
    /** プレゼンス更新 */
    PRESENCE_UPDATE(3),
    /** ボイスステート更新 */
    VOICE_STATE_UPDATE(4),
    /** セッション復元 */
    RESUME(6),
    /** 再接続要求 */
    RECONNECT(7),
    /** ギルドメンバー要求 */
    REQUEST_GUILD_MEMBERS(8),
    /** 無効なセッション */
    INVALID_SESSION(9),
    /** ハローハンドシェイク */
    HELLO(10),
    /** ハートビート確認応答 */
    HEARTBEAT_ACK(11);

    companion object {
        /** コード値からOpCodeを逆引きします。 */
        fun fromCode(code: Int): OpCode? = entries.find { it.code == code }
    }
}
