package net.kdt.pojavlaunch.tasks;

import com.arata.yukarilauncher.event.sticky.MinecraftVersionValueEvent;

import net.kdt.pojavlaunch.JMinecraftVersionList;

import org.greenrobot.eventbus.EventBus;

/**
 * マインクラフトのバージョン情報を非同期で取得するためのユーティリティクラス。
 */
public class AsyncMinecraftDownloader {

    /**
     * バージョン文字列からバージョン情報を検索します。
     * @param versionString 検索するバージョンID
     * @return 見つかったバージョン、存在しない場合はnull
     */
    public static JMinecraftVersionList.Version getListedVersion(String versionString) {
        JMinecraftVersionList versionList = getJMinecraftVersionList();
        if (versionList == null || versionList.versions == null) return null;
        for (JMinecraftVersionList.Version version : versionList.versions) {
            if (version.id.equals(versionString)) return version;
        }
        return null;
    }

    /**
     * イベントバスからJMinecraftVersionListを取得します。
     */
    private static JMinecraftVersionList getJMinecraftVersionList() {
        MinecraftVersionValueEvent event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent.class);
        if (event != null) return event.getList();
        else return null;
    }

    /**
     * ダウンロード完了リスナーインターフェース。
     */
    public interface DoneListener{
        /** ダウンロードが完了したときに呼び出されます。 */
        void onDownloadDone();
        /** ダウンロードが失敗したときに呼び出されます。 */
        void onDownloadFailed(Throwable throwable);
    }
}
