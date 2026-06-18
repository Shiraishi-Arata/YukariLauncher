package com.arata.yukarilauncher.ui.subassembly.customcontrols

/** ジョイスティックコントロールの設定データを保持するクラス。ControlDataを継承し、フォワードロックや絶対追跡の設定を追加します。 */
class ControlJoystickData : ControlData {
    /** フォワードロックが有効かどうか。 */
    var forwardLock = false
    /** 絶対座標追跡が有効かどうか。 */
    var absolute = false

    constructor() : super()

    /** @param properties コピー元のControlJoystickData */
    constructor(properties: ControlJoystickData) : super(properties) {
        forwardLock = properties.forwardLock
        absolute = properties.absolute
    }
}