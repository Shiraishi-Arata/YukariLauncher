package com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard

/** 文字入力をMinecraftエンジンに送信する戦略インターフェース。 */
interface CharacterSenderStrategy {
    /** バックスペースキーを送信します。 */
    fun sendBackspace()
    /** エンターキーを送信します。 */
    fun sendEnter()
    /**
     * 1文字を送信します。
     * @param character 送信する文字
     */
    fun sendChar(character: Char)
}