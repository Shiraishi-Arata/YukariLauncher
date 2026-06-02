package com.arata.yukarilauncher.ui.subassembly.hotbar

import com.arata.yukarilauncher.setting.AllSettings.Companion.hotbarType

/**
 * ホットバー設定に関するユーティリティクラス
 */
class HotbarUtils {
    companion object {
        /**
         * 現在のホットバータイプを取得する
         */
        @JvmStatic
        fun getCurrentType(): HotbarType {
            val hotbarType = hotbarType.getValue()
            return when (hotbarType) {
                "manually" -> HotbarType.MANUALLY
                "auto" -> HotbarType.AUTO
                else -> HotbarType.AUTO
            }
        }

        /**
         * 現在のホットバータイプのインデックスを取得する
         */
        @JvmStatic
        fun getCurrentTypeIndex(): Int {
            val hotbarType = hotbarType.getValue()
            return when (hotbarType) {
                "manually" -> 1
                "auto" -> 0
                else -> 0
            }
        }
    }
}