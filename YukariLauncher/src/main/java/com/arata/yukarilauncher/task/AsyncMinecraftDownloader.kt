package com.arata.yukarilauncher.task

import com.arata.yukarilauncher.event.sticky.MinecraftVersionValueEvent
import com.arata.yukarilauncher.value.JMinecraftVersionList
import org.greenrobot.eventbus.EventBus

/** 非同期Minecraftダウンローダー。 */
object AsyncMinecraftDownloader {

    /** バージョン文字列からリスト内のバージョンを取得する。 @param versionString バージョン文字列 @return バージョン情報、見つからない場合はnull */
    fun getListedVersion(versionString: String): JMinecraftVersionList.Version? {
        val versionList = getJMinecraftVersionList() ?: return null
        for (version in versionList.versions!!) {
            if (version.id == versionString) return version
        }
        return null
    }

    /** JMinecraftVersionListをEventBusから取得する。 @return バージョンリスト */
    private fun getJMinecraftVersionList(): JMinecraftVersionList? {
        val event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent::class.java)
        return event?.list
    }

    /** ダウンロード完了リスナー。 */
    interface DoneListener {
        /** ダウンロード完了時に呼び出される。 */
        fun onDownloadDone()
        /** ダウンロード失敗時に呼び出される。 @param throwable 例外 */
        fun onDownloadFailed(throwable: Throwable)
    }
}