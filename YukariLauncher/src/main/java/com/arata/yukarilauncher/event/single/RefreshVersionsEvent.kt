package com.arata.yukarilauncher.event.single

/**
 * 当版本列表刷新时，通过此事件进行通知
 * 刷新版本是异步进行的，所以需要确保接收事件时，在UI线程运行
 * @see com.arata.yukarilauncher.feature.version.VersionsManager
 */
class RefreshVersionsEvent(val mode: MODE) {
    enum class MODE {
        START, END
    }
}