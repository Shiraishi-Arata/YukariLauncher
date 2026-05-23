package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.arata.yukarilauncher.feature.accounts.AccountsManager;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.utils.path.PathManager;
import com.arata.yukarilauncher.utils.skin.SkinFileDownloader;
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt;

import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * マインクラフトのアカウント情報を保持するクラス。
 * ローカルアカウントとMicrosoftアカウントの両方をサポートします。
 */
@Keep
public class MinecraftAccount {
    /** アクセストークン */
    public String accessToken = "0";
    /** クライアントトークン（リフレッシュ・無効化用） */
    public String clientToken = "0";
    /** プロファイルUUID（スキン取得用） */
    public String profileId = "00000000-0000-0000-0000-000000000000";
    /** ユーザー名 */
    public String username = "Steve";
    /** MSAリフレッシュトークン */
    public String msaRefreshToken = "0";
    /** XboxユーザーID */
    public String xuid;
    /** 他の認証システムのベースURL */
    public String otherBaseUrl;
    /** 他の認証システムのアカウント名 */
    public String otherAccount;
    /** 他の認証システムのパスワード */
    public String otherPassword;
    /** アカウントタイプ */
    public String accountType;
    /** 内部で使用される一意のUUID */
    private final String uniqueUUID = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);

    /**
     * Microsoftアカウントのスキンを更新します。
     */
    public void updateMicrosoftSkin() {
        updateSkin("https://sessionserver.mojang.com");
    }

    /**
     * 他の認証システムのスキンを更新します。
     */
    public void updateOtherSkin() {
        updateSkin(StringUtilsKt.removeSuffix(otherBaseUrl, "/") + "/sessionserver/");
    }

    /**
     * 指定されたURLからスキンをダウンロードして更新します。
     */
    private void updateSkin(String url) {
        File skinFile = new File(PathManager.DIR_USER_SKIN, uniqueUUID + ".png");
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile);
        try {
            new SkinFileDownloader().yggdrasil(url, skinFile, profileId);
            Logging.i("SkinLoader", "Update skin success");
        } catch (Exception e) {
            Logging.i("SkinLoader", "Could not update skin\n" + Tools.printToString(e));
        }
    }

    /**
     * アカウントをファイルに保存します。
     */
    public void save() throws IOException {
        Tools.write(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID, Tools.GLOBAL_GSON.toJson(this));
    }

    /**
     * JSON文字列からMinecraftAccountをパースします。
     */
    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
    }

    /**
     * プロファイルIDからアカウントをロードします。
     */
    public static MinecraftAccount loadFromProfileID(String profileID) {
        for (MinecraftAccount account : AccountsManager.INSTANCE.getAllAccounts()) {
            if (Objects.equals(account.profileId, profileID)) return account;
        }
        return null;
    }

    /**
     * 一意のUUIDからアカウントをロードします。
     */
    public static MinecraftAccount loadFromUniqueUUID(String uniqueUUID) {
        if(!accountExists(uniqueUUID)) return null;
        try {
            MinecraftAccount acc = parse(Tools.read(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID));
            if (acc.accessToken == null) acc.accessToken = "0";
            if (acc.clientToken == null) acc.clientToken = "0";
            if (acc.profileId == null) acc.profileId = "00000000-0000-0000-0000-000000000000";
            if (acc.username == null) acc.username = "0";
            if (acc.msaRefreshToken == null) acc.msaRefreshToken = "0";
            return acc;
        } catch(IOException | JsonSyntaxException e) {
            Logging.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile",e);
            return null;
        }
    }

    /**
     * @return アカウントファイルが存在するかどうか
     */
    private static boolean accountExists(String uniqueUUID) {
        return !uniqueUUID.isEmpty() && new File(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID).exists();
    }

    /**
     * @return 内部で使用される一意のUUID
     */
    public String getUniqueUUID() {
        return this.uniqueUUID;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinecraftAccount{" +
                "username='" + username + '\'' +
                ", accountType=" + accountType +
                '}';
    }
}
