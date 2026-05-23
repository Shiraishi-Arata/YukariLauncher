package net.kdt.pojavlaunch.value.launcherprofiles;

import androidx.annotation.Keep;

/**
 * ランチャープロファイルの認証データベースエントリを保持するクラス。
 */
@Keep
public class MinecraftAuthenticationDatabase {
    /** アクセストークン */
    public String accessToken;
    /** 表示名 */
    public String displayName;
    /** ユーザー名 */
    public String username;
    /** UUID */
    public String uuid;
}
