package net.kdt.pojavlaunch.plugins;

import androidx.annotation.Keep;

/**
 * プラグインとランチャー間の通信に使用されるクラス。
 */
@Keep
public class PluginInfo {
    /**
     * プラグインが認識するイベントの識別子。
     */
    @Keep
    public static class Events {
        /**
         * このプラグインが処理できるイベントの配列。
         */
        public String[] values;
    }

    /**
     * プラグイン名。
     */
    @Keep
    public String name;

    /**
     * プラグインバージョン。
     */
    @Keep
    public String version;

    /**
     * 著者情報。
     */
    @Keep
    public String author;

    /**
     * プラグインの説明。
     */
    @Keep
    public String description;

    /**
     * プラグインアイコンのパス。
     */
    @Keep
    public String icon;

    /**
     * 最新版のチェックとダウンロードに使用されるURL。
     */
    @Keep
    public String updateUrl;

    /**
     * プラグインのメインクラス。
     */
    @Keep
    public String mainClass;

    /**
     * プラグインが使用するイベントと依存関係のリスト。
     */
    @Keep
    public Events events;
}
