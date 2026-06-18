package com.arata.yukarilauncher.feature.discord

import android.content.SharedPreferences
import com.arata.yukarilauncher.context.ContextExecutor
import com.arata.yukarilauncher.feature.log.Logging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Discord RPC関連の設定をSharedPreferencesで管理するユーティリティ。
 * アカウント情報、RPC有効/無効、選択中アカウント、画像プロキシキャッシュを永続化します。
 */
object DiscordPrefs {
    private const val PREF_NAME = "discord_prefs"
    private const val KEY_ACCOUNTS = "discord_accounts"
    private const val KEY_ENABLED = "discord_rpc_enabled"
    private const val KEY_SELECTED_ACCOUNT = "discord_selected_account"
    private const val KEY_LAST_ACTIVITY = "discord_last_activity_time"
    private const val KEY_IMAGE_CACHE = "discord_image_cache"
    private const val KEY_CUSTOM_BUTTON_LABEL = "discord_custom_button_label"
    private const val KEY_CUSTOM_BUTTON_URL = "discord_custom_button_url"

    private val gson = Gson()

    private fun prefs(): SharedPreferences {
        return ContextExecutor.getApplication().getSharedPreferences(PREF_NAME, 0)
    }

    /** 保存済みのDiscordアカウント一覧を取得します。 */
    fun getAccounts(): MutableList<DiscordAccount> {
        val json = prefs().getString(KEY_ACCOUNTS, null) ?: return mutableListOf()
        val type = object : TypeToken<List<DiscordAccount>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /** アカウント一覧を保存します（同期的にコミット）。 */
    fun saveAccounts(accounts: List<DiscordAccount>) {
        prefs().edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).commit()
    }

    /** アカウントを追加します（同一IDの場合は上書き）。 */
    fun addAccount(account: DiscordAccount) {
        val accounts = getAccounts().toMutableList()
        accounts.removeAll { it.id == account.id }
        accounts.add(account)
        saveAccounts(accounts)
    }

    /** アカウントを削除し、選択中アカウントをクリアします。 */
    fun removeAccount(accountId: String) {
        val accounts = getAccounts().toMutableList()
        accounts.removeAll { it.id == accountId }
        saveAccounts(accounts)
        setSelectedAccountId(null)
    }

    /** ログインデータを全てクリアします（RPC有効設定は維持）。 */
    fun clearLoginData() {
        val rpcEnabled = isRpcEnabled()
        val committed = prefs().edit()
            .putString(KEY_ACCOUNTS, "[]")
            .remove(KEY_SELECTED_ACCOUNT)
            .remove(KEY_LAST_ACTIVITY)
            .remove(KEY_IMAGE_CACHE)
            .putBoolean(KEY_ENABLED, rpcEnabled)
            .commit()
        Logging.i("DiscordPrefs", "Login data cleared: success=$committed")
    }

    /** 選択中アカウントをクリアします。 */
    fun clearSelectedAccount() {
        prefs().edit().remove(KEY_SELECTED_ACCOUNT).commit()
    }

    /** 全てのDiscord関連設定を削除します。 */
    fun clearAll() {
        prefs().edit()
            .remove(KEY_ACCOUNTS)
            .remove(KEY_ENABLED)
            .remove(KEY_SELECTED_ACCOUNT)
            .remove(KEY_LAST_ACTIVITY)
            .remove(KEY_IMAGE_CACHE)
            .commit()
    }

    /** RPC機能が有効かどうかを返します。 */
    fun isRpcEnabled(): Boolean = prefs().getBoolean(KEY_ENABLED, false)

    /** RPC機能の有効/無効を設定します。 */
    fun setRpcEnabled(enabled: Boolean) = prefs().edit().putBoolean(KEY_ENABLED, enabled).commit()

    /** 選択中のアカウントIDを取得します。 */
    fun getSelectedAccountId(): String? = prefs().getString(KEY_SELECTED_ACCOUNT, null)

    /** 選択中のアカウントIDを設定します。 */
    fun setSelectedAccountId(accountId: String?) = prefs().edit().putString(KEY_SELECTED_ACCOUNT, accountId).commit()

    /** 最終アクティビティ時刻を取得します（Idle判定用）。 */
    fun getLastActivityTime(): Long = prefs().getLong(KEY_LAST_ACTIVITY, 0L)

    /** 最終アクティビティ時刻を設定します。 */
    fun setLastActivityTime(time: Long) = prefs().edit().putLong(KEY_LAST_ACTIVITY, time).commit()

    /** 画像プロキシキャッシュ（external_asset_path）を取得します。 */
    fun getImagePaths(): Map<String, String> {
        val json = prefs().getString(KEY_IMAGE_CACHE, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(json, type) ?: emptyMap() } catch (e: Exception) { emptyMap() }
    }

    /** 画像プロキシキャッシュを保存します。 */
    fun saveImagePaths(paths: Map<String, String>) {
        prefs().edit().putString(KEY_IMAGE_CACHE, gson.toJson(paths)).commit()
    }

    /** カスタムボタンラベルを取得します。 */
    fun getCustomButtonLabel(): String = prefs().getString(KEY_CUSTOM_BUTTON_LABEL, "") ?: ""

    /** カスタムボタンラベルを設定します。 */
    fun setCustomButtonLabel(label: String) = prefs().edit().putString(KEY_CUSTOM_BUTTON_LABEL, label).commit()

    /** カスタムボタンURLを取得します。 */
    fun getCustomButtonUrl(): String = prefs().getString(KEY_CUSTOM_BUTTON_URL, "") ?: ""

    /** カスタムボタンURLを設定します。 */
    fun setCustomButtonUrl(url: String) = prefs().edit().putString(KEY_CUSTOM_BUTTON_URL, url).commit()
}
