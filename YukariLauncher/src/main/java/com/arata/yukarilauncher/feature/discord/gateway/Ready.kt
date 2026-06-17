package com.arata.yukarilauncher.feature.discord.gateway

/** GatewayからのREADYイベントデータ。 */
data class Ready(
    val v: Int,
    val user: ReadyUser,
    val session_id: String
)

/** READYイベントに含まれるユーザー情報。 */
data class ReadyUser(
    val id: String,
    val username: String,
    val discriminator: String
)
