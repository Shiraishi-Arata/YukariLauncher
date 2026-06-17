package com.arata.yukarilauncher.feature.discord.gateway

/** セッション復元（Resume）ペイロード。 */
data class Resume(
    val token: String,
    val session_id: String,
    val seq: Int
)
