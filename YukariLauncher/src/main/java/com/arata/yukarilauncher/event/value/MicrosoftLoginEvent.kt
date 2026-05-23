package com.arata.yukarilauncher.event.value

import android.net.Uri

/**
 * Microsoftアカウントでのログインが行われたことを通知するイベント
 * @param uri ログインコールバックのURI
 */
data class MicrosoftLoginEvent(val uri: Uri)
