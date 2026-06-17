package com.arata.yukarilauncher.feature.discord.gateway

/** Discordのプレゼンス情報。 */
data class Presence(
    val activities: List<Activity> = emptyList(),
    val status: String = "online",
    val afk: Boolean = false,
    val since: Long = 0L
)
