package com.arata.yukarilauncher.feature.accounts

import android.content.Context
import com.kdt.mcgui.ProgressLayout
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.feature.login.DoneListener
import com.arata.yukarilauncher.feature.login.ErrorListener
import com.arata.yukarilauncher.feature.login.MicrosoftBackgroundLogin
import com.arata.yukarilauncher.value.MinecraftAccount
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Objects

class AccountUtils {
    companion object {
        @JvmStatic
/**
 * microsoftLoginする
 */
        fun microsoftLogin(context: Context, account: MinecraftAccount, doneListener: DoneListener, errorListener: ErrorListener) {
            MicrosoftBackgroundLogin(true, account.msaRefreshToken)
                .performLogin(context, account, doneListener, errorListener)
        }

        @JvmStatic
/**
 * otherLoginする
 */
        fun otherLogin(context: Context, account: MinecraftAccount, doneListener: DoneListener, errorListener: ErrorListener) {
/**
 * clearProgressする
 */
            fun clearProgress() = ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT)

            Task.runTask {
                OtherLoginHelper(account.otherBaseUrl!!, account.accountType!!, account.otherAccount!!, account.otherPassword!!,
                    object : OtherLoginHelper.OnLoginListener {
/**
 * onLoadingする
 */
                        override fun onLoading() {
                            ProgressLayout.setProgress(ProgressLayout.LOGIN_ACCOUNT, 0, R.string.account_login_start)
                        }

/**
 * unLoadingする
 */
                        override fun unLoading() {}

/**
 * onSuccessする
 */
                        override fun onSuccess(account: MinecraftAccount) {
                            account.save()
                            clearProgress()
                            doneListener.onLoginDone(account)
                        }

/**
 * onFailedする
 */
                        override fun onFailed(error: String) {
                            clearProgress()
                            errorListener.onLoginError(RuntimeException(error))
                            ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT)
                        }
                    }).justLogin(context, account)
            }.onThrowable { t -> errorListener.onLoginError(RuntimeException(t.message)) }.execute()
        }

        @JvmStatic
/**
 * isOtherLoginAccountする
 */
        fun isOtherLoginAccount(account: MinecraftAccount): Boolean {
            return !Objects.isNull(account.otherBaseUrl) && account.otherBaseUrl != "0"
        }

        @JvmStatic
/**
 * isMicrosoftAccountする
 */
        fun isMicrosoftAccount(account: MinecraftAccount): Boolean {
            return account.accountType == AccountType.MICROSOFT.type
        }

        @JvmStatic
/**
 * isNoLoginRequiredする
 */
        fun isNoLoginRequired(account: MinecraftAccount?): Boolean {
            return account == null || account.accountType == AccountType.LOCAL.type
        }

        @JvmStatic
/**
 * getAccountTypeNameする
 */
        fun getAccountTypeName(context: Context, account: MinecraftAccount): String {
            return if (isMicrosoftAccount(account)) {
                context.getString(R.string.account_microsoft_account)
            } else if (isOtherLoginAccount(account)) {
                account.accountType ?: ""
            } else {
                context.getString(R.string.account_local_account)
            }
        }

        /**
         * 修改自源代码：[HMCL Core: AuthlibInjectorServer.java](https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L60-#L76)
         * <br>原项目版权归原作者所有，遵循GPL v3协议
         */
/**
 * tryGetFullServerUrlする
 */
        fun tryGetFullServerUrl(baseUrl: String): String {
/**
 * Stringする
 */
            fun String.addSlashIfMissing(): String {
                if (!endsWith("/")) return "$this/"
                return this
            }

            var url = addHttpsIfMissing(baseUrl)
            runCatching {
                var conn = URL(url).openConnection() as HttpURLConnection
                conn.getHeaderField("x-authlib-injector-api-location")?.let { ali ->
                    val absoluteAli = URL(conn.url, ali)
                    url = url.addSlashIfMissing()
                    val absoluteUrl = absoluteAli.toString().addSlashIfMissing()
                    if (url != absoluteUrl) {
                        conn.disconnect()
                        url = absoluteUrl
                        conn = absoluteAli.openConnection() as HttpURLConnection
                    }
                }

                return url.addSlashIfMissing()
            }.getOrElse { e ->
                Logging.e("getFullServerUrl", Tools.printToString(e))
            }
            return baseUrl
        }

        /**
         * 修改自源代码：[HMCL Core: AuthlibInjectorServer.java](https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L90-#L96)
         * <br>原项目版权归原作者所有，遵循GPL v3协议
         */
/**
 * addHttpsIfMissingする
 */
        private fun addHttpsIfMissing(baseUrl: String): String {
            return if (!baseUrl.startsWith("http://", true) && !baseUrl.startsWith("https://")) {
                "https://$baseUrl".lowercase(Locale.ROOT)
            } else baseUrl.lowercase(Locale.ROOT)
        }
    }
}
