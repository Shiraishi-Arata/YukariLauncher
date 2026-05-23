package net.kdt.pojavlaunch.modloaders;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * ForgeのメタデータXMLをパースするSAXハンドラー
 */
public class ForgeVersionListHandler extends DefaultHandler {
    private List<String> mForgeVersions;
    private StringBuilder mCurrentVersion = null;

    /**
     * ドキュメント解析開始時にバージョンリストを初期化します。
     */
    @Override
    public void startDocument() throws SAXException {
        mForgeVersions = new ArrayList<>();
    }

    /**
     * XML要素内のテキストを収集します。
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(mCurrentVersion != null) mCurrentVersion.append(ch, start, length);
    }

    /**
     & version要素の開始時に新しいStringBuilderを作成します。
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(qName.equals("version")) mCurrentVersion = new StringBuilder();
    }

    /**
     * version要素の終了時に収集したバージョンをリストに追加します。
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("version")) {
            String version = mCurrentVersion.toString();
            mForgeVersions.add(version);
            mCurrentVersion = null;
        }
    }

    /**
     * 解析結果のバージョンリストを返します。
     */
    public List<String> getVersions() {
        return mForgeVersions;
    }
}
