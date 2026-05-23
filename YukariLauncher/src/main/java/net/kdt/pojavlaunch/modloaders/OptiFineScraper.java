package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.List;

public class OptiFineScraper implements DownloadUtils.ParseCallback<OptiFineUtils.OptiFineVersions> {
    private final OptiFineUtils.OptiFineVersions mOptiFineVersions;
    private List<OptiFineUtils.OptiFineVersion> mListInProgress;
    private String mMinecraftVersion;

    /**
     * OptiFineScraperを構築します。
     */
    public OptiFineScraper() {
        mOptiFineVersions = new OptiFineUtils.OptiFineVersions();
        mOptiFineVersions.minecraftVersions = new ArrayList<>();
        mOptiFineVersions.optifineVersions = new ArrayList<>();
    }

    /**
     * OptiFineのダウンロードページをパースしてバージョン情報を取得します。
     * @param input HTMLページの内容
     * @return パースされたOptiFineバージョン情報
     * @throws DownloadUtils.ParseException パースに失敗した場合
     */
    @Override
    public OptiFineUtils.OptiFineVersions process(String input) throws DownloadUtils.ParseException {
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        TagNode tagNode = htmlCleaner.clean(input);
        traverseTagNode(tagNode);
        insertVersionContent(null);
        if(mOptiFineVersions.optifineVersions.isEmpty() || mOptiFineVersions.minecraftVersions.isEmpty()) throw new DownloadUtils.ParseException(null);
        return mOptiFineVersions;
    }

    /**
     * HTMLタグノードを再帰的に走査します。
     */
    public void traverseTagNode(TagNode tagNode) {
        if(isDownloadLine(tagNode) && mMinecraftVersion != null) {
            traverseDownloadLine(tagNode);
        } else if(isMinecraftVersionTag(tagNode)) {
           insertVersionContent(tagNode);
        } else {
            for(TagNode tagNodes : tagNode.getChildTags()) {
                traverseTagNode(tagNodes);
            }
        }
    }

    /**
     * ダウンロード行のタグかどうかを判定します。
     */
    private boolean isDownloadLine(TagNode tagNode) {
        return tagNode.getName().equals("tr") &&
                tagNode.hasAttribute("class") &&
                tagNode.getAttributeByName("class").startsWith("downloadLine");
    }

    /**
     * Minecraftバージョンタグかどうかを判定します。
     */
    private boolean isMinecraftVersionTag(TagNode tagNode) {
        return tagNode.getName().equals("h2") &&
                tagNode.getText().toString().startsWith("Minecraft ");
    }

    /**
     * ダウンロード行を走査してOptiFineバージョン情報を抽出します。
     */
    private void traverseDownloadLine(TagNode tagNode) {
        OptiFineUtils.OptiFineVersion optiFineVersion = new OptiFineUtils.OptiFineVersion();
        optiFineVersion.minecraftVersion = mMinecraftVersion;
        for(TagNode subNode : tagNode.getChildTags()) {
            if(!subNode.getName().equals("td")) continue;
            switch(subNode.getAttributeByName("class")) {
                case "colFile":
                    optiFineVersion.versionName = subNode.getText().toString();
                    break;
                case "colMirror":
                    optiFineVersion.downloadUrl = getLinkHref(subNode);
            }
        }
        mListInProgress.add(optiFineVersion);
    }

    /**
     * 親ノードからリンク先のhref属性を取得します。
     */
    private String getLinkHref(TagNode parent) {
        for(TagNode subNode : parent.getChildTags()) {
            if(subNode.getName().equals("a") && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://");
            }
        }
        return null;
    }

    /**
     * 現在のMinecraftバージョンの内容をリストに追加し、新しいバージョンの処理を開始します。
     */
    private void insertVersionContent(TagNode tagNode) {
        if(mListInProgress != null && mMinecraftVersion != null) {
            mOptiFineVersions.minecraftVersions.add(mMinecraftVersion);
            mOptiFineVersions.optifineVersions.add(mListInProgress);
        }
        if(tagNode != null) {
            mMinecraftVersion = tagNode.getText().toString();
            mListInProgress = new ArrayList<>();
        }
    }
}
