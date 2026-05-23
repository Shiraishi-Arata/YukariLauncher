package com.arata.yukarilauncher.ui.subassembly.customcontrols

/**
 * コントロール設定のリストアイテムを表すBeanクラス
 */
class ControlItemBean(@JvmField val controlInfoData: ControlInfoData) {
    @JvmField
    var isHighlighted: Boolean = false
    @JvmField
    var isInvalid: Boolean = false
}
