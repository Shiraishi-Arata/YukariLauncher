package com.arata.yukarilauncher.ui.subassembly.customcontrols.handleview

import android.view.View
import com.arata.yukarilauncher.ui.subassembly.customcontrols.buttons.ControlInterface

/** アクション行に配置されるアクションボタンのインターフェース。 */
interface ActionButtonInterface : View.OnClickListener {
    /** ボタンの初期化処理を行います。 */
    fun init()
    /**
     * フォローするビューを設定します。
     * @param view フォローするコントロール
     */
    fun setFollowedView(view: ControlInterface?)
    /** クリック時のアクションを実行します。 */
    fun onClick()
    /** @return ボタンを表示すべきかどうか */
    fun shouldBeVisible(): Boolean

    override fun onClick(v: View) = onClick()
}