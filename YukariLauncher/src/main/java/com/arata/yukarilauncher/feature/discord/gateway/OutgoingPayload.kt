package com.arata.yukarilauncher.feature.discord.gateway

/** Discord Gatewayへの送信ペイロードを表します。 */
data class OutgoingPayload(
    val op: Int,
    val d: Any? = null
)
