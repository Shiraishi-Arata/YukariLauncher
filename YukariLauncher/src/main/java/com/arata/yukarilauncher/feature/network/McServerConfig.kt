package com.arata.yukarilauncher.feature.network

enum class McServerType(val displayName: String) {
    VANILLA("Vanilla"),
    PAPER("Paper"),
    FABRIC("Fabric")
}

data class McServerConfig(
    val type: McServerType = McServerType.PAPER,
    val version: String = "26.1.1",
    val jvmArgs: String = "-Xmx1G -Xms512M",
    val javaVersion: Int = 25,
    val port: Int = 25565
)
