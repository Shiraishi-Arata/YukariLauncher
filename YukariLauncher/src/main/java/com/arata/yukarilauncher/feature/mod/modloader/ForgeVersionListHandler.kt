package com.arata.yukarilauncher.feature.mod.modloader

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/** ForgeのMavenメタデータXMLをパースしてバージョン一覧を抽出するSAXハンドラ。 */
class ForgeVersionListHandler : DefaultHandler() {
    /** パース中のForgeバージョンリスト。 */
    private var mForgeVersions: MutableList<String>? = null
    /** 現在読み込み中のバージョン文字列バッファ。 */
    private var mCurrentVersion: StringBuilder? = null

    /** XMLドキュメントの開始時にバージョンリストを初期化する。 */
    override fun startDocument() {
        mForgeVersions = ArrayList()
    }

    /** XML要素内の文字データをバッファに追加する。 @param ch 文字配列 @param start 開始位置 @param length 長さ */
    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (mCurrentVersion != null) mCurrentVersion!!.append(ch, start, length)
    }

    /** XML要素の開始タグを処理する。"version"タグで新しいバッファを作成する。 @param uri 名前空間URI @param localName ローカル名 @param qName 修飾名 @param attributes 属性 */
    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        if (qName == "version") mCurrentVersion = StringBuilder()
    }

    /** XML要素の終了タグを処理する。"version"タグでバージョンをリストに追加する。 @param uri 名前空間URI @param localName ローカル名 @param qName 修飾名 */
    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (qName == "version") {
            val version = mCurrentVersion.toString()
            mForgeVersions!!.add(version)
            mCurrentVersion = null
        }
    }

    /** パース済みのForgeバージョンリストを取得する。 @return バージョン文字列のリスト */
    val versions: List<String>
        get() = mForgeVersions!!
}