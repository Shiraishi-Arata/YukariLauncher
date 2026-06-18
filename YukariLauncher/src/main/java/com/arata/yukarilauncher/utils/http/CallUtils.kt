package com.arata.yukarilauncher.utils.http

import com.arata.yukarilauncher.utils.path.UrlManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

/**
 * HTTPリクエスト実行のユーティリティクラス
 * トークン認証付きのOkHttpリクエストを非同期的・同期的に実行する
 */
class CallUtils(
    private val listener: CallbackListener,
    url: String,
    private val token: String?
) {
    /**
     * リクエストにAuthorizationヘッダーを追加するインターセプター
     */
    private val tokenInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithToken = originalRequest.newBuilder()
        token?.let { requestWithToken.header("Authorization", "token $token") }

        chain.proceed(requestWithToken.build())
    }

    val client: OkHttpClient = UrlManager.createOkHttpClientBuilder { builder ->
        builder.addInterceptor(tokenInterceptor)
    }.build()

    private val newCall: Call = client.newCall(UrlManager.createRequestBuilder(url).build())

    /**
     * 非同期でリクエストを実行する
     */
    fun enqueue() {
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure(call)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                listener.onResponse(call, response)
            }
        })
    }

    /**
     * 同期でリクエストを実行する
     */
    fun execute() {
        try {
            val response = newCall.execute()

            if (response.isSuccessful) {
                listener.onResponse(newCall, response)
            } else {
                listener.onFailure(newCall)
            }
        } catch (e: IOException) {
            listener.onFailure(newCall)
        }
    }

    /**
     * コールバックリスナーインターフェース
     */
    interface CallbackListener {
        fun onFailure(call: Call?)

        @Throws(IOException::class)
        fun onResponse(call: Call?, response: Response?)
    }
}