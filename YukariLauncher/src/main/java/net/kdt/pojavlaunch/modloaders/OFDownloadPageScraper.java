package net.kdt.pojavlaunch.modloaders;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;

import java.io.IOException;
import java.net.URL;

/**
 * OptiFineのダウンロードページをスクレイピングして実際のダウンロードURLを取得します。
 */
public class OFDownloadPageScraper implements TagNodeVisitor {
    /**
     * 指定されたURLから実際のダウンロードURLを抽出します。
     * @param urlInput OptiFineのダウンロードページURL
     * @return 実際のダウンロードURL
     * @throws IOException I/Oエラーが発生した場合
     */
    public static String run(String urlInput) throws IOException{
        return new OFDownloadPageScraper().runInner(urlInput);
    }

    private String mDownloadFullUrl;

    /**
     * 内部的なスクレイピング処理を実行します。
     */
    private String runInner(String url) throws IOException {
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        htmlCleaner.clean(new URL(url)).traverse(this);
        return mDownloadFullUrl;
    }

    /**
     * タグノードを訪問し、ダウンロードURLを検出したら抽出します。
     */
    @Override
    public boolean visit(TagNode parentNode, HtmlNode htmlNode) {
        if(isDownloadUrl(parentNode, htmlNode)) {
            TagNode tagNode = (TagNode) htmlNode;
            String href = tagNode.getAttributeByName("href");
            if(!href.startsWith("https://")) href = "https://optifine.net/"+href;
            this.mDownloadFullUrl = href;
            return false;
        }
        return true;
    }

    /**
     * 指定されたノードがダウンロードURLかどうかを判定します。
     */
    public boolean isDownloadUrl(TagNode parentNode, HtmlNode htmlNode) {
        if(!(htmlNode instanceof TagNode)) return false;
        if(parentNode == null) return false;
        TagNode tagNode = (TagNode) htmlNode;
        if(!(parentNode.getName().equals("span")
            && "Download".equals(parentNode.getAttributeByName("id")))) return false;
        return tagNode.getName().equals("a") &&
                "onDownload()".equals(tagNode.getAttributeByName("onclick"));
    }
}
