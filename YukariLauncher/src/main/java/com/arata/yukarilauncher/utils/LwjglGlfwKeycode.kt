package com.arata.yukarilauncher.utils

/**
 * LWJGLとGLFWのキーコード定数を定義するオブジェクト。
 * GLFWライブラリのキー入力およびマウス入力に関する定数を提供します。
 * キーボードの各キーに対応するGLFW_KEY_*定数や、マウスボタン、モディファイアキーの定数を保持します。
 */
@Suppress("unused")
object LwjglGlfwKeycode {
    /** 不明なキー。 */
    const val GLFW_KEY_UNKNOWN: Short = 0

    /** Spaceキー。 */
    const val GLFW_KEY_SPACE: Short = 32
    /** アポストロフィキー。 */
    const val GLFW_KEY_APOSTROPHE: Short = 39
    /** カンマキー。 */
    const val GLFW_KEY_COMMA: Short = 44
    /** マイナスキー。 */
    const val GLFW_KEY_MINUS: Short = 45
    /** ピリオドキー。 */
    const val GLFW_KEY_PERIOD: Short = 46
    /** スラッシュキー。 */
    const val GLFW_KEY_SLASH: Short = 47
    /** 0キー。 */
    const val GLFW_KEY_0: Short = 48
    /** 1キー。 */
    const val GLFW_KEY_1: Short = 49
    /** 2キー。 */
    const val GLFW_KEY_2: Short = 50
    /** 3キー。 */
    const val GLFW_KEY_3: Short = 51
    /** 4キー。 */
    const val GLFW_KEY_4: Short = 52
    /** 5キー。 */
    const val GLFW_KEY_5: Short = 53
    /** 6キー。 */
    const val GLFW_KEY_6: Short = 54
    /** 7キー。 */
    const val GLFW_KEY_7: Short = 55
    /** 8キー。 */
    const val GLFW_KEY_8: Short = 56
    /** 9キー。 */
    const val GLFW_KEY_9: Short = 57
    /** セミコロンキー。 */
    const val GLFW_KEY_SEMICOLON: Short = 59
    /** イコールキー。 */
    const val GLFW_KEY_EQUAL: Short = 61
    /** Aキー。 */
    const val GLFW_KEY_A: Short = 65
    /** Bキー。 */
    const val GLFW_KEY_B: Short = 66
    /** Cキー。 */
    const val GLFW_KEY_C: Short = 67
    /** Dキー。 */
    const val GLFW_KEY_D: Short = 68
    /** Eキー。 */
    const val GLFW_KEY_E: Short = 69
    /** Fキー。 */
    const val GLFW_KEY_F: Short = 70
    /** Gキー。 */
    const val GLFW_KEY_G: Short = 71
    /** Hキー。 */
    const val GLFW_KEY_H: Short = 72
    /** Iキー。 */
    const val GLFW_KEY_I: Short = 73
    /** Jキー。 */
    const val GLFW_KEY_J: Short = 74
    /** Kキー。 */
    const val GLFW_KEY_K: Short = 75
    /** Lキー。 */
    const val GLFW_KEY_L: Short = 76
    /** Mキー。 */
    const val GLFW_KEY_M: Short = 77
    /** Nキー。 */
    const val GLFW_KEY_N: Short = 78
    /** Oキー。 */
    const val GLFW_KEY_O: Short = 79
    /** Pキー。 */
    const val GLFW_KEY_P: Short = 80
    /** Qキー。 */
    const val GLFW_KEY_Q: Short = 81
    /** Rキー。 */
    const val GLFW_KEY_R: Short = 82
    /** Sキー。 */
    const val GLFW_KEY_S: Short = 83
    /** Tキー。 */
    const val GLFW_KEY_T: Short = 84
    /** Uキー。 */
    const val GLFW_KEY_U: Short = 85
    /** Vキー。 */
    const val GLFW_KEY_V: Short = 86
    /** Wキー。 */
    const val GLFW_KEY_W: Short = 87
    /** Xキー。 */
    const val GLFW_KEY_X: Short = 88
    /** Yキー。 */
    const val GLFW_KEY_Y: Short = 89
    /** Zキー。 */
    const val GLFW_KEY_Z: Short = 90
    /** 左大括弧キー。 */
    const val GLFW_KEY_LEFT_BRACKET: Short = 91
    /** バックスラッシュキー。 */
    const val GLFW_KEY_BACKSLASH: Short = 92
    /** 右大括弧キー。 */
    const val GLFW_KEY_RIGHT_BRACKET: Short = 93
    /** グレイヴアクセントキー。 */
    const val GLFW_KEY_GRAVE_ACCENT: Short = 96
    /** 非USキーボードのキー1。 */
    const val GLFW_KEY_WORLD_1: Short = 161
    /** 非USキーボードのキー2。 */
    const val GLFW_KEY_WORLD_2: Short = 162

    /** Escapeキー。 */
    const val GLFW_KEY_ESCAPE: Short = 256
    /** Enterキー。 */
    const val GLFW_KEY_ENTER: Short = 257
    /** Tabキー。 */
    const val GLFW_KEY_TAB: Short = 258
    /** Backspaceキー。 */
    const val GLFW_KEY_BACKSPACE: Short = 259
    /** Insertキー。 */
    const val GLFW_KEY_INSERT: Short = 260
    /** Deleteキー。 */
    const val GLFW_KEY_DELETE: Short = 261
    /** 右矢印キー。 */
    const val GLFW_KEY_RIGHT: Short = 262
    /** 左矢印キー。 */
    const val GLFW_KEY_LEFT: Short = 263
    /** 下矢印キー。 */
    const val GLFW_KEY_DOWN: Short = 264
    /** 上矢印キー。 */
    const val GLFW_KEY_UP: Short = 265
    /** Page Upキー。 */
    const val GLFW_KEY_PAGE_UP: Short = 266
    /** Page Downキー。 */
    const val GLFW_KEY_PAGE_DOWN: Short = 267
    /** Homeキー。 */
    const val GLFW_KEY_HOME: Short = 268
    /** Endキー。 */
    const val GLFW_KEY_END: Short = 269
    /** Caps Lockキー。 */
    const val GLFW_KEY_CAPS_LOCK: Short = 280
    /** Scroll Lockキー。 */
    const val GLFW_KEY_SCROLL_LOCK: Short = 281
    /** Num Lockキー。 */
    const val GLFW_KEY_NUM_LOCK: Short = 282
    /** Print Screenキー。 */
    const val GLFW_KEY_PRINT_SCREEN: Short = 283
    /** Pauseキー。 */
    const val GLFW_KEY_PAUSE: Short = 284
    /** F1キー。 */
    const val GLFW_KEY_F1: Short = 290
    /** F2キー。 */
    const val GLFW_KEY_F2: Short = 291
    /** F3キー。 */
    const val GLFW_KEY_F3: Short = 292
    /** F4キー。 */
    const val GLFW_KEY_F4: Short = 293
    /** F5キー。 */
    const val GLFW_KEY_F5: Short = 294
    /** F6キー。 */
    const val GLFW_KEY_F6: Short = 295
    /** F7キー。 */
    const val GLFW_KEY_F7: Short = 296
    /** F8キー。 */
    const val GLFW_KEY_F8: Short = 297
    /** F9キー。 */
    const val GLFW_KEY_F9: Short = 298
    /** F10キー。 */
    const val GLFW_KEY_F10: Short = 299
    /** F11キー。 */
    const val GLFW_KEY_F11: Short = 300
    /** F12キー。 */
    const val GLFW_KEY_F12: Short = 301
    /** F13キー。 */
    const val GLFW_KEY_F13: Short = 302
    /** F14キー。 */
    const val GLFW_KEY_F14: Short = 303
    /** F15キー。 */
    const val GLFW_KEY_F15: Short = 304
    /** F16キー。 */
    const val GLFW_KEY_F16: Short = 305
    /** F17キー。 */
    const val GLFW_KEY_F17: Short = 306
    /** F18キー。 */
    const val GLFW_KEY_F18: Short = 307
    /** F19キー。 */
    const val GLFW_KEY_F19: Short = 308
    /** F20キー。 */
    const val GLFW_KEY_F20: Short = 309
    /** F21キー。 */
    const val GLFW_KEY_F21: Short = 310
    /** F22キー。 */
    const val GLFW_KEY_F22: Short = 311
    /** F23キー。 */
    const val GLFW_KEY_F23: Short = 312
    /** F24キー。 */
    const val GLFW_KEY_F24: Short = 313
    /** F25キー。 */
    const val GLFW_KEY_F25: Short = 314
    /** テンキー0。 */
    const val GLFW_KEY_KP_0: Short = 320
    /** テンキー1。 */
    const val GLFW_KEY_KP_1: Short = 321
    /** テンキー2。 */
    const val GLFW_KEY_KP_2: Short = 322
    /** テンキー3。 */
    const val GLFW_KEY_KP_3: Short = 323
    /** テンキー4。 */
    const val GLFW_KEY_KP_4: Short = 324
    /** テンキー5。 */
    const val GLFW_KEY_KP_5: Short = 325
    /** テンキー6。 */
    const val GLFW_KEY_KP_6: Short = 326
    /** テンキー7。 */
    const val GLFW_KEY_KP_7: Short = 327
    /** テンキー8。 */
    const val GLFW_KEY_KP_8: Short = 328
    /** テンキー9。 */
    const val GLFW_KEY_KP_9: Short = 329
    /** テンキー小数点。 */
    const val GLFW_KEY_KP_DECIMAL: Short = 330
    /** テンキー除算。 */
    const val GLFW_KEY_KP_DIVIDE: Short = 331
    /** テンキー乗算。 */
    const val GLFW_KEY_KP_MULTIPLY: Short = 332
    /** テンキー減算。 */
    const val GLFW_KEY_KP_SUBTRACT: Short = 333
    /** テンキー加算。 */
    const val GLFW_KEY_KP_ADD: Short = 334
    /** テンキーEnter。 */
    const val GLFW_KEY_KP_ENTER: Short = 335
    /** テンキーイコール。 */
    const val GLFW_KEY_KP_EQUAL: Short = 336
    /** 左Shiftキー。 */
    const val GLFW_KEY_LEFT_SHIFT: Short = 340
    /** 左Controlキー。 */
    const val GLFW_KEY_LEFT_CONTROL: Short = 341
    /** 左Altキー。 */
    const val GLFW_KEY_LEFT_ALT: Short = 342
    /** 左Superキー。 */
    const val GLFW_KEY_LEFT_SUPER: Short = 343
    /** 右Shiftキー。 */
    const val GLFW_KEY_RIGHT_SHIFT: Short = 344
    /** 右Controlキー。 */
    const val GLFW_KEY_RIGHT_CONTROL: Short = 345
    /** 右Altキー。 */
    const val GLFW_KEY_RIGHT_ALT: Short = 346
    /** 右Superキー。 */
    const val GLFW_KEY_RIGHT_SUPER: Short = 347
    /** Menuキー。 */
    const val GLFW_KEY_MENU: Short = 348
    /** 最後のキー（Menuキー）。 */
    const val GLFW_KEY_LAST: Short = GLFW_KEY_MENU

    /** Shiftモディファイア。 */
    const val GLFW_MOD_SHIFT = 0x1
    /** Controlモディファイア。 */
    const val GLFW_MOD_CONTROL = 0x2
    /** Altモディファイア。 */
    const val GLFW_MOD_ALT = 0x4
    /** Superモディファイア。 */
    const val GLFW_MOD_SUPER = 0x8
    /** Caps Lockモディファイア。 */
    const val GLFW_MOD_CAPS_LOCK = 0x10
    /** Num Lockモディファイア。 */
    const val GLFW_MOD_NUM_LOCK = 0x20

    /** マウスボタン1。 */
    const val GLFW_MOUSE_BUTTON_1: Short = 0
    /** マウスボタン2。 */
    const val GLFW_MOUSE_BUTTON_2: Short = 1
    /** マウスボタン3。 */
    const val GLFW_MOUSE_BUTTON_3: Short = 2
    /** マウスボタン4。 */
    const val GLFW_MOUSE_BUTTON_4: Short = 3
    /** マウスボタン5。 */
    const val GLFW_MOUSE_BUTTON_5: Short = 4
    /** マウスボタン6。 */
    const val GLFW_MOUSE_BUTTON_6: Short = 5
    /** マウスボタン7。 */
    const val GLFW_MOUSE_BUTTON_7: Short = 6
    /** マウスボタン8。 */
    const val GLFW_MOUSE_BUTTON_8: Short = 7
    /** 最後のマウスボタン。 */
    const val GLFW_MOUSE_BUTTON_LAST: Short = GLFW_MOUSE_BUTTON_8
    /** 左マウスボタン。 */
    const val GLFW_MOUSE_BUTTON_LEFT: Short = GLFW_MOUSE_BUTTON_1
    /** 右マウスボタン。 */
    const val GLFW_MOUSE_BUTTON_RIGHT: Short = GLFW_MOUSE_BUTTON_2
    /** 中マウスボタン。 */
    const val GLFW_MOUSE_BUTTON_MIDDLE: Short = GLFW_MOUSE_BUTTON_3

    /** ウィンドウがフォーカスされている状態。 */
    const val GLFW_FOCUSED = 0x20001
    /** ウィンドウが表示されている状態。 */
    const val GLFW_VISIBLE = 0x20004
    /** マウスがウィンドウ上にある状態。 */
    const val GLFW_HOVERED = 0x2000B
}