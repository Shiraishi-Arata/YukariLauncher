package com.arata.yukarilauncher.ui.subassembly.hotbar

import com.arata.yukarilauncher.R

/**
 * ホットバー判定タイプ
 * @param nameId タイプのローカライズ名リソースID
 * @param valueName タイプの設定保存値
 */
enum class HotbarType(val nameId: Int, val valueName: String) {
    /** 自動: 画面解像度とGUIスケールに基づいて判定枠の幅と高さを自動計算する（精度が低い可能性あり） */
    AUTO(R.string.option_hotbar_type_auto, "auto"),
    /** 手動: ユーザーが判定枠の幅と高さを手動調整する */
    MANUALLY(R.string.option_hotbar_type_manually, "manually")
}
