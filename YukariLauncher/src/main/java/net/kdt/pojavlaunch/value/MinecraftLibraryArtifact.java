package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

/**
 * マインクラフトライブラリアーティファクトの情報を保持するクラス。MinecraftClientInfoを継承し、パス情報を追加します。
 */
@Keep
public class MinecraftLibraryArtifact extends MinecraftClientInfo {
    /** アーティファクトのパス */
    public String path;
}
