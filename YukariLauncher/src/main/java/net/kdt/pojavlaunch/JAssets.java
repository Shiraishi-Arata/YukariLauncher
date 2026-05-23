package net.kdt.pojavlaunch;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * マインクラフトのアセットインデックスを保持するクラス。
 */
@Keep
public class JAssets {
    /* 古いバージョンのMCで使用され、ファイルが .minecraft/resources 以下に配置されていた場合 */
    @SerializedName("map_to_resources") public boolean mapToResources;
    /** アセットオブジェクトのマップ */
    public Map<String, JAssetInfo> objects;

    /* legacy.json（〜1.6.X）アセットファイルで使用され、.minecraft/assetsフォルダのルートのパスに使用されます */
    public boolean virtual;
}
