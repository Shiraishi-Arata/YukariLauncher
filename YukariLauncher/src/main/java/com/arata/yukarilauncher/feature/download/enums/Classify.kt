package com.arata.yukarilauncher.feature.download.enums

/**
 * ダウンロードリソースの種類を分類する列挙型。
 * 全て、Mod、ModPack、リソースパック、ワールド、シェーダーパックの区分を表す。
 */
enum class Classify(val type: Int) {
    ALL(-1),
    MOD(0),
    MODPACK(1),
    RESOURCE_PACK(2),
    WORLD(3),
    SHADER_PACK(4)
}
