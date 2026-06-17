package com.arata.yukarilauncher.feature.discord.gateway

/** Discord Gatewayの受信ペイロードを表します。 */
data class Payload(
    val op: Int,
    val d: Any? = null,
    val s: Int? = null,
    val t: String? = null
)
