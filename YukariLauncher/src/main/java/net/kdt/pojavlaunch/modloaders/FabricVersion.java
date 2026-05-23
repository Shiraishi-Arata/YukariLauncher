package net.kdt.pojavlaunch.modloaders;

import androidx.annotation.NonNull;

/**
 * Fabric Loaderのバージョン情報を表すクラス
 */
public class FabricVersion {
    public String version;
    public boolean stable;

    /**
     * Fabric Loaderのディスクリプタークラス
     */
    public static class LoaderDescriptor extends FabricVersion {
        public FabricVersion loader;

        /**
         * このLoaderDescriptorの文字列表現を返します。
         */
        @NonNull
        @Override
        public String toString() {
            return loader != null ? loader.toString() : "null";
        }
    }

    /**
     * このFabricVersionの文字列表現を返します。
     */
    @NonNull
    @Override
    public String toString() {
        return version;
    }
}
