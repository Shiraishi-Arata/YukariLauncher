package com.arata.yukarilauncher.feature.version.install

/**
 * インストール可能なアドオン（ModLoaderやMod）を定義する列挙型
 * @property addonName アドオンの表示名
 */
enum class Addon(val addonName: String) {
    OPTIFINE("OptiFine"),
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    FABRIC("Fabric"),
    FABRIC_API("Fabric API"),
    QUILT("Quilt"),
    QSL("QSL");

    companion object {
        private val compatibleMap = mapOf(
            OPTIFINE to setOf(OPTIFINE, FORGE),
            FORGE to setOf(OPTIFINE, FORGE),
            NEOFORGE to setOf(NEOFORGE),
            FABRIC to setOf(FABRIC, FABRIC_API),
            FABRIC_API to setOf(FABRIC, FABRIC_API),
            QUILT to setOf(QUILT, QSL),
            QSL to setOf(QUILT, QSL)
        )

        /**
         * 指定されたアドオンと互換性のあるアドオンのセットを取得する
         * @param addon 対象のアドオン
         * @return 互換性のあるアドオンのセット
         */
        fun getCompatibles(addon: Addon) = compatibleMap[addon]
    }
}