package com.arata.yukarilauncher.feature.mod.modpack.api

import android.util.ArrayMap
import com.google.gson.Gson
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.utils.path.UrlManager
import com.arata.yukarilauncher.Tools
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InterruptedIOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** ModパックAPI（CurseForge / Modrinth等）との通信を処理するハンドラ。 @param baseUrl APIのベースURL */
class ApiHandler(val baseUrl: String) {
    /** 追加のHTTPヘッダー。 */
    val additionalHeaders: MutableMap<String, String> = ArrayMap()

    /** APIキー付きのコンストラクタ。 @param url APIのベースURL @param apiKey APIキー */
    constructor(url: String, apiKey: String) : this(url) {
        additionalHeaders["x-api-key"] = apiKey
    }

    /** GETリクエストを実行する。 @param endpoint エンドポイントパス @param tClass レスポンスの型 @return デシリアライズされたレスポンス */
    fun <T> get(endpoint: String, tClass: Class<T>): T {
        return getFullUrl(additionalHeaders, "$baseUrl/$endpoint", tClass)
    }

    /** クエリパラメータ付きのGETリクエストを実行する。 @param endpoint エンドポイントパス @param query クエリパラメータ @param tClass レスポンスの型 @return デシリアライズされたレスポンス */
    fun <T> get(endpoint: String, query: HashMap<String, Any>, tClass: Class<T>): T {
        return getFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, tClass)
    }

    /** POSTリクエストを実行する。 @param endpoint エンドポイントパス @param body リクエストボディ @param tClass レスポンスの型 @return デシリアライズされたレスポンス */
    fun <T> post(endpoint: String, body: T, tClass: Class<T>): T {
        return postFullUrl(additionalHeaders, "$baseUrl/$endpoint", body, tClass)
    }

    /** クエリパラメータ付きのPOSTリクエストを実行する。 @param endpoint エンドポイントパス @param query クエリパラメータ @param body リクエストボディ @param tClass レスポンスの型 @return デシリアライズされたレスポンス */
    fun <T> post(endpoint: String, query: HashMap<String, Any>, body: T, tClass: Class<T>): T {
        return postFullUrl(additionalHeaders, "$baseUrl/$endpoint", query, body, tClass)
    }

    companion object {
        /** 指定されたURLから生のレスポンスを取得する。 @param url 取得対象のURL @return レスポンス文字列、失敗時はnull */
        fun getRaw(url: String): String? {
            return getRaw(null, url)
        }

        /** カスタムヘッダー付きで指定されたURLから生のレスポンスを取得する。 @param headers 追加HTTPヘッダー @param url 取得対象のURL @return レスポンス文字列、失敗時はnull */
        fun getRaw(headers: Map<String, String>?, url: String): String? {
            Logging.d("ApiHandler", url)
            var conn: HttpURLConnection? = null
            try {
                conn = UrlManager.createHttpConnection(URL(url))
                addHeaders(conn, headers)
                val inputStream: InputStream = BufferedInputStream(conn.inputStream)
                val data = Tools.read(inputStream)

                inputStream.close()
                conn.disconnect()
                return data
            } catch (e: FileNotFoundException) {
                Logging.d("ApiHandler", "File Not Found! " + Tools.printToString(e))
                if (conn != null) {
                    try {
                        val responseCode = conn.responseCode
                        val errorBody = conn.errorStream?.let { Tools.read(it) }
                        Logging.e("ApiHandler", "HTTP $responseCode error body: $errorBody")
                    } catch (_: Exception) {}
                }
                return null
            } catch (e: InterruptedIOException) {
                Logging.d("ApiHandler", "The connection has been interrupted, or has been canceled.\n" + Tools.printToString(e))
                return null
            } catch (e: Exception) {
                Logging.e("ApiHandler", Tools.printToString(e))
                if (conn != null) {
                    try {
                        val responseCode = conn.responseCode
                        val errorBody = conn.errorStream?.let { Tools.read(it) }
                        Logging.e("ApiHandler", "HTTP $responseCode error body: $errorBody")
                    } catch (_: Exception) {}
                    conn.disconnect()
                }
                throw RuntimeException(e)
            }
        }

        /** 指定されたURLにPOSTリクエストを送信し、生のレスポンスを取得する。 @param url 送信先URL @param body リクエストボディ文字列 @return レスポンス文字列、失敗時はnull */
        fun postRaw(url: String, body: String): String? {
            return postRaw(null, url, body)
        }

        /** カスタムヘッダー付きで指定されたURLにPOSTリクエストを送信する。 @param headers 追加HTTPヘッダー @param url 送信先URL @param body リクエストボディ文字列 @return レスポンス文字列、失敗時はnull */
        fun postRaw(headers: Map<String, String>?, url: String, body: String): String? {
            try {
                val conn = UrlManager.createHttpConnection(URL(url))
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                addHeaders(conn, headers)
                conn.doOutput = true

                val outputStream: OutputStream = conn.outputStream
                val input = body.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(input, 0, input.size)
                outputStream.close()

                val inputStream: InputStream = conn.inputStream
                val data = Tools.read(inputStream)
                inputStream.close()

                conn.disconnect()
                return data
            } catch (e: IOException) {
                Logging.e("ApiHandler", Tools.printToString(e))
            }
            return null
        }

        /** HttpURLConnectionにヘッダーを追加する。 @param connection 接続オブジェクト @param headers 追加するヘッダーマップ */
        private fun addHeaders(connection: HttpURLConnection, headers: Map<String, String>?) {
            if (headers != null) {
                for (key in headers.keys)
                    connection.addRequestProperty(key, headers[key])
            }
        }

        /** クエリパラメータマップをURLクエリ文字列に変換する。 @param query クエリパラメータ @return "?key=value&..."形式のクエリ文字列 */
        private fun parseQueries(query: HashMap<String, Any>): String {
            val params = StringBuilder("?")
            for (param in query.keys) {
                val value = query[param].toString()
                params.append(urlEncodeUTF8(param))
                        .append("=")
                        .append(urlEncodeUTF8(value))
                        .append("&")
            }
            return params.substring(0, params.length - 1)
        }

        /** 指定されたURLにGETリクエストを送信し、JSONをデシリアライズする。 @param url 完全なURL @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun getFullUrl(url: String, tClass: Class<*>): Any? {
            return getFullUrl(null, url, tClass)
        }

        /** クエリパラメータ付きでGETリクエストを送信し、JSONをデシリアライズする。 @param url 完全なURL @param query クエリパラメータ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun getFullUrl(url: String, query: HashMap<String, Any>, tClass: Class<*>): Any? {
            val nullHeaders: Map<String, String>? = null
            return getFullUrl(nullHeaders, url, query, tClass)
        }

        /** 指定されたURLにPOSTリクエストを送信し、JSONをデシリアライズする。 @param url 完全なURL @param body リクエストボディ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun postFullUrl(url: String, body: Any?, tClass: Class<*>): Any? {
            @Suppress("UNCHECKED_CAST")
            return postFullUrl(null, url, body, tClass as Class<Any?>)
        }

        /** クエリパラメータ付きでPOSTリクエストを送信し、JSONをデシリアライズする。 @param url 完全なURL @param query クエリパラメータ @param body リクエストボディ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun postFullUrl(url: String, query: HashMap<String, Any>, body: Any?, tClass: Class<*>): Any? {
            @Suppress("UNCHECKED_CAST")
            return postFullUrl(null, url, query, body, tClass as Class<Any?>)
        }

        /** カスタムヘッダー付きでGETリクエストを送信し、JSONをデシリアライズする。 @param headers 追加HTTPヘッダー @param url 完全なURL @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, tClass: Class<T>): T {
            return Gson().fromJson(getRaw(headers, url), tClass)
        }

        /** カスタムヘッダーとクエリパラメータ付きでGETリクエストを送信する。 @param headers 追加HTTPヘッダー @param url 完全なURL @param query クエリパラメータ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun <T> getFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, tClass: Class<T>): T {
            return getFullUrl(headers, url + parseQueries(query), tClass)
        }

        /** カスタムヘッダー付きでPOSTリクエストを送信し、JSONをデシリアライズする。 @param headers 追加HTTPヘッダー @param url 完全なURL @param body リクエストボディ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, body: T, tClass: Class<T>): T {
            return Gson().fromJson(postRaw(headers, url, body.toString()), tClass)
        }

        /** カスタムヘッダーとクエリパラメータ付きでPOSTリクエストを送信する。 @param headers 追加HTTPヘッダー @param url 完全なURL @param query クエリパラメータ @param body リクエストボディ @param tClass デシリアライズ先の型 @return デシリアライズされたオブジェクト */
        fun <T> postFullUrl(headers: Map<String, String>?, url: String, query: HashMap<String, Any>, body: T, tClass: Class<T>): T {
            return Gson().fromJson(postRaw(headers, url + parseQueries(query), body.toString()), tClass)
        }

        /** 文字列をUTF-8でURLエンコードする。 @param input エンコード対象の文字列 @return URLエンコードされた文字列 */
        private fun urlEncodeUTF8(input: String): String {
            try {
                return URLEncoder.encode(input, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("UTF-8 is required")
            }
        }
    }
}