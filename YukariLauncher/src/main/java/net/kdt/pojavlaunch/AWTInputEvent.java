package net.kdt.pojavlaunch;

/*
     * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
     *
     * This code is free software; you can redistribute it and/or modify it
     * under the terms of the GNU General Public License version 2 only, as
     * published by the Free Software Foundation.  Oracle designates this
     * particular file as subject to the "Classpath" exception as provided
     * by Oracle in the LICENSE file that accompanied this code.
     *
     * This code is distributed in the hope that it will be useful, but WITHOUT
     * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
     * version 2 for more details (a copy is included in the LICENSE file that
     * accompanied this code).
     *
     * You should have received a copy of the GNU General Public License version
     * 2 along with this work; if not, write to the Free Software Foundation,
     * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
     *
     * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
     * or visit www.oracle.com if you need additional information or have any
     * questions.
     */

/**
 * AWT入力イベントの定数定義クラス。AWTのInputEvent/KeyEvent/MouseEventの定数を再定義します。
 */
@SuppressWarnings("unused")
public class AWTInputEvent {
// InputEvent
    /**
     * イベント発生時にShiftキーが押されていたことを示すフラグ。
     */
    public static final int SHIFT_MASK          = 1; //first bit

    /**
     * イベント発生時にControlキーが押されていたことを示すフラグ。
     */
    public static final int CTRL_MASK           = 1 << 1;

    /**
     * イベント発生時にMetaキーが押されていたことを示すフラグ。マウスイベントでは、右ボタンが押された／解放されたことを示します。
     */
    public static final int META_MASK           = 1 << 2;

    /**
     * イベント発生時にAltキーが押されていたことを示すフラグ。マウスイベントでは、中ボタンが押された／解放されたことを示します。
     */
    public static final int ALT_MASK            = 1 << 3;

    /**
     * AltGraphキー修飾子定数。
     */
    public static final int ALT_GRAPH_MASK = 1 << 5;

    /**
     * マウスボタン1修飾子定数。BUTTON1_DOWN_MASKを使用することを推奨します。
     */
    public static final int BUTTON1_MASK = 1 << 4;

    /**
     * マウスボタン2修飾子定数。BUTTON2_DOWN_MASKを使用することを推奨します。BUTTON2_MASKはALT_MASKと同じ値です。
     */
    public static final int BUTTON2_MASK = ALT_MASK;

    /**
     * マウスボタン3修飾子定数。BUTTON3_DOWN_MASKを使用することを推奨します。BUTTON3_MASKはMETA_MASKと同じ値です。
     */
    public static final int BUTTON3_MASK = META_MASK;

    /**
     * Shiftキー拡張修飾子定数。
     * @since 1.4
     */
    public static final int SHIFT_DOWN_MASK = 1 << 6;

    /**
     * Controlキー拡張修飾子定数。
     * @since 1.4
     */
    public static final int CTRL_DOWN_MASK = 1 << 7;

    /**
     * Metaキー拡張修飾子定数。
     * @since 1.4
     */
    public static final int META_DOWN_MASK = 1 << 8;

    /**
     * Altキー拡張修飾子定数。
     * @since 1.4
     */
    public static final int ALT_DOWN_MASK = 1 << 9;

    /**
     * マウスボタン1拡張修飾子定数。
     * @since 1.4
     */
    public static final int BUTTON1_DOWN_MASK = 1 << 10;

    /**
     * マウスボタン2拡張修飾子定数。
     * @since 1.4
     */
    public static final int BUTTON2_DOWN_MASK = 1 << 11;

    /**
     * マウスボタン3拡張修飾子定数。
     * @since 1.4
     */
    public static final int BUTTON3_DOWN_MASK = 1 << 12;

    /**
     * AltGraphキー拡張修飾子定数。
     * @since 1.4
     */
    public static final int ALT_GRAPH_DOWN_MASK = 1 << 13;


// KeyEvent
    /**
     * キーイベントに使用されるID範囲の最初の番号。
     */
    public static final int KEY_FIRST = 400;

    /**
     * キーイベントに使用されるID範囲の最後の番号。
     */
    public static final int KEY_LAST  = 402;

    /**
     * 「キー入力」イベント。このイベントは文字が入力されたときに生成されます。
     */
    public static final int KEY_TYPED = KEY_FIRST;

    /**
     * 「キー押下」イベント。キーが押されたときに生成されます。
     */
    public static final int KEY_PRESSED = 1 + KEY_FIRST; //Event.KEY_PRESS

    /**
     * 「キー解放」イベント。キーが離されたときに生成されます。
     */
    public static final int KEY_RELEASED = 2 + KEY_FIRST; //Event.KEY_RELEASE

    /* 仮想キーコード。 */

    public static final int VK_ENTER          = '\n';
    public static final int VK_BACK_SPACE     = '\b';
    public static final int VK_TAB            = '\t';
    public static final int VK_CANCEL         = 0x03;
    public static final int VK_CLEAR          = 0x0C;
    public static final int VK_SHIFT          = 0x10;
    public static final int VK_CONTROL        = 0x11;
    public static final int VK_ALT            = 0x12;
    public static final int VK_PAUSE          = 0x13;
    public static final int VK_CAPS_LOCK      = 0x14;
    public static final int VK_ESCAPE         = 0x1B;
    public static final int VK_SPACE          = 0x20;
    public static final int VK_PAGE_UP        = 0x21;
    public static final int VK_PAGE_DOWN      = 0x22;
    public static final int VK_END            = 0x23;
    public static final int VK_HOME           = 0x24;

    /**
     * 非テンキーの左矢印キー用定数。
     * @see #VK_KP_LEFT
     */
    public static final int VK_LEFT           = 0x25;

    /**
     * 非テンキーの上矢印キー用定数。
     * @see #VK_KP_UP
     */
    public static final int VK_UP             = 0x26;

    /**
     * 非テンキーの右矢印キー用定数。
     * @see #VK_KP_RIGHT
     */
    public static final int VK_RIGHT          = 0x27;

    /**
     * 非テンキーの下矢印キー用定数。
     * @see #VK_KP_DOWN
     */
    public static final int VK_DOWN           = 0x28;

    /**
     * カンマキー「,」用定数。
     */
    public static final int VK_COMMA          = 0x2C;

    /**
     * マイナスキー「-」用定数。
     * @since 1.2
     */
    public static final int VK_MINUS          = 0x2D;

    /**
     * ピリオドキー「.」用定数。
     */
    public static final int VK_PERIOD         = 0x2E;

    /**
     * スラッシュキー「/」用定数。
     */
    public static final int VK_SLASH          = 0x2F;

    /** VK_0〜VK_9はASCIIの'0'〜'9'と同じ（0x30 - 0x39） */
    public static final int VK_0              = 0x30;
    public static final int VK_1              = 0x31;
    public static final int VK_2              = 0x32;
    public static final int VK_3              = 0x33;
    public static final int VK_4              = 0x34;
    public static final int VK_5              = 0x35;
    public static final int VK_6              = 0x36;
    public static final int VK_7              = 0x37;
    public static final int VK_8              = 0x38;
    public static final int VK_9              = 0x39;

    /**
     * セミコロンキー「;」用定数。
     */
    public static final int VK_SEMICOLON      = 0x3B;

    /**
     * イコールキー「=」用定数。
     */
    public static final int VK_EQUALS         = 0x3D;

    /** VK_A〜VK_ZはASCIIの'A'〜'Z'と同じ（0x41 - 0x5A） */
    public static final int VK_A              = 0x41;
    public static final int VK_B              = 0x42;
    public static final int VK_C              = 0x43;
    public static final int VK_D              = 0x44;
    public static final int VK_E              = 0x45;
    public static final int VK_F              = 0x46;
    public static final int VK_G              = 0x47;
    public static final int VK_H              = 0x48;
    public static final int VK_I              = 0x49;
    public static final int VK_J              = 0x4A;
    public static final int VK_K              = 0x4B;
    public static final int VK_L              = 0x4C;
    public static final int VK_M              = 0x4D;
    public static final int VK_N              = 0x4E;
    public static final int VK_O              = 0x4F;
    public static final int VK_P              = 0x50;
    public static final int VK_Q              = 0x51;
    public static final int VK_R              = 0x52;
    public static final int VK_S              = 0x53;
    public static final int VK_T              = 0x54;
    public static final int VK_U              = 0x55;
    public static final int VK_V              = 0x56;
    public static final int VK_W              = 0x57;
    public static final int VK_X              = 0x58;
    public static final int VK_Y              = 0x59;
    public static final int VK_Z              = 0x5A;

    /**
     * 開き括弧キー「[」用定数。
     */
    public static final int VK_OPEN_BRACKET   = 0x5B;

    /**
     * バックスラッシュキー「\」用定数。
     */
    public static final int VK_BACK_SLASH     = 0x5C;

    /**
     * 閉じ括弧キー「]」用定数。
     */
    public static final int VK_CLOSE_BRACKET  = 0x5D;

    public static final int VK_NUMPAD0        = 0x60;
    public static final int VK_NUMPAD1        = 0x61;
    public static final int VK_NUMPAD2        = 0x62;
    public static final int VK_NUMPAD3        = 0x63;
    public static final int VK_NUMPAD4        = 0x64;
    public static final int VK_NUMPAD5        = 0x65;
    public static final int VK_NUMPAD6        = 0x66;
    public static final int VK_NUMPAD7        = 0x67;
    public static final int VK_NUMPAD8        = 0x68;
    public static final int VK_NUMPAD9        = 0x69;
    public static final int VK_MULTIPLY       = 0x6A;
    public static final int VK_ADD            = 0x6B;

    /**
     * この定数は廃止されており、後方互換性のためにのみ含まれています。
     * @see #VK_SEPARATOR
     */
    public static final int VK_SEPARATER      = 0x6C;

    /**
     * テンキー区切りキー用定数。
     * @since 1.4
     */
    public static final int VK_SEPARATOR      = VK_SEPARATER;

    public static final int VK_SUBTRACT       = 0x6D;
    public static final int VK_DECIMAL        = 0x6E;
    public static final int VK_DIVIDE         = 0x6F;
    public static final int VK_DELETE         = 0x7F; /* ASCII DEL */
    public static final int VK_NUM_LOCK       = 0x90;
    public static final int VK_SCROLL_LOCK    = 0x91;

    /** F1ファンクションキー用定数。 */
    public static final int VK_F1             = 0x70;

    /** F2ファンクションキー用定数。 */
    public static final int VK_F2             = 0x71;

    /** F3ファンクションキー用定数。 */
    public static final int VK_F3             = 0x72;

    /** F4ファンクションキー用定数。 */
    public static final int VK_F4             = 0x73;

    /** F5ファンクションキー用定数。 */
    public static final int VK_F5             = 0x74;

    /** F6ファンクションキー用定数。 */
    public static final int VK_F6             = 0x75;

    /** F7ファンクションキー用定数。 */
    public static final int VK_F7             = 0x76;

    /** F8ファンクションキー用定数。 */
    public static final int VK_F8             = 0x77;

    /** F9ファンクションキー用定数。 */
    public static final int VK_F9             = 0x78;

    /** F10ファンクションキー用定数。 */
    public static final int VK_F10            = 0x79;

    /** F11ファンクションキー用定数。 */
    public static final int VK_F11            = 0x7A;

    /** F12ファンクションキー用定数。 */
    public static final int VK_F12            = 0x7B;

    /**
     * F13ファンクションキー用定数。
     * @since 1.2
     */
    /* F13 - F24 はIBM 3270キーボードで使用されます。ランダムな範囲の定数を使用します。 */
    public static final int VK_F13            = 0xF000;

    /**
     * F14ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F14            = 0xF001;

    /**
     * F15ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F15            = 0xF002;

    /**
     * F16ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F16            = 0xF003;

    /**
     * F17ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F17            = 0xF004;

    /**
     * F18ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F18            = 0xF005;

    /**
     * F19ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F19            = 0xF006;

    /**
     * F20ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F20            = 0xF007;

    /**
     * F21ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F21            = 0xF008;

    /**
     * F22ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F22            = 0xF009;

    /**
     * F23ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F23            = 0xF00A;

    /**
     * F24ファンクションキー用定数。
     * @since 1.2
     */
    public static final int VK_F24            = 0xF00B;

    public static final int VK_PRINTSCREEN    = 0x9A;
    public static final int VK_INSERT         = 0x9B;
    public static final int VK_HELP           = 0x9C;
    public static final int VK_META           = 0x9D;

    public static final int VK_BACK_QUOTE     = 0xC0;
    public static final int VK_QUOTE          = 0xDE;

    /**
     * テンキーの上矢印キー用定数。
     * @see #VK_UP
     * @since 1.2
     */
    public static final int VK_KP_UP          = 0xE0;

    /**
     * テンキーの下矢印キー用定数。
     * @see #VK_DOWN
     * @since 1.2
     */
    public static final int VK_KP_DOWN        = 0xE1;

    /**
     * テンキーの左矢印キー用定数。
     * @see #VK_LEFT
     * @since 1.2
     */
    public static final int VK_KP_LEFT        = 0xE2;

    /**
     * テンキーの右矢印キー用定数。
     * @see #VK_RIGHT
     * @since 1.2
     */
    public static final int VK_KP_RIGHT       = 0xE3;

    /* 欧州キーボード用 */
    /** @since 1.2 */
    public static final int VK_DEAD_GRAVE               = 0x80;
    /** @since 1.2 */
    public static final int VK_DEAD_ACUTE               = 0x81;
    /** @since 1.2 */
    public static final int VK_DEAD_CIRCUMFLEX          = 0x82;
    /** @since 1.2 */
    public static final int VK_DEAD_TILDE               = 0x83;
    /** @since 1.2 */
    public static final int VK_DEAD_MACRON              = 0x84;
    /** @since 1.2 */
    public static final int VK_DEAD_BREVE               = 0x85;
    /** @since 1.2 */
    public static final int VK_DEAD_ABOVEDOT            = 0x86;
    /** @since 1.2 */
    public static final int VK_DEAD_DIAERESIS           = 0x87;
    /** @since 1.2 */
    public static final int VK_DEAD_ABOVERING           = 0x88;
    /** @since 1.2 */
    public static final int VK_DEAD_DOUBLEACUTE         = 0x89;
    /** @since 1.2 */
    public static final int VK_DEAD_CARON               = 0x8a;
    /** @since 1.2 */
    public static final int VK_DEAD_CEDILLA             = 0x8b;
    /** @since 1.2 */
    public static final int VK_DEAD_OGONEK              = 0x8c;
    /** @since 1.2 */
    public static final int VK_DEAD_IOTA                = 0x8d;
    /** @since 1.2 */
    public static final int VK_DEAD_VOICED_SOUND        = 0x8e;
    /** @since 1.2 */
    public static final int VK_DEAD_SEMIVOICED_SOUND    = 0x8f;

    /** @since 1.2 */
    public static final int VK_AMPERSAND                = 0x96;
    /** @since 1.2 */
    public static final int VK_ASTERISK                 = 0x97;
    /** @since 1.2 */
    public static final int VK_QUOTEDBL                 = 0x98;
    /** @since 1.2 */
    public static final int VK_LESS                     = 0x99;

    /** @since 1.2 */
    public static final int VK_GREATER                  = 0xa0;
    /** @since 1.2 */
    public static final int VK_BRACELEFT                = 0xa1;
    /** @since 1.2 */
    public static final int VK_BRACERIGHT               = 0xa2;

    /**
     * 「@」キー用定数。
     * @since 1.2
     */
    public static final int VK_AT                       = 0x0200;

    /**
     * 「:」キー用定数。
     * @since 1.2
     */
    public static final int VK_COLON                    = 0x0201;

    /**
     * 「^」キー用定数。
     * @since 1.2
     */
    public static final int VK_CIRCUMFLEX               = 0x0202;

    /**
     * 「$」キー用定数。
     * @since 1.2
     */
    public static final int VK_DOLLAR                   = 0x0203;

    /**
     * ユーロ通貨記号キー用定数。
     * @since 1.2
     */
    public static final int VK_EURO_SIGN                = 0x0204;

    /**
     * 「!」キー用定数。
     * @since 1.2
     */
    public static final int VK_EXCLAMATION_MARK         = 0x0205;

    /**
     * 逆感嘆符キー用定数。
     * @since 1.2
     */
    public static final int VK_INVERTED_EXCLAMATION_MARK = 0x0206;

    /**
     * 「(」キー用定数。
     * @since 1.2
     */
    public static final int VK_LEFT_PARENTHESIS         = 0x0207;

    /**
     * 「#」キー用定数。
     * @since 1.2
     */
    public static final int VK_NUMBER_SIGN              = 0x0208;

    /**
     * 「+」キー用定数。
     * @since 1.2
     */
    public static final int VK_PLUS                     = 0x0209;

    /**
     * 「)」キー用定数。
     * @since 1.2
     */
    public static final int VK_RIGHT_PARENTHESIS        = 0x020A;

    /**
     * 「_」キー用定数。
     * @since 1.2
     */
    public static final int VK_UNDERSCORE               = 0x020B;

    /**
     * Microsoft Windows「Windows」キー用定数。左右のバージョンのキーの両方に使用されます。
     * @since 1.5
     */
    public static final int VK_WINDOWS                  = 0x020C;

    /**
     * Microsoft Windows コンテキストメニューキー用定数。
     * @since 1.5
     */
    public static final int VK_CONTEXT_MENU             = 0x020D;

    /* アジアのキーボードの入力メソッドサポート用 */

    /* Microsoft Windows APIにリストされているが意味は不明 */
    public static final int VK_FINAL                    = 0x0018;

    /** 変換機能キー用定数。 */
    /* 日本PC 106キーボード、日本Solarisキーボード：変換 */
    public static final int VK_CONVERT                  = 0x001C;

    /** 無変換機能キー用定数。 */
    /* 日本PC 106キーボード：無変換 */
    public static final int VK_NONCONVERT               = 0x001D;

    /** 確定機能キー用定数。 */
    /* 日本Solarisキーボード：確定 */
    public static final int VK_ACCEPT                   = 0x001E;

    /* Microsoft Windows APIにリストされているが意味は不明 */
    public static final int VK_MODECHANGE               = 0x001F;

    /* Microsoft WindowsおよびSolarisではVK_KANA_LOCKに置き換えられました。
       他のプラットフォームではまだ使用される可能性があります。 */
    public static final int VK_KANA                     = 0x0015;

    /* Microsoft WindowsおよびSolarisではVK_INPUT_METHOD_ON_OFFに置き換えられました。
       他のプラットフォームではまだ使用される可能性があります。 */
    public static final int VK_KANJI                    = 0x0019;

    /**
     * 英数字機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：英数 */
    public static final int VK_ALPHANUMERIC             = 0x00F0;

    /**
     * カタカナ機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：カタカナ */
    public static final int VK_KATAKANA                 = 0x00F1;

    /**
     * ひらがな機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：ひらがな */
    public static final int VK_HIRAGANA                 = 0x00F2;

    /**
     * 全角文字機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：全角 */
    public static final int VK_FULL_WIDTH               = 0x00F3;

    /**
     * 半角文字機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：半角 */
    public static final int VK_HALF_WIDTH               = 0x00F4;

    /**
     * ローマ字機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード：ローマ字 */
    public static final int VK_ROMAN_CHARACTERS         = 0x00F5;

    /**
     * 全候補機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード - VK_CONVERT + ALT：全候補 */
    public static final int VK_ALL_CANDIDATES           = 0x0100;

    /**
     * 前候補機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード - VK_CONVERT + SHIFT：前候補 */
    public static final int VK_PREVIOUS_CANDIDATE       = 0x0101;

    /**
     * コード入力機能キー用定数。
     * @since 1.2
     */
    /* 日本PC 106キーボード - VK_ALPHANUMERIC + ALT：漢字番号 */
    public static final int VK_CODE_INPUT               = 0x0102;

    /**
     * 日本語カタカナ機能キー用定数。このキーは日本語入力メソッドに切り替え、カタカナ入力モードを選択します。
     * @since 1.2
     */
    /* 日本Macintoshキーボード - VK_JAPANESE_HIRAGANA + SHIFT */
    public static final int VK_JAPANESE_KATAKANA        = 0x0103;

    /**
     * 日本語ひらがな機能キー用定数。このキーは日本語入力メソッドに切り替え、ひらがな入力モードを選択します。
     * @since 1.2
     */
    /* 日本Macintoshキーボード */
    public static final int VK_JAPANESE_HIRAGANA        = 0x0104;

    /**
     * 日本語ローマ字機能キー用定数。このキーは日本語入力メソッドに切り替え、ローマ字直接入力モードを選択します。
     * @since 1.2
     */
    /* 日本Macintoshキーボード */
    public static final int VK_JAPANESE_ROMAN           = 0x0105;

    /**
     * ロック用かな機能キー用定数。このキーはキーボードをかなレイアウトにロックします。
     * @since 1.3
     */
    /* 特別なWindowsドライバーを使用する日本PC 106キーボード - 英数 + Control; 日本Solarisキーボード: かな */
    public static final int VK_KANA_LOCK                = 0x0106;

    /**
     * 入力メソッドオン/オフキー用定数。
     * @since 1.3
     */
    /* 日本PC 106キーボード: 漢字。日本Solarisキーボード: にほんご */
    public static final int VK_INPUT_METHOD_ON_OFF      = 0x0107;

    /* Sunキーボード用 */
    /** @since 1.2 */
    public static final int VK_CUT                      = 0xFFD1;
    /** @since 1.2 */
    public static final int VK_COPY                     = 0xFFCD;
    /** @since 1.2 */
    public static final int VK_PASTE                    = 0xFFCF;
    /** @since 1.2 */
    public static final int VK_UNDO                     = 0xFFCB;
    /** @since 1.2 */
    public static final int VK_AGAIN                    = 0xFFC9;
    /** @since 1.2 */
    public static final int VK_FIND                     = 0xFFD0;
    /** @since 1.2 */
    public static final int VK_PROPS                    = 0xFFCA;
    /** @since 1.2 */
    public static final int VK_STOP                     = 0xFFC8;

    /**
     * Compose機能キー用定数。
     * @since 1.2
     */
    public static final int VK_COMPOSE                  = 0xFF20;

    /**
     * AltGraph機能キー用定数。
     * @since 1.2
     */
    public static final int VK_ALT_GRAPH                = 0xFF7E;

    /**
     * Beginキー用定数。
     * @since 1.5
     */
    public static final int VK_BEGIN                    = 0xFF58;

    /**
     * keyCodeが不明であることを示す値。KEY_TYPEDイベントにはkeyCode値がありません。代わりにこの値が使用されます。
     */
    public static final int VK_UNDEFINED      = 0x0;

    /**
     * KEY_PRESSEDおよびKEY_RELEASEDイベントで、有効なUnicode文字にマップされない場合にkeyCharとして使用されます。
     */
    public static final char CHAR_UNDEFINED   = 0xFFFF;

    /**
     * keyLocationが不定または無関係であることを示す定数。
     * KEY_TYPEDイベントにはkeyLocationがありません。代わりにこの値が使用されます。
     * @since 1.4
     */
    public static final int KEY_LOCATION_UNKNOWN  = 0;

    /**
     * 押された／解放されたキーが左右の区別がない、またはテンキーから発生していないことを示す定数。
     * @since 1.4
     */
    public static final int KEY_LOCATION_STANDARD = 1;

    /**
     * 押された／解放されたキーが左側のキー位置にあることを示す定数（このキーには複数の可能な位置があります）。
     * @since 1.4
     */
    public static final int KEY_LOCATION_LEFT     = 2;

    /**
     * 押された／解放されたキーが右側のキー位置にあることを示す定数（このキーには複数の可能な位置があります）。
     * @since 1.4
     */
    public static final int KEY_LOCATION_RIGHT    = 3;

    /**
     * キーイベントがテンキーまたはテンキーに対応する仮想キーから発生したことを示す定数。
     * @since 1.4
     */
    public static final int KEY_LOCATION_NUMPAD   = 4;


// MOUSE
    /**
     * マウスイベントに使用されるID範囲の最初の番号。
     */
    public static final int MOUSE_FIRST         = 500;

    /**
     * マウスイベントに使用されるID範囲の最後の番号。
     */
    public static final int MOUSE_LAST          = 507;

    /**
     * 「マウスクリック」イベント。マウスボタンが押されて離されたときに発生します。
     */
    public static final int MOUSE_CLICKED = MOUSE_FIRST;

    /**
     * 「マウス押下」イベント。マウスボタンが押されたときに発生します。
     */
    public static final int MOUSE_PRESSED = 1 + MOUSE_FIRST; //Event.MOUSE_DOWN

    /**
     * 「マウス解放」イベント。マウスボタンが離されたときに発生します。
     */
    public static final int MOUSE_RELEASED = 2 + MOUSE_FIRST; //Event.MOUSE_UP

    /**
     * 「マウス移動」イベント。マウス位置が変更されたときに発生します。
     */
    public static final int MOUSE_MOVED = 3 + MOUSE_FIRST; //Event.MOUSE_MOVE

    /**
     * 「マウス進入」イベント。マウスカーソルがコンポーネントの隠されていない部分に入ったときに発生します。
     */
    public static final int MOUSE_ENTERED = 4 + MOUSE_FIRST; //Event.MOUSE_ENTER

    /**
     * 「マウス退出」イベント。マウスカーソルがコンポーネントの隠されていない部分から出たときに発生します。
     */
    public static final int MOUSE_EXITED = 5 + MOUSE_FIRST; //Event.MOUSE_EXIT

    /**
     * 「マウスドラッグ」イベント。マウスボタンが押された状態でマウス位置が変更されたときに発生します。
     */
    public static final int MOUSE_DRAGGED = 6 + MOUSE_FIRST; //Event.MOUSE_DRAG

    /**
     * 「マウスホイール」イベント。ホイールが回されたときに発生します。
     * @since 1.4
     */
    public static final int MOUSE_WHEEL = 7 + MOUSE_FIRST;

    /**
     * マウスボタンなしを示します。getButtonで使用されます。
     * @since 1.4
     */
    public static final int NOBUTTON = 0;

    /**
     * マウスボタン#1を示します。getButtonで使用されます。
     * @since 1.4
     */
    public static final int BUTTON1 = 1;

    /**
     * マウスボタン#2を示します。getButtonで使用されます。
     * @since 1.4
     */
    public static final int BUTTON2 = 2;

    /**
     * マウスボタン#3を示します。getButtonで使用されます。
     * @since 1.4
     */
    public static final int BUTTON3 = 3;
}
