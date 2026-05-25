package com.arata.yukarilauncher.feature.mod.modloader

import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.HtmlNode
import org.htmlcleaner.TagNode
import org.htmlcleaner.TagNodeVisitor
import java.net.URL

/** OptiFineのダウンロードページをスクレイピングして実際のダウンロードURLを取得するクラス。 */
class OFDownloadPageScraper : TagNodeVisitor {
    /** 抽出されたダウンロードURL。 */
    private var mDownloadFullUrl: String? = null

    companion object {
        /** 指定されたURLからダウンロードリンクを抽出して実行する。 @param urlInput OptiFineダウンロードページのURL @return 実際のダウンロードURL */
        fun run(urlInput: String): String {
            return OFDownloadPageScraper().runInner(urlInput)
        }
    }

    /** 内部実行メソッド。HTMLをパースしてダウンロードURLを抽出する。 @param url スクレイピング対象のURL @return 実際のダウンロードURL */
    private fun runInner(url: String): String {
        val htmlCleaner = HtmlCleaner()
        htmlCleaner.clean(URL(url)).traverse(this)
        return mDownloadFullUrl!!
    }

    /** タグノードを訪問し、ダウンロードリンクを検出する。 @param parentNode 親ノード @param htmlNode 現在のHTMLノード @return 走査を続行する場合はtrue */
    override fun visit(parentNode: TagNode?, htmlNode: HtmlNode): Boolean {
        if (isDownloadUrl(parentNode, htmlNode)) {
            val tagNode = htmlNode as TagNode
            var href = tagNode.getAttributeByName("href")
            if (!href.startsWith("https://")) href = "https://optifine.net/$href"
            mDownloadFullUrl = href
            return false
        }
        return true
    }

    /** ノードがダウンロードURLリンクかどうかを判定する。 @param parentNode 親ノード @param htmlNode 判定対象のHTMLノード @return ダウンロードリンクの場合はtrue */
    fun isDownloadUrl(parentNode: TagNode?, htmlNode: HtmlNode): Boolean {
        if (htmlNode !is TagNode) return false
        if (parentNode == null) return false
        val tagNode = htmlNode
        if (!(parentNode.name == "span"
                    && "Download" == parentNode.getAttributeByName("id"))) return false
        return tagNode.name == "a" &&
                "onDownload()" == tagNode.getAttributeByName("onclick")
    }
}
