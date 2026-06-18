package com.arata.yukarilauncher.feature.awt

/**
 * AWT入力イベントの定数を定義するオブジェクト。
 * Java AWTのキーイベントおよびマウスイベントに関連する
 * マスク値、仮想キーコード、イベントIDを提供します。
 */
@Suppress("unused")
object AWTInputEvent {
    /** Shiftキーのマスク。 */
    const val SHIFT_MASK = 1
    /** Ctrlキーのマスク。 */
    const val CTRL_MASK = 1 shl 1
    /** Metaキーのマスク。 */
    const val META_MASK = 1 shl 2
    /** Altキーのマスク。 */
    const val ALT_MASK = 1 shl 3
    /** Alt Graphキーのマスク。 */
    const val ALT_GRAPH_MASK = 1 shl 5
    /** マウスボタン1のマスク。 */
    const val BUTTON1_MASK = 1 shl 4
    /** マウスボタン2のマスク（Altマスクと同一）。 */
    const val BUTTON2_MASK = ALT_MASK
    /** マウスボタン3のマスク（Metaマスクと同一）。 */
    const val BUTTON3_MASK = META_MASK
    /** Shiftキーのダウンマスク。 */
    const val SHIFT_DOWN_MASK = 1 shl 6
    /** Ctrlキーのダウンマスク。 */
    const val CTRL_DOWN_MASK = 1 shl 7
    /** Metaキーのダウンマスク。 */
    const val META_DOWN_MASK = 1 shl 8
    /** Altキーのダウンマスク。 */
    const val ALT_DOWN_MASK = 1 shl 9
    /** マウスボタン1のダウンマスク。 */
    const val BUTTON1_DOWN_MASK = 1 shl 10
    /** マウスボタン2のダウンマスク。 */
    const val BUTTON2_DOWN_MASK = 1 shl 11
    /** マウスボタン3のダウンマスク。 */
    const val BUTTON3_DOWN_MASK = 1 shl 12
    /** Alt Graphキーのダウンマスク。 */
    const val ALT_GRAPH_DOWN_MASK = 1 shl 13

    /** キーイベントIDの開始値。 */
    const val KEY_FIRST = 400
    /** キーイベントIDの終了値。 */
    const val KEY_LAST = 402
    /** キーがタイプされたイベントID。 */
    const val KEY_TYPED = KEY_FIRST
    /** キーが押されたイベントID。 */
    const val KEY_PRESSED = 1 + KEY_FIRST
    /** キーが解放されたイベントID。 */
    const val KEY_RELEASED = 2 + KEY_FIRST

    /** Enterキーの仮想キーコード。 */
    const val VK_ENTER = '\n'.code
    /** Backspaceキーの仮想キーコード。 */
    const val VK_BACK_SPACE = '\b'.code
    /** Tabキーの仮想キーコード。 */
    const val VK_TAB = '\t'.code
    /** Cancelキーの仮想キーコード。 */
    const val VK_CANCEL = 0x03
    /** Clearキーの仮想キーコード。 */
    const val VK_CLEAR = 0x0C
    /** Shiftキーの仮想キーコード。 */
    const val VK_SHIFT = 0x10
    /** Controlキーの仮想キーコード。 */
    const val VK_CONTROL = 0x11
    /** Altキーの仮想キーコード。 */
    const val VK_ALT = 0x12
    /** Pauseキーの仮想キーコード。 */
    const val VK_PAUSE = 0x13
    /** Caps Lockキーの仮想キーコード。 */
    const val VK_CAPS_LOCK = 0x14
    /** Escapeキーの仮想キーコード。 */
    const val VK_ESCAPE = 0x1B
    /** Spaceキーの仮想キーコード。 */
    const val VK_SPACE = 0x20
    /** Page Upキーの仮想キーコード。 */
    const val VK_PAGE_UP = 0x21
    /** Page Downキーの仮想キーコード。 */
    const val VK_PAGE_DOWN = 0x22
    /** Endキーの仮想キーコード。 */
    const val VK_END = 0x23
    /** Homeキーの仮想キーコード。 */
    const val VK_HOME = 0x24
    /** 左矢印キーの仮想キーコード。 */
    const val VK_LEFT = 0x25
    /** 上矢印キーの仮想キーコード。 */
    const val VK_UP = 0x26
    /** 右矢印キーの仮想キーコード。 */
    const val VK_RIGHT = 0x27
    /** 下矢印キーの仮想キーコード。 */
    const val VK_DOWN = 0x28
    /** カンマキーの仮想キーコード。 */
    const val VK_COMMA = 0x2C
    /** マイナスキーの仮想キーコード。 */
    const val VK_MINUS = 0x2D
    /** ピリオドキーの仮想キーコード。 */
    const val VK_PERIOD = 0x2E
    /** スラッシュキーの仮想キーコード。 */
    const val VK_SLASH = 0x2F
    /** 0キーの仮想キーコード。 */
    const val VK_0 = 0x30
    /** 1キーの仮想キーコード。 */
    const val VK_1 = 0x31
    /** 2キーの仮想キーコード。 */
    const val VK_2 = 0x32
    /** 3キーの仮想キーコード。 */
    const val VK_3 = 0x33
    /** 4キーの仮想キーコード。 */
    const val VK_4 = 0x34
    /** 5キーの仮想キーコード。 */
    const val VK_5 = 0x35
    /** 6キーの仮想キーコード。 */
    const val VK_6 = 0x36
    /** 7キーの仮想キーコード。 */
    const val VK_7 = 0x37
    /** 8キーの仮想キーコード。 */
    const val VK_8 = 0x38
    /** 9キーの仮想キーコード。 */
    const val VK_9 = 0x39
    /** セミコロンキーの仮想キーコード。 */
    const val VK_SEMICOLON = 0x3B
    /** イコールキーの仮想キーコード。 */
    const val VK_EQUALS = 0x3D
    /** Aキーの仮想キーコード。 */
    const val VK_A = 0x41
    /** Bキーの仮想キーコード。 */
    const val VK_B = 0x42
    /** Cキーの仮想キーコード。 */
    const val VK_C = 0x43
    /** Dキーの仮想キーコード。 */
    const val VK_D = 0x44
    /** Eキーの仮想キーコード。 */
    const val VK_E = 0x45
    /** Fキーの仮想キーコード。 */
    const val VK_F = 0x46
    /** Gキーの仮想キーコード。 */
    const val VK_G = 0x47
    /** Hキーの仮想キーコード。 */
    const val VK_H = 0x48
    /** Iキーの仮想キーコード。 */
    const val VK_I = 0x49
    /** Jキーの仮想キーコード。 */
    const val VK_J = 0x4A
    /** Kキーの仮想キーコード。 */
    const val VK_K = 0x4B
    /** Lキーの仮想キーコード。 */
    const val VK_L = 0x4C
    /** Mキーの仮想キーコード。 */
    const val VK_M = 0x4D
    /** Nキーの仮想キーコード。 */
    const val VK_N = 0x4E
    /** Oキーの仮想キーコード。 */
    const val VK_O = 0x4F
    /** Pキーの仮想キーコード。 */
    const val VK_P = 0x50
    /** Qキーの仮想キーコード。 */
    const val VK_Q = 0x51
    /** Rキーの仮想キーコード。 */
    const val VK_R = 0x52
    /** Sキーの仮想キーコード。 */
    const val VK_S = 0x53
    /** Tキーの仮想キーコード。 */
    const val VK_T = 0x54
    /** Uキーの仮想キーコード。 */
    const val VK_U = 0x55
    /** Vキーの仮想キーコード。 */
    const val VK_V = 0x56
    /** Wキーの仮想キーコード。 */
    const val VK_W = 0x57
    /** Xキーの仮想キーコード。 */
    const val VK_X = 0x58
    /** Yキーの仮想キーコード。 */
    const val VK_Y = 0x59
    /** Zキーの仮想キーコード。 */
    const val VK_Z = 0x5A
    /** 開き括弧キーの仮想キーコード。 */
    const val VK_OPEN_BRACKET = 0x5B
    /** バックスラッシュキーの仮想キーコード。 */
    const val VK_BACK_SLASH = 0x5C
    /** 閉じ括弧キーの仮想キーコード。 */
    const val VK_CLOSE_BRACKET = 0x5D
    /** テンキー0の仮想キーコード。 */
    const val VK_NUMPAD0 = 0x60
    /** テンキー1の仮想キーコード。 */
    const val VK_NUMPAD1 = 0x61
    /** テンキー2の仮想キーコード。 */
    const val VK_NUMPAD2 = 0x62
    /** テンキー3の仮想キーコード。 */
    const val VK_NUMPAD3 = 0x63
    /** テンキー4の仮想キーコード。 */
    const val VK_NUMPAD4 = 0x64
    /** テンキー5の仮想キーコード。 */
    const val VK_NUMPAD5 = 0x65
    /** テンキー6の仮想キーコード。 */
    const val VK_NUMPAD6 = 0x66
    /** テンキー7の仮想キーコード。 */
    const val VK_NUMPAD7 = 0x67
    /** テンキー8の仮想キーコード。 */
    const val VK_NUMPAD8 = 0x68
    /** テンキー9の仮想キーコード。 */
    const val VK_NUMPAD9 = 0x69
    /** 乗算キーの仮想キーコード。 */
    const val VK_MULTIPLY = 0x6A
    /** 加算キーの仮想キーコード。 */
    const val VK_ADD = 0x6B
    /** 区切りキーの仮想キーコード。 */
    const val VK_SEPARATER = 0x6C
    /** 区切りキーの仮想キーコード（エイリアス）。 */
    const val VK_SEPARATOR = VK_SEPARATER
    /** 減算キーの仮想キーコード。 */
    const val VK_SUBTRACT = 0x6D
    /** 小数点キーの仮想キーコード。 */
    const val VK_DECIMAL = 0x6E
    /** 除算キーの仮想キーコード。 */
    const val VK_DIVIDE = 0x6F
    /** Deleteキーの仮想キーコード。 */
    const val VK_DELETE = 0x7F
    /** Num Lockキーの仮想キーコード。 */
    const val VK_NUM_LOCK = 0x90
    /** Scroll Lockキーの仮想キーコード。 */
    const val VK_SCROLL_LOCK = 0x91
    /** F1キーの仮想キーコード。 */
    const val VK_F1 = 0x70
    /** F2キーの仮想キーコード。 */
    const val VK_F2 = 0x71
    /** F3キーの仮想キーコード。 */
    const val VK_F3 = 0x72
    /** F4キーの仮想キーコード。 */
    const val VK_F4 = 0x73
    /** F5キーの仮想キーコード。 */
    const val VK_F5 = 0x74
    /** F6キーの仮想キーコード。 */
    const val VK_F6 = 0x75
    /** F7キーの仮想キーコード。 */
    const val VK_F7 = 0x76
    /** F8キーの仮想キーコード。 */
    const val VK_F8 = 0x77
    /** F9キーの仮想キーコード。 */
    const val VK_F9 = 0x78
    /** F10キーの仮想キーコード。 */
    const val VK_F10 = 0x79
    /** F11キーの仮想キーコード。 */
    const val VK_F11 = 0x7A
    /** F12キーの仮想キーコード。 */
    const val VK_F12 = 0x7B
    /** F13キーの仮想キーコード。 */
    const val VK_F13 = 0xF000
    /** F14キーの仮想キーコード。 */
    const val VK_F14 = 0xF001
    /** F15キーの仮想キーコード。 */
    const val VK_F15 = 0xF002
    /** F16キーの仮想キーコード。 */
    const val VK_F16 = 0xF003
    /** F17キーの仮想キーコード。 */
    const val VK_F17 = 0xF004
    /** F18キーの仮想キーコード。 */
    const val VK_F18 = 0xF005
    /** F19キーの仮想キーコード。 */
    const val VK_F19 = 0xF006
    /** F20キーの仮想キーコード。 */
    const val VK_F20 = 0xF007
    /** F21キーの仮想キーコード。 */
    const val VK_F21 = 0xF008
    /** F22キーの仮想キーコード。 */
    const val VK_F22 = 0xF009
    /** F23キーの仮想キーコード。 */
    const val VK_F23 = 0xF00A
    /** F24キーの仮想キーコード。 */
    const val VK_F24 = 0xF00B
    /** Print Screenキーの仮想キーコード。 */
    const val VK_PRINTSCREEN = 0x9A
    /** Insertキーの仮想キーコード。 */
    const val VK_INSERT = 0x9B
    /** Helpキーの仮想キーコード。 */
    const val VK_HELP = 0x9C
    /** Metaキーの仮想キーコード。 */
    const val VK_META = 0x9D
    /** バッククォートキーの仮想キーコード。 */
    const val VK_BACK_QUOTE = 0xC0
    /** 引用符キーの仮想キーコード。 */
    const val VK_QUOTE = 0xDE
    /** テンキー上矢印の仮想キーコード。 */
    const val VK_KP_UP = 0xE0
    /** テンキー下矢印の仮想キーコード。 */
    const val VK_KP_DOWN = 0xE1
    /** テンキー左矢印の仮想キーコード。 */
    const val VK_KP_LEFT = 0xE2
    /** テンキー右矢印の仮想キーコード。 */
    const val VK_KP_RIGHT = 0xE3
    /** グレイヴアクセントデッドキーの仮想キーコード。 */
    const val VK_DEAD_GRAVE = 0x80
    /** アキュートアクセントデッドキーの仮想キーコード。 */
    const val VK_DEAD_ACUTE = 0x81
    /** サーカムフレックスデッドキーの仮想キーコード。 */
    const val VK_DEAD_CIRCUMFLEX = 0x82
    /** チルデデッドキーの仮想キーコード。 */
    const val VK_DEAD_TILDE = 0x83
    /** マクロンデッドキーの仮想キーコード。 */
    const val VK_DEAD_MACRON = 0x84
    /** ブリーヴデッドキーの仮想キーコード。 */
    const val VK_DEAD_BREVE = 0x85
    /** 上付きドットデッドキーの仮想キーコード。 */
    const val VK_DEAD_ABOVEDOT = 0x86
    /** ディエレシスデッドキーの仮想キーコード。 */
    const val VK_DEAD_DIAERESIS = 0x87
    /** 上付きリングデッドキーの仮想キーコード。 */
    const val VK_DEAD_ABOVERING = 0x88
    /** ダブルアキュートデッドキーの仮想キーコード。 */
    const val VK_DEAD_DOUBLEACUTE = 0x89
    /** キャロンデッドキーの仮想キーコード。 */
    const val VK_DEAD_CARON = 0x8a
    /** セディラデッドキーの仮想キーコード。 */
    const val VK_DEAD_CEDILLA = 0x8b
    /** オゴネクデッドキーの仮想キーコード。 */
    const val VK_DEAD_OGONEK = 0x8c
    /** イオタデッドキーの仮想キーコード。 */
    const val VK_DEAD_IOTA = 0x8d
    /** 有声記号デッドキーの仮想キーコード。 */
    const val VK_DEAD_VOICED_SOUND = 0x8e
    /** 半有声記号デッドキーの仮想キーコード。 */
    const val VK_DEAD_SEMIVOICED_SOUND = 0x8f
    /** アンパサンドキーの仮想キーコード。 */
    const val VK_AMPERSAND = 0x96
    /** アスタリスクキーの仮想キーコード。 */
    const val VK_ASTERISK = 0x97
    /** 二重引用符キーの仮想キーコード。 */
    const val VK_QUOTEDBL = 0x98
    /** 小なりキーの仮想キーコード。 */
    const val VK_LESS = 0x99
    /** 大なりキーの仮想キーコード。 */
    const val VK_GREATER = 0xa0
    /** 左中括弧キーの仮想キーコード。 */
    const val VK_BRACELEFT = 0xa1
    /** 右中括弧キーの仮想キーコード。 */
    const val VK_BRACERIGHT = 0xa2
    /** Atマークキーの仮想キーコード。 */
    const val VK_AT = 0x0200
    /** コロンキーの仮想キーコード。 */
    const val VK_COLON = 0x0201
    /** サーカムフレックスキーの仮想キーコード。 */
    const val VK_CIRCUMFLEX = 0x0202
    /** ドル記号キーの仮想キーコード。 */
    const val VK_DOLLAR = 0x0203
    /** ユーロ記号キーの仮想キーコード。 */
    const val VK_EURO_SIGN = 0x0204
    /** 感嘆符キーの仮想キーコード。 */
    const val VK_EXCLAMATION_MARK = 0x0205
    /** 逆感嘆符キーの仮想キーコード。 */
    const val VK_INVERTED_EXCLAMATION_MARK = 0x0206
    /** 左括弧キーの仮想キーコード。 */
    const val VK_LEFT_PARENTHESIS = 0x0207
    /** 番号記号キーの仮想キーコード。 */
    const val VK_NUMBER_SIGN = 0x0208
    /** プラス記号キーの仮想キーコード。 */
    const val VK_PLUS = 0x0209
    /** 右括弧キーの仮想キーコード。 */
    const val VK_RIGHT_PARENTHESIS = 0x020A
    /** アンダースコアキーの仮想キーコード。 */
    const val VK_UNDERSCORE = 0x020B
    /** Windowsキーの仮想キーコード。 */
    const val VK_WINDOWS = 0x020C
    /** コンテキストメニューキーの仮想キーコード。 */
    const val VK_CONTEXT_MENU = 0x020D
    /** Finalモードキーの仮想キーコード。 */
    const val VK_FINAL = 0x0018
    /** 変換キーの仮想キーコード。 */
    const val VK_CONVERT = 0x001C
    /** 無変換キーの仮想キーコード。 */
    const val VK_NONCONVERT = 0x001D
    /** Acceptキーの仮想キーコード。 */
    const val VK_ACCEPT = 0x001E
    /** モード変更キーの仮想キーコード。 */
    const val VK_MODECHANGE = 0x001F
    /** 仮名キーの仮想キーコード。 */
    const val VK_KANA = 0x0015
    /** 漢字キーの仮想キーコード。 */
    const val VK_KANJI = 0x0019
    /** 英数字キーの仮想キーコード。 */
    const val VK_ALPHANUMERIC = 0x00F0
    /** 片仮名キーの仮想キーコード。 */
    const val VK_KATAKANA = 0x00F1
    /** 平仮名キーの仮想キーコード。 */
    const val VK_HIRAGANA = 0x00F2
    /** 全角キーの仮想キーコード。 */
    const val VK_FULL_WIDTH = 0x00F3
    /** 半角キーの仮想キーコード。 */
    const val VK_HALF_WIDTH = 0x00F4
    /** ローマ字キーの仮想キーコード。 */
    const val VK_ROMAN_CHARACTERS = 0x00F5
    /** 全候補キーの仮想キーコード。 */
    const val VK_ALL_CANDIDATES = 0x0100
    /** 前候補キーの仮想キーコード。 */
    const val VK_PREVIOUS_CANDIDATE = 0x0101
    /** コード入力キーの仮想キーコード。 */
    const val VK_CODE_INPUT = 0x0102
    /** 日本語片仮名キーの仮想キーコード。 */
    const val VK_JAPANESE_KATAKANA = 0x0103
    /** 日本語平仮名キーの仮想キーコード。 */
    const val VK_JAPANESE_HIRAGANA = 0x0104
    /** 日本語ローマ字キーの仮想キーコード。 */
    const val VK_JAPANESE_ROMAN = 0x0105
    /** 仮名ロックキーの仮想キーコード。 */
    const val VK_KANA_LOCK = 0x0106
    /** 入力メソッドON/OFFキーの仮想キーコード。 */
    const val VK_INPUT_METHOD_ON_OFF = 0x0107
    /** 切り取りキーの仮想キーコード。 */
    const val VK_CUT = 0xFFD1
    /** コピーキーの仮想キーコード。 */
    const val VK_COPY = 0xFFCD
    /** 貼り付けキーの仮想キーコード。 */
    const val VK_PASTE = 0xFFCF
    /** 元に戻すキーの仮想キーコード。 */
    const val VK_UNDO = 0xFFCB
    /** やり直しキーの仮想キーコード。 */
    const val VK_AGAIN = 0xFFC9
    /** 検索キーの仮想キーコード。 */
    const val VK_FIND = 0xFFD0
    /** プロパティキーの仮想キーコード。 */
    const val VK_PROPS = 0xFFCA
    /** 停止キーの仮想キーコード。 */
    const val VK_STOP = 0xFFC8
    /** Composeキーの仮想キーコード。 */
    const val VK_COMPOSE = 0xFF20
    /** Alt Graphキーの仮想キーコード。 */
    const val VK_ALT_GRAPH = 0xFF7E
    /** Beginキーの仮想キーコード。 */
    const val VK_BEGIN = 0xFF58
    /** 未定義の仮想キーコード。 */
    const val VK_UNDEFINED = 0x0
    /** 未定義の文字。 */
    const val CHAR_UNDEFINED = 0xFFFF.toChar()
    /** キーの位置が不明。 */
    const val KEY_LOCATION_UNKNOWN = 0
    /** キーの位置が標準。 */
    const val KEY_LOCATION_STANDARD = 1
    /** キーの位置が左側。 */
    const val KEY_LOCATION_LEFT = 2
    /** キーの位置が右側。 */
    const val KEY_LOCATION_RIGHT = 3
    /** キーの位置がテンキー。 */
    const val KEY_LOCATION_NUMPAD = 4

    /** マウスイベントIDの開始値。 */
    const val MOUSE_FIRST = 500
    /** マウスイベントIDの終了値。 */
    const val MOUSE_LAST = 507
    /** マウスクリックイベントID。 */
    const val MOUSE_CLICKED = MOUSE_FIRST
    /** マウスボタン押下イベントID。 */
    const val MOUSE_PRESSED = 1 + MOUSE_FIRST
    /** マウスボタン解放イベントID。 */
    const val MOUSE_RELEASED = 2 + MOUSE_FIRST
    /** マウス移動イベントID。 */
    const val MOUSE_MOVED = 3 + MOUSE_FIRST
    /** マウス領域進入イベントID。 */
    const val MOUSE_ENTERED = 4 + MOUSE_FIRST
    /** マウス領域退出イベントID。 */
    const val MOUSE_EXITED = 5 + MOUSE_FIRST
    /** マウスドラッグイベントID。 */
    const val MOUSE_DRAGGED = 6 + MOUSE_FIRST
    /** マウスホイールイベントID。 */
    const val MOUSE_WHEEL = 7 + MOUSE_FIRST
    /** ボタンなし。 */
    const val NOBUTTON = 0
    /** マウスボタン1。 */
    const val BUTTON1 = 1
    /** マウスボタン2。 */
    const val BUTTON2 = 2
    /** マウスボタン3。 */
    const val BUTTON3 = 3
}