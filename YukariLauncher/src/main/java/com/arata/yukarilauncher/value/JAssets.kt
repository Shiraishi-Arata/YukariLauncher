package com.arata.yukarilauncher.value

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.arata.yukarilauncher.value.JAssetInfo

/**
 * Minecraftのアセットインデックス情報を保持するクラス。
 * アセットのマッピング設定やオブジェクト一覧を格納します。
 */
@Keep
class JAssets {
    /** アセットをリソースにマッピングするかどうかのフラグ。 */
    @SerializedName("map_to_resources") var mapToResources: Boolean = false
    /** アセットオブジェクトのマップ。キーはアセットパス、値はアセット情報。 */
    var objects: MutableMap<String, JAssetInfo>? = null
    /** 仮想アセットかどうかのフラグ。 */
    var virtual: Boolean = false
}