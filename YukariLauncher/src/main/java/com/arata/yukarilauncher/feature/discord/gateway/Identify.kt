package com.arata.yukarilauncher.feature.discord.gateway

/** GatewayへのIdentifyペイロード。 */
data class Identify(
    val token: String,
    val properties: IdentifyProperties = IdentifyProperties(),
    val compress: Boolean = false,
    val large_threshold: Int = 250,
    val presence: Presence? = null,
    val intents: Int = 1
)

/** Identifyで送信するクライアント情報。 */
data class IdentifyProperties(
    val os: String = "android",
    val browser: String = "YukariLauncher",
    val device: String = "YukariLauncher"
)
