package com.arata.yukarilauncher.value

import androidx.annotation.Keep

/**
 * Minecraftアセット情報を保持するデータクラス。
 * アセットのハッシュ値とサイズを格納し、JSONデシリアライズに使用されます。
 */
@Keep
class JAssetInfo {
    /** アセットのSHA-1ハッシュ値。 */
    var hash: String? = null
    /** アセットのファイルサイズ（バイト単位）。 */
    var size: Int = 0
}