package com.arata.yukarilauncher.feature.discord.gateway

/** ハートビートペイロード。 */
data class HeartBeat(
    val op: Int = OpCode.HEARTBEAT.code,
    val d: Int? = null
)
