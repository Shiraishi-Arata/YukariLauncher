package com.arata.yukarilauncher.event.value

/**
 * JVMが終了したことを通知するイベント
 * @param exitCode JVMの終了コード
 */
class JvmExitEvent(val exitCode: Int)