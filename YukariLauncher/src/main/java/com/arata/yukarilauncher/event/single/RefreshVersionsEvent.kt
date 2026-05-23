package com.arata.yukarilauncher.event.single

/**
 * バージョンリストが更新されたときに通知するイベント
 * バージョンの更新は非同期で行われるため、イベント受信時はUIスレッドで実行する必要がある
 * @see com.arata.yukarilauncher.feature.version.VersionsManager
 */
class RefreshVersionsEvent(val mode: MODE) {
    enum class MODE {
        START, END
    }
}
