package com.arata.yukarilauncher.feature.login

import android.content.Context
import androidx.annotation.NonNull
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.accounts.AccountType
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.utils.path.UrlManager
import com.arata.yukarilauncher.value.MinecraftAccount
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections

/** Microsoftアカウントを使用したバックグラウンドログイン処理を担当するクラス。 */
class MicrosoftBackgroundLogin(
    private val mIsRefresh: Boolean,
    private val mAuthCode: String
) {
    /** 更新トークン */
    var msRefreshToken: String? = null
    /** Minecraftユーザー名 */
    var mcName: String? = null
    /** Minecraftアクセストークン */
    var mcToken: String? = null
    /** Minecraft UUID */
    var mcUuid: String? = null
    /** ゲームを所有しているか */
    var doesOwnGame = false

    /** ログイン処理を実行する。 @param context コンテキスト @param account 既存のアカウント @param doneListener 完了リスナー @param errorListener エラーレスナー */
    fun performLogin(
        context: Context,
        account: MinecraftAccount?,
        doneListener: DoneListener?,
        errorListener: ErrorListener?
    ) {
        Task.runTask {
            notifyProgress(1, context.getString(R.string.account_login_progress_access_token))
            val accessToken = acquireAccessToken(mIsRefresh, mAuthCode)
            notifyProgress(2, context.getString(R.string.account_login_progress_xbl_token))
            val xboxLiveToken = acquireXBLToken(accessToken)
            notifyProgress(3, context.getString(R.string.account_login_progress_xsts_token))
            val xsts = acquireXsts(xboxLiveToken)
            notifyProgress(4, context.getString(R.string.account_login_progress_minecraft_token))
            val mcToken = acquireMinecraftToken(xsts[0], xsts[1])
            notifyProgress(5, context.getString(R.string.account_login_progress_checking))
            fetchOwnedItems(mcToken)
            checkMcProfile(mcToken)

            val acc: MinecraftAccount = if (account == null) {
                MinecraftAccount.loadFromProfileID(mcUuid ?: "") ?: MinecraftAccount()
            } else {
                account
            }

            if (doesOwnGame) {
                acc.xuid = xsts[0]
                acc.clientToken = "0"
                acc.accessToken = mcToken
                acc.username = mcName ?: ""
                acc.profileId = mcUuid ?: ""
                acc.msaRefreshToken = msRefreshToken ?: ""
                acc.accountType = AccountType.MICROSOFT.type
                acc.updateMicrosoftSkin()
            }
            acc.save()
            Logging.i("Account", "Saved the account : " + acc.username)

            acc
        }.ended(TaskExecutors.getAndroidUI()) { acc ->
            if (doneListener != null && acc != null) doneListener.onLoginDone(acc)
        }.onThrowable(TaskExecutors.getAndroidUI()) { e ->
            Logging.e("MicroAuth", "Exception thrown during authentication", e)
            if (errorListener != null) errorListener.onLoginError(e)
        }.finallyTask {
            ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT)
        }.execute()
    }

    /** アクセストークンを取得する。 @param isRefresh リフレッシュトークンを使用するか @param authcode 認証コード @return アクセストークン */
    @Throws(IOException::class, JSONException::class)
    fun acquireAccessToken(isRefresh: Boolean, authcode: String): String {
        val url = URL(authTokenUrl)
        Logging.i("MicrosoftLogin", "isRefresh=$isRefresh")

        val formData = convertToFormData(
            "client_id", "00000000402b5328",
            if (isRefresh) "refresh_token" else "code", authcode,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code",
            "redirect_url", "https://login.live.com/oauth20_desktop.srf",
            "scope", "service::user.auth.xboxlive.com::MBI_SSL"
        )

        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("charset", "utf-8")
        conn.setRequestProperty("Content-Length", formData.toByteArray(StandardCharsets.UTF_8).size.toString())
        conn.requestMethod = "POST"
        conn.useCaches = false
        conn.doInput = true
        conn.doOutput = true
        conn.connect()
        conn.outputStream.use { wr ->
            wr.write(formData.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.responseCode in 200..299) {
            val jo = JSONObject(Tools.read(conn.inputStream))
            msRefreshToken = jo.getString("refresh_token")
            conn.disconnect()
            return jo.getString("access_token")
        } else {
            throw getResponseThrowable(conn)
        }
    }

    /** XBLトークンを取得する。 @param accessToken アクセストークン @return XBLトークン */
    @Throws(IOException::class, JSONException::class)
    private fun acquireXBLToken(accessToken: String): String {
        val url = URL(xblAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("AuthMethod", "RPS")
        properties.put("SiteName", "user.auth.xboxlive.com")
        properties.put("RpsTicket", accessToken)
        data.put("Properties", properties)
        data.put("RelyingParty", "http://auth.xboxlive.com")
        data.put("TokenType", "JWT")

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.outputStream.use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.responseCode in 200..299) {
            val jo = JSONObject(Tools.read(conn.inputStream))
            conn.disconnect()
            return jo.getString("Token")
        } else {
            throw getResponseThrowable(conn)
        }
    }

    /** XSTSトークンを取得する。 @param xblToken XBLトークン @return [UHS, トークン]の配列 */
    @NonNull
    @Throws(IOException::class, JSONException::class)
    private fun acquireXsts(xblToken: String): Array<String> {
        val url = URL(xstsAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("SandboxId", "RETAIL")
        properties.put("UserTokens", JSONArray(Collections.singleton(xblToken)))
        data.put("Properties", properties)
        data.put("RelyingParty", "rp://api.minecraftservices.com/")
        data.put("TokenType", "JWT")

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.outputStream.use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }

        if (conn.responseCode in 200..299) {
            val jo = JSONObject(Tools.read(conn.inputStream))
            val uhs = jo.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs")
            val token = jo.getString("Token")
            conn.disconnect()
            return arrayOf(uhs, token)
        } else if (conn.responseCode == 401) {
            val responseContents = Tools.read(conn.errorStream)
            val jo = JSONObject(responseContents)
            val xerr = jo.optLong("XErr", -1)
            val localeId = XSTS_ERRORS[xerr]
            if (localeId != null) {
                throw PresentedException(RuntimeException(responseContents), localeId, false)
            }
            throw PresentedException(RuntimeException(responseContents), R.string.xerr_unknown, true, xerr)
        } else {
            throw getResponseThrowable(conn)
        }
    }

    /** Minecraftアクセストークンを取得する。 @param xblUhs XBL UHS @param xblXsts XBL XSTS @return Minecraftアクセストークン */
    @Throws(IOException::class, JSONException::class)
    private fun acquireMinecraftToken(xblUhs: String, xblXsts: String): String {
        val url = URL(mcLoginUrl)

        val data = JSONObject()
        data.put("identityToken", "XBL3.0 x=$xblUhs;$xblXsts")

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.outputStream.use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }

        if (conn.responseCode in 200..299) {
            val jo = JSONObject(Tools.read(conn.inputStream))
            conn.disconnect()
            mcToken = jo.getString("access_token")
            return jo.getString("access_token")
        } else {
            throw getResponseThrowable(conn)
        }
    }

    /** 所有権を確認する。 @param mcAccessToken Minecraftアクセストークン */
    @Throws(IOException::class)
    private fun fetchOwnedItems(mcAccessToken: String) {
        val url = URL(mcStoreUrl)
        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.useCaches = false
        conn.connect()
        if (conn.responseCode !in 200..299) {
            throw getResponseThrowable(conn)
        }
    }

    /** Minecraftプロフィールを確認する。 @param mcAccessToken Minecraftアクセストークン */
    @Throws(IOException::class, JSONException::class)
    private fun checkMcProfile(mcAccessToken: String) {
        val url = URL(mcProfileUrl)
        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.useCaches = false
        conn.connect()

        if (conn.responseCode in 200..299) {
            val s = Tools.read(conn.inputStream)
            conn.disconnect()
            val jsonObject = JSONObject(s)
            val name = jsonObject.get("name") as String
            val uuid = jsonObject.get("id") as String
            val uuidDashes = uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(), "$1-$2-$3-$4-$5"
            )
            doesOwnGame = true
            Logging.i("MicrosoftLogin", "UserName = $name")
            Logging.i("MicrosoftLogin", "Uuid Minecraft = $uuidDashes")
            mcName = name
            mcUuid = uuidDashes
        } else {
            Logging.i("MicrosoftLogin", "It seems that this Microsoft Account does not own the game.")
            doesOwnGame = false
            throw PresentedException(RuntimeException(conn.responseMessage), R.string.minecraft_not_owned, true)
        }
    }

    /** 進捗を通知する。 @param step 進捗ステップ @param stepString 進捗メッセージ */
    private fun notifyProgress(step: Int, stepString: String) {
        ProgressLayout.setProgress(ProgressLayout.LOGIN_ACCOUNT, step * 20, R.string.account_login_microsoft_progress, stepString)
    }

    /** HTTPレスポンスから例外を生成する。 @param conn HTTPコネクション @return ランタイム例外 */
    private fun getResponseThrowable(conn: HttpURLConnection): RuntimeException {
        Logging.i("MicrosoftLogin", "Error code: " + conn.responseCode + ": " + conn.responseMessage)
        if (conn.responseCode == 429) {
            return PresentedException(R.string.microsoft_login_retry_later, false)
        }
        return RuntimeException(conn.responseMessage)
    }

    companion object {
        /** 認証トークンURL */
        private const val authTokenUrl = "https://login.live.com/oauth20_token.srf"
        /** XBL認証URL */
        private const val xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate"
        /** XSTS認証URL */
        private const val xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize"
        /** MinecraftログインURL */
        private const val mcLoginUrl = "https://api.minecraftservices.com/authentication/login_with_xbox"
        /** MinecraftプロフィールURL */
        private const val mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile"
        /** MinecraftストアURL */
        private const val mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore"

        /** XSTSエラーコードとローカライズリソースIDのマッピング */
        private val XSTS_ERRORS: Map<Long, Int> = mapOf(
            2148916233L to R.string.xerr_no_account,
            2148916235L to R.string.xerr_not_available,
            2148916236L to R.string.xerr_adult_verification,
            2148916237L to R.string.xerr_adult_verification,
            2148916238L to R.string.xerr_child,
            2148074255L to R.string.account_microsoft_xerr_unauthorized
        )

        /** HTTPコネクションに共通のプロパティを設定する。 @param conn HTTPコネクション @param formData リクエストデータ */
        private fun setCommonProperties(conn: HttpURLConnection, formData: String) {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("charset", "utf-8")
            try {
                conn.setRequestProperty("Content-Length", formData.toByteArray(StandardCharsets.UTF_8).size.toString())
                conn.requestMethod = "POST"
            } catch (e: ProtocolException) {
                Logging.e("MicrosoftAuth", e.toString())
            }
            conn.useCaches = false
            conn.doInput = true
            conn.doOutput = true
        }

        /** フォームデータをURLエンコードされた文字列に変換する。 @param data キーと値のペア @return URLエンコードされたフォームデータ */
        @Throws(UnsupportedEncodingException::class)
        private fun convertToFormData(vararg data: String): String {
            val builder = StringBuilder()
            var i = 0
            while (i < data.size) {
                if (builder.isNotEmpty()) builder.append("&")
                builder.append(URLEncoder.encode(data[i], "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(data[i + 1], "UTF-8"))
                i += 2
            }
            return builder.toString()
        }
    }
}