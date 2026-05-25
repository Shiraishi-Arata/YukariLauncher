package com.arata.yukarilauncher.feature.awt

/**
 * AWT入力イベントをネイティブコードに転送するブリッジオブジェクト。
 * Java AWTのキー入力、マウス入力、カーソル位置の各イベントを
 * JNIを介してC++側のPOJAV実行環境に送信します。
 */
object AWTInputBridge {
    /** 文字入力イベントタイプ。 */
    const val EVENT_TYPE_CHAR = 1000
    /** カーソル位置イベントタイプ。 */
    const val EVENT_TYPE_CURSOR_POS = 1003
    /** キー入力イベントタイプ。 */
    const val EVENT_TYPE_KEY = 1005
    /** マウスボタンイベントタイプ。 */
    const val EVENT_TYPE_MOUSE_BUTTON = 1006

    /**
     * キー入力を送信します（押下と解放の両方を自動的に行います）。
     * @param keychar 入力された文字
     * @param keycode キーコード
     */
    fun sendKey(keychar: Char, keycode: Int) {
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, 1, 0)
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, 0, 0)
    }

    /**
     * 指定された状態でキー入力を送信します。
     * @param keychar 入力された文字
     * @param keycode キーコード
     * @param state キーの状態（1: 押下, 0: 解放）
     */
    fun sendKey(keychar: Char, keycode: Int, state: Int) {
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, state, 0)
    }

    /**
     * 文字入力を送信します。
     * @param keychar 送信する文字
     */
    fun sendChar(keychar: Char) {
        nativeSendData(EVENT_TYPE_CHAR, keychar.code, 0, 0, 0)
    }

    /**
     * マウスボタンの押下または解放を送信します。
     * @param awtButtons AWTボタンマスク値
     * @param isDown 押下の場合はtrue、解放の場合はfalse
     */
    fun sendMousePress(awtButtons: Int, isDown: Boolean) {
        nativeSendData(EVENT_TYPE_MOUSE_BUTTON, awtButtons, if (isDown) 1 else 0, 0, 0)
    }

    /**
     * マウスボタンのクリック（押下＋解放）を送信します。
     * @param awtButtons AWTボタンマスク値
     */
    fun sendMousePress(awtButtons: Int) {
        sendMousePress(awtButtons, true)
        sendMousePress(awtButtons, false)
    }

    /**
     * マウスカーソルの位置を送信します。
     * @param x X座標
     * @param y Y座標
     */
    fun sendMousePos(x: Int, y: Int) {
        nativeSendData(EVENT_TYPE_CURSOR_POS, x, y, 0, 0)
    }

    init {
        System.loadLibrary("pojavexec_awt")
    }

    /**
     * ネイティブデータを送信します。
     * @param type イベントタイプ
     * @param i1 データ1
     * @param i2 データ2
     * @param i3 データ3
     * @param i4 データ4
     */
    @JvmStatic external fun nativeSendData(type: Int, i1: Int, i2: Int, i3: Int, i4: Int)
    @JvmStatic external fun nativeClipboardReceived(data: String?, mimeTypeSub: String?)
    @JvmStatic external fun nativeMoveWindow(xoff: Int, yoff: Int)
}
