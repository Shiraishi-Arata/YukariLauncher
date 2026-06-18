package com.arata.yukarilauncher.ui.subassembly.customcontrols

/** コントロールエディタを終了するためのリスナーインターフェース。 */
fun interface EditorExitable {
    /** エディタを終了するための処理を実行します。 */
    fun exitEditor()
}