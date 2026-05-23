package net.kdt.pojavlaunch.authenticator.microsoft;

import android.content.Context;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.utils.stringutils.StringUtils;

/**
 * Microsoft認証中にユーザーに表示するためのローカライズされた例外クラス。
 */
public class PresentedException extends RuntimeException {
    /** ローカライズされたエラーメッセージの文字列リソースID */
    final int localizationStringId;
    /** Minecraftを購入していない可能性があるかどうか */
    final boolean suspectedNoMinecraftPurchase;
    /** メッセージフォーマットの追加引数 */
    final Object[] extraArgs;

    /**
     * @param localizationStringId エラーメッセージのリソースID
     * @param suspectedNoMinecraftPurchase Minecraft未購入の可能性がある場合はtrue
     * @param extraArgs メッセージフォーマットの追加引数
     */
    public PresentedException(int localizationStringId, boolean suspectedNoMinecraftPurchase, Object... extraArgs) {
        this.localizationStringId = localizationStringId;
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase;
        this.extraArgs = extraArgs;
    }

    /**
     * @param throwable 元の例外
     * @param localizationStringId エラーメッセージのリソースID
     * @param suspectedNoMinecraftPurchase Minecraft未購入の可能性がある場合はtrue
     * @param extraArgs メッセージフォーマットの追加引数
     */
    public PresentedException(Throwable throwable, int localizationStringId, boolean suspectedNoMinecraftPurchase, Object... extraArgs) {
        super(throwable);
        this.localizationStringId = localizationStringId;
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase;
        this.extraArgs = extraArgs;
    }

    /**
     * コンテキストに応じてローカライズされたエラーメッセージを生成します。
     * @param context Androidコンテキスト
     * @return ローカライズされたエラーメッセージ文字列
     */
    public String toString(Context context) {
        String string = context.getString(localizationStringId, extraArgs);
        if (suspectedNoMinecraftPurchase) {
            string = StringUtils.insertNewline(string, context.getString(R.string.no_minecraft_purchase));
        }
        return string;
    }
}
