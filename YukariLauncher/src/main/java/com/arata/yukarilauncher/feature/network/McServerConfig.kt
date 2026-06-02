package com.arata.yukarilauncher.feature.network

/**
 * Minecraftサーバーの種類を定義する列挙型。
 * 各サーバータイプの表示名を保持する。
 */
enum class McServerType(val displayName: String) {
    VANILLA("Vanilla"),
    PAPER("Paper"),
    FABRIC("Fabric")
}

/**
 * Minecraftサーバーの設定を保持するデータクラス。
 * サーバータイプ、バージョン、JVM引数、Javaバージョン、ポートを設定する。
 */
data class McServerConfig(
    val type: McServerType = McServerType.PAPER,
    val version: String = "26.1.1",
    val jvmArgs: String = "-Xmx1G -Xms512M",
    val javaVersion: Int = 25,
    val port: Int = 25565
)