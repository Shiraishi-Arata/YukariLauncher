package com.arata.yukarilauncher.feature.mod.modloader

import com.arata.yukarilauncher.utils.http.DownloadUtils
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode

/** OptiFineのダウンロード一覧ページをスクレイピングしてバージョン情報を抽出するクラス。 */
class OptiFineScraper : DownloadUtils.ParseCallback<OptiFineUtils.OptiFineVersions> {
    /** スクレイピング結果を格納するOptiFineバージョン情報。 */
    private val mOptiFineVersions = OptiFineUtils.OptiFineVersions()
    /** 現在処理中のバージョンリスト。 */
    private var mListInProgress: MutableList<OptiFineUtils.OptiFineVersion>? = null
    /** 現在のMinecraftバージョン。 */
    private var mMinecraftVersion: String? = null

    /** 初期化処理。MinecraftバージョンリストとOptiFineバージョンリストを初期化する。 */
    init {
        mOptiFineVersions.minecraftVersions = ArrayList()
        mOptiFineVersions.optifineVersions = ArrayList()
    }

    /** ダウンロードしたHTMLをパースしてOptiFineバージョン情報を取得する。 @param input ダウンロードページのHTML文字列 @return パース済みのOptiFineバージョン情報 @throws DownloadUtils.ParseException パースに失敗した場合 */
    override fun process(input: String): OptiFineUtils.OptiFineVersions {
        val htmlCleaner = HtmlCleaner()
        val tagNode = htmlCleaner.clean(input)
        traverseTagNode(tagNode)
        insertVersionContent(null)
        if (mOptiFineVersions.optifineVersions!!.isEmpty() || mOptiFineVersions.minecraftVersions!!.isEmpty()) throw DownloadUtils.ParseException(Exception())
        return mOptiFineVersions
    }

    /** タグノードを再帰的に走査し、Minecraftバージョンとダウンロード行を抽出する。 @param tagNode 走査対象のルートタグノード */
    fun traverseTagNode(tagNode: TagNode) {
        if (isDownloadLine(tagNode) && mMinecraftVersion != null) {
            traverseDownloadLine(tagNode)
        } else if (isMinecraftVersionTag(tagNode)) {
            insertVersionContent(tagNode)
        } else {
            for (tagNodes in tagNode.childTags) {
                traverseTagNode(tagNodes)
            }
        }
    }

    /** ノードがダウンロード行（tr.downloadLine）かどうかを判定する。 @param tagNode 判定対象のタグノード @return ダウンロード行の場合はtrue */
    private fun isDownloadLine(tagNode: TagNode): Boolean {
        return tagNode.name == "tr" &&
                tagNode.hasAttribute("class") &&
                tagNode.getAttributeByName("class").startsWith("downloadLine")
    }

    /** ノードがMinecraftバージョン見出し（h2）かどうかを判定する。 @param tagNode 判定対象のタグノード @return Minecraftバージョン見出しの場合はtrue */
    private fun isMinecraftVersionTag(tagNode: TagNode): Boolean {
        return tagNode.name == "h2" &&
                tagNode.text.toString().startsWith("Minecraft ")
    }

    /** ダウンロード行からOptiFineバージョン情報を抽出する。 @param tagNode ダウンロード行のタグノード */
    private fun traverseDownloadLine(tagNode: TagNode) {
        val optiFineVersion = OptiFineUtils.OptiFineVersion()
        optiFineVersion.minecraftVersion = mMinecraftVersion
        for (subNode in tagNode.childTags) {
            if (subNode.name != "td") continue
            when (subNode.getAttributeByName("class")) {
                "colFile" -> optiFineVersion.versionName = subNode.text.toString()
                "colMirror" -> optiFineVersion.downloadUrl = getLinkHref(subNode)
            }
        }
        mListInProgress!!.add(optiFineVersion)
    }

    /** 親ノードから最初のaタグのhref属性を取得する。 @param parent 検索対象の親タグノード @return href属性の値（http→httpsに変換済み）、見つからない場合はnull */
    private fun getLinkHref(parent: TagNode): String? {
        for (subNode in parent.childTags) {
            if (subNode.name == "a" && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://")
            }
        }
        return null
    }

    /** バージョンコンテキストを切り替え、完了したリストを保存する。 @param tagNode 新しいMinecraftバージョンのタグノード（nullの場合は最終フラッシュ） */
    private fun insertVersionContent(tagNode: TagNode?) {
        if (mListInProgress != null && mMinecraftVersion != null) {
            mOptiFineVersions.minecraftVersions!!.add(mMinecraftVersion!!)
            mOptiFineVersions.optifineVersions!!.add(mListInProgress!!)
        }
        if (tagNode != null) {
            mMinecraftVersion = tagNode.text.toString()
            mListInProgress = ArrayList()
        }
    }
}