package com.arata.yukarilauncher.event.value

/**
 * 新しいダウンロードタスクが発生したときに、LauncherActivityにタスクのキーを通知するイベント
 * このタスクのダウンロード進捗を監視するために使用する
 * @param progressKey ダウンロード進捗のキー
 * @param observe 監視を継続するかどうか
 * @see com.arata.yukarilauncher.ui.activity.LauncherActivity
 */
class DownloadProgressKeyEvent(val progressKey: String, val observe: Boolean)
