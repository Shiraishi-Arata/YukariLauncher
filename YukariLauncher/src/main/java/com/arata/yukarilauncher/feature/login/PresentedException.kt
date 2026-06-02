package com.arata.yukarilauncher.feature.login

import android.content.Context
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.utils.stringutils.StringUtils

/** ユーザーに表示する例外。ローカライズされたエラーメッセージを保持する。 */
class PresentedException : RuntimeException {
    /** ローカライズ文字列リソースID */
    val localizationStringId: Int
    /** Minecraft未購入の疑いがあるか */
    val suspectedNoMinecraftPurchase: Boolean
    /** 追加引数 */
    val extraArgs: Array<out Any?>

    /** @param localizationStringId ローカライズ文字列リソースID @param suspectedNoMinecraftPurchase Minecraft未購入の疑いがあるか @param extraArgs 追加引数 */
    constructor(localizationStringId: Int, suspectedNoMinecraftPurchase: Boolean, vararg extraArgs: Any?) {
        this.localizationStringId = localizationStringId
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase
        this.extraArgs = extraArgs
    }

    /** @param throwable 元の例外 @param localizationStringId ローカライズ文字列リソースID @param suspectedNoMinecraftPurchase Minecraft未購入の疑いがあるか @param extraArgs 追加引数 */
    constructor(throwable: Throwable, localizationStringId: Int, suspectedNoMinecraftPurchase: Boolean, vararg extraArgs: Any?) : super(throwable) {
        this.localizationStringId = localizationStringId
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase
        this.extraArgs = extraArgs
    }

    /** ローカライズされたエラーメッセージを文字列として取得する。 @param context コンテキスト @return ローカライズされたエラーメッセージ */
    fun toString(context: Context): String {
        var string = context.getString(localizationStringId, *extraArgs)
        if (suspectedNoMinecraftPurchase) {
            string = StringUtils.insertNewline(string, context.getString(R.string.no_minecraft_purchase))
        }
        return string
    }
}