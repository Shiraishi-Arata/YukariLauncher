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
                "none" -> HotbarType.NONE
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
                "none" -> 0
                "manually" -> 2
                "auto" -> 1
                else -> 1
            }
        }
    }
}