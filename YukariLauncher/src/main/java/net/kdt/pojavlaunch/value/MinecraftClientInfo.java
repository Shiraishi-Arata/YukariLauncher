package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

/**
 * マインクラフトクライアントのダウンロード情報を保持するクラス。
 */
@Keep
public class MinecraftClientInfo {
    /** SHA1ハッシュ */
    public String sha1;
    /** ファイルサイズ */
    public int size;
    /** ダウンロードURL */
    public String url;
}
