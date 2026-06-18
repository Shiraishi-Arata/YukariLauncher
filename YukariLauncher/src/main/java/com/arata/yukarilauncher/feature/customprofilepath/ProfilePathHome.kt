package com.arata.yukarilauncher.feature.customprofilepath

class ProfilePathHome {
    companion object {
        /**
         * ゲームのホームディレクトリを取得する。
         * @return .minecraft フォルダの絶対パス
         */
        @JvmStatic
        fun getGameHome(): String = "${ProfilePathManager.getCurrentPath()}/.minecraft"

        /**
         * バージョンフォルダのパスを取得する。
         * @return versions フォルダの絶対パス
         */
        @JvmStatic
        fun getVersionsHome(): String = "${getGameHome()}/versions"

        /**
         * ライブラリフォルダのパスを取得する。
         * @return libraries フォルダの絶対パス
         */
        @JvmStatic
        fun getLibrariesHome(): String = "${getGameHome()}/libraries"

        /**
         * アセットフォルダのパスを取得する。
         * @return assets フォルダの絶対パス
         */
        @JvmStatic
        fun getAssetsHome(): String = "${getGameHome()}/assets"

        /**
         * リソースフォルダのパスを取得する。
         * @return resources フォルダの絶対パス
         */
        @JvmStatic
        fun getResourcesHome(): String = "${getGameHome()}/resources"
    }
}