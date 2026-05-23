package com.arata.yukarilauncher.utils.path

import com.arata.yukarilauncher.BuildConfig
import com.arata.yukarilauncher.InfoDistributor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit

class UrlManager {
    companion object {
        private const val URL_USER_AGENT: String = "${InfoDistributor.LAUNCHER_NAME}/${BuildConfig.VERSION_NAME}"
        @JvmField
        val TIME_OUT = Pair(8000, TimeUnit.MILLISECONDS)
        const val URL_GITHUB_HOME: String = "https://api.github.com/repos/Shiraishi-Arata/Yukari-Fixes/contents/"
        const val URL_MCMOD: String = "https://www.mcmod.cn/"
        const val URL_MINECRAFT: String = "https://www.minecraft.net/"
        const val URL_MINECRAFT_VERSION_REPOS: String = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val URL_SUPPORT: String = ""
        const val URL_HOME: String = "https://github.com/Shiraishi-Arata/YukariLauncher"
        const val URL_FCL_RENDERER_PLUGIN: String = "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer"
        const val URL_FCL_DRIVER_PLUGIN: String = "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip"

        /**
         * URL接続を作成し、タイムアウトとUser-Agentを設定する
         */
        @JvmStatic
        fun createConnection(url: URL): URLConnection {
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", URL_USER_AGENT)
            connection.setConnectTimeout(TIME_OUT.first)
            connection.setReadTimeout(TIME_OUT.first)

            return connection
        }

        /**
         * HTTP用のURL接続を作成する
         */
        @JvmStatic
        @Throws(IOException::class)
        fun createHttpConnection(url: URL): HttpURLConnection {
            return createConnection(url) as HttpURLConnection
        }

        /**
         * OkHttpのRequest.Builderを作成する（ボディなし）
         */
        @JvmStatic
        fun createRequestBuilder(url: String): Request.Builder {
            return createRequestBuilder(url, null)
        }

        /**
         * OkHttpのRequest.Builderを作成する（ボディ付き）
         */
        @JvmStatic
        fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
            val request = Request.Builder().url(url).header("User-Agent", URL_USER_AGENT)
            body?.let{ request.post(it) }
            return request
        }

        /**
         * デフォルト設定のOkHttpClientを作成する
         */
        @JvmStatic
        fun createOkHttpClient(): OkHttpClient = createOkHttpClientBuilder().build()

        /**
         * OkHttpClient.Builderを作成する
         * カスタム設定を追加可能な拡張ポイントを提供する
         */
        @JvmStatic
        fun createOkHttpClientBuilder(action: (OkHttpClient.Builder) -> Unit = { }): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .callTimeout(TIME_OUT.first.toLong(), TIME_OUT.second)
                .apply(action)
        }
    }
}
