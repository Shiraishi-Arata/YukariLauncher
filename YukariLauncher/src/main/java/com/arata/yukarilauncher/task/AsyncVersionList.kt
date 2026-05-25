package com.arata.yukarilauncher.task

import androidx.annotation.Nullable
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.path.UrlManager
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.arata.yukarilauncher.value.JMinecraftVersionList
import com.arata.yukarilauncher.Tools
import java.util.concurrent.Callable
import com.arata.yukarilauncher.utils.http.DownloadUtils.downloadString
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

/** バージョンリストを非同期で取得するクラス。 */
class AsyncVersionList {

    private companion object {
        /** キャッシュ時間（ミリ秒） */
        private const val CACHE_TIME = 5 * 60 * 1000L
    }

    /** バージョンリストを取得する。 @param listener 完了リスナー @param forceRefresh 強制リフレッシュするか */
    fun getVersionList(@Nullable listener: VersionDoneListener?, forceRefresh: Boolean) {
        Task.runTask<Void>(object : Callable<Void> {
            override fun call(): Void? {
                val versionFile = File(PathManager.FILE_VERSION_LIST)
                var versionList: JMinecraftVersionList? = null

                try {
                    val shouldDownload = forceRefresh ||
                            !versionFile.exists() ||
                            YLTools.getCurrentTimeMillis() > versionFile.lastModified() + CACHE_TIME

                    if (shouldDownload) {
                        versionList = downloadVersionList(UrlManager.URL_MINECRAFT_VERSION_REPOS)
                    }
                } catch (e: Exception) {
                    Logging.e("AsyncVersionList", "Refreshing version list failed :$e")
                    Logging.e("GetVersionList", Tools.printToString(e))
                }

                if (versionList == null) {
                    try {
                        versionList = Tools.GLOBAL_GSON.fromJson(
                            JsonReader(FileReader(versionFile)),
                            JMinecraftVersionList::class.java
                        )
                    } catch (e: FileNotFoundException) {
                        Logging.e("File Not Found", Tools.printToString(e))
                    } catch (e: JsonIOException) {
                        Logging.e("AsyncVersionList", Tools.printToString(e))
                        versionFile.delete()
                        if (!forceRefresh) {
                            getVersionList(listener, true)
                            return null
                        }
                    } catch (e: JsonSyntaxException) {
                        Logging.e("AsyncVersionList", Tools.printToString(e))
                        versionFile.delete()
                        if (!forceRefresh) {
                            getVersionList(listener, true)
                            return null
                        }
                    }
                }

                if (listener != null && versionList != null) {
                    listener.onVersionDone(versionList)
                }

                return null
            }
        }).execute()
    }

    /** バージョンリストを強制リフレッシュする。 @param listener 完了リスナー */
    fun refresh(@Nullable listener: VersionDoneListener?) {
        getVersionList(listener, true)
    }

    /** バージョンリストのキャッシュをクリアする。 */
    fun clearCache() {
        val versionFile = File(PathManager.FILE_VERSION_LIST)
        if (versionFile.exists()) versionFile.delete()
    }

    /** バージョンリストをダウンロードする。 @param mirror ミラーURL @return バージョンリスト */
    private fun downloadVersionList(mirror: String): JMinecraftVersionList? {
        var list: JMinecraftVersionList? = null

        try {
            Logging.i("ExtVL", "Syncing to external: $mirror")

            val jsonString = downloadString(mirror)

            list = Tools.GLOBAL_GSON.fromJson(jsonString, JMinecraftVersionList::class.java)

            Logging.i("ExtVL", "Downloaded version list, len=${list!!.versions!!.size}")

            FileOutputStream(PathManager.FILE_VERSION_LIST).use { fos ->
                fos.write(jsonString.toByteArray())
            }
        } catch (e: IOException) {
            Logging.e("AsyncVersionList", Tools.printToString(e))
        }

        return list
    }

    /** バージョンリスト取得完了リスナー。 */
    interface VersionDoneListener {
        /** バージョンリスト取得完了時に呼び出される。 @param versions バージョンリスト */
        fun onVersionDone(versions: JMinecraftVersionList)
    }
}
