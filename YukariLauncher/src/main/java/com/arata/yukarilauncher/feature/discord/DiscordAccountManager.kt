package com.arata.yukarilauncher.feature.discord

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.Task
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Discordアカウントのログイン検証と画像プロキシ（Imgur→Discord external-assets）を担当します。
 * 画像はImgurにアップロード後、Discordのexternal-assets APIでプロキシして使用します。
 */
object DiscordAccountManager {
    private const val API_BASE = "https://discord.com/api/v10"
    private const val IMGUR_API_BASE = "https://api.imgur.com/3"
    private const val APPLICATION_ID = "1369911585576845362"
    private const val IMGUR_CLIENT_ID = "d70305e7c3ac5c6"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** ログイン試行の結果 */
    data class LoginResult(val success: Boolean, val account: DiscordAccount? = null, val error: String? = null)

    /**
     * 現在のカスタムステータスをDiscord APIから取得します。
     * @param token Discord認証トークン
     * @param callback カスタムステータスのテキスト（未設定時はnull）を受け取るコールバック（メインスレッドで実行）
     */
    fun fetchCustomStatus(token: String, callback: (String?) -> Unit) {
        Task.runTask {
            try {
                val request = Request.Builder()
                    .url("$API_BASE/users/@me/settings")
                    .header("Authorization", token)
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post { callback(null) }
                    return@runTask null
                }
                val json = JSONObject(response.body?.string() ?: "")
                val status = json.optJSONObject("custom_status")
                val text = status?.optString("text", null)?.ifEmpty { null }
                Logging.i("DiscordAccountManager", "Custom status from API: $text")
                Handler(Looper.getMainLooper()).post { callback(text) }
            } catch (e: Exception) {
                Logging.e("DiscordAccountManager", "Failed to fetch custom status", e)
                Handler(Looper.getMainLooper()).post { callback(null) }
            }
            null
        }.execute()
    }

    /**
     * トークンを使用してDiscordログインを試行します。
     * トークンの検証後、有効な場合はアカウントを保存します。
     * @param token Discord認証トークン
     * @param callback ログイン結果を受け取るコールバック（メインスレッドで実行）
     */
    fun loginWithToken(token: String, callback: (LoginResult) -> Unit) {
        Task.runTask {
            val result = validateToken(token)
            Handler(Looper.getMainLooper()).post { callback(result) }
            null
        }.execute()
    }

    /**
     * 指定されたURL一覧をDiscord external-assets APIでプロキシします。
     * @param urls プロキシするURL一覧
     * @param callback プロキシ結果（URL→external_asset_path）のマップを受け取るコールバック
     */
    fun proxyExternalAssets(urls: List<String>, callback: (Map<String, String>) -> Unit) {
        val token = DiscordPrefs.getAccounts().firstOrNull()?.token ?: return callback(emptyMap())
        Task.runTask {
            val results = doProxyAssets(token, urls)
            Handler(Looper.getMainLooper()).post { callback(results) }
            null
        }.execute()
    }

    /**
     * ランチャーアイコン（ic_launcher）をImgurにアップロードし、external-assetsでプロキシします。
     * @param callback プロキシ結果の外部アセットパスを受け取るコールバック
     */
    fun proxyLauncherIcon(callback: (String?) -> Unit) {
        val token = DiscordPrefs.getAccounts().firstOrNull()?.token ?: return callback(null)
        Task.runTask {
            val iconBytes = getLauncherIconBytes()
            val result = if (iconBytes.isNotEmpty()) uploadBytesAndProxy(token, iconBytes, "launcher_icon.png") else null
            Handler(Looper.getMainLooper()).post { callback(result) }
            null
        }.execute()
    }

    /**
     * 任意の画像バイトデータをImgurアップロード→Discordプロキシします。
     * @param imageBytes 画像のバイトデータ
     * @param fileName Imgurアップロード時のファイル名
     * @param callback プロキシ結果の外部アセットパスを受け取るコールバック
     */
    fun proxyImageBytes(imageBytes: ByteArray, fileName: String, callback: (String?) -> Unit) {
        val token = DiscordPrefs.getAccounts().firstOrNull()?.token ?: return callback(null)
        Task.runTask {
            val result = uploadBytesAndProxy(token, imageBytes, fileName)
            Handler(Looper.getMainLooper()).post { callback(result) }
            null
        }.execute()
    }

    /** ランチャーアイコンをPNGバイトデータとして取得します。 */
    private fun getLauncherIconBytes(): ByteArray {
        val context = ContextExecutor.getApplication()
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: return ByteArray(0)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return stream.toByteArray()
    }

    /**
     * 画像をImgurにアップロードし、Discord external-assets APIでプロキシします。
     * @return 外部アセットパス（"mp:..."）、失敗時はnull
     */
    private fun uploadBytesAndProxy(token: String, iconBytes: ByteArray, fileName: String): String? {
        try {
            // Step 1: Imgurに画像をアップロード
            val imgurBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName,
                    iconBytes.toRequestBody("image/png".toMediaType()))
                .addFormDataPart("type", "png")
                .build()

            val imgurRequest = Request.Builder()
                .url("$IMGUR_API_BASE/image")
                .header("Authorization", "Client-ID $IMGUR_CLIENT_ID")
                .post(imgurBody)
                .build()

            val imgurResponse = client.newCall(imgurRequest).execute()
            if (!imgurResponse.isSuccessful) {
                Logging.e("DiscordAccountManager", "Imgur upload failed: ${imgurResponse.code}")
                return null
            }

            val imgurJson = JSONObject(imgurResponse.body?.string() ?: return null)
            val imgurUrl = imgurJson.optJSONObject("data")?.optString("link", "")
            if (imgurUrl.isNullOrEmpty()) {
                Logging.e("DiscordAccountManager", "Imgur response missing link")
                return null
            }

            // Step 2: Discord external-assetsでプロキシ
            val proxied = doProxyAssets(token, listOf(imgurUrl))
            return proxied[imgurUrl]
        } catch (e: Exception) {
            Logging.e("DiscordAccountManager", "Upload and proxy icon error", e)
            return null
        }
    }

    /**
     * Discord external-assets APIを呼び出し、指定されたURLをプロキシします。
     * @return URL→external_asset_path（"mp:..."）のマップ
     */
    private fun doProxyAssets(token: String, urls: List<String>): Map<String, String> {
        return try {
            val body = JSONObject().apply {
                put("urls", JSONArray(urls))
            }
            val request = Request.Builder()
                .url("$API_BASE/applications/$APPLICATION_ID/external-assets")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Logging.e("DiscordAccountManager", "Proxy assets failed: ${response.code}")
                return emptyMap()
            }
            val assets = JSONArray(response.body?.string() ?: return emptyMap())
            val result = mutableMapOf<String, String>()
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val url = asset.optString("url", "")
                val path = asset.optString("external_asset_path", "")
                if (url.isNotEmpty() && path.isNotEmpty()) {
                    result[url] = "mp:$path"
                }
            }
            result
        } catch (e: Exception) {
            Logging.e("DiscordAccountManager", "Proxy assets error", e)
            emptyMap()
        }
    }

    /**
     * Discordトークンを検証し、有効な場合はDiscordAccountを作成して保存します。
     * @param token Discord認証トークン
     */
    private fun validateToken(token: String): LoginResult {
        return try {
            val request = Request.Builder()
                .url("$API_BASE/users/@me")
                .header("Authorization", token)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return LoginResult(false, error = "Token validation failed: ${response.code}")
            }

            val body = response.body?.string() ?: return LoginResult(false, error = "Empty response")
            val json = JSONObject(body)
            val account = DiscordAccount(
                id = json.getString("id"),
                username = json.getString("username"),
                globalName = json.optString("global_name", null)?.ifEmpty { null },
                discriminator = json.optString("discriminator", "0"),
                avatar = json.optString("avatar", null)?.ifEmpty { null },
                banner = json.optString("banner", null)?.ifEmpty { null },
                token = token
            )
            DiscordPrefs.addAccount(account)
            LoginResult(true, account)
        } catch (e: Exception) {
            Logging.e("DiscordAccountManager", "Login failed", e)
            LoginResult(false, error = e.message ?: "Unknown error")
        }
    }
}
