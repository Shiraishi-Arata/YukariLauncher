package com.arata.yukarilauncher.feature

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Minecraftゲームの実行状態を監視するシングルトン。
 * GameService.setActive()と連動し、ゲーム開始・終了を検出して
 * リスナーに通知します。ブロードキャストに依存しない信頼性の高い状態管理を提供します。
 */
object GameStateMonitor {

    @Volatile
    private var gameRunning = false

    private val listeners = CopyOnWriteArrayList<Listener>()

    fun interface Listener {
        fun onGameStateChanged(running: Boolean)
    }

    /** ゲームが実行中かどうかを返します。 */
    fun isGameRunning(): Boolean = gameRunning

    /** ゲーム状態変更リスナーを追加します。 */
    fun addListener(listener: Listener): Boolean = listeners.add(listener)

    /** ゲーム状態変更リスナーを削除します。 */
    fun removeListener(listener: Listener): Boolean = listeners.remove(listener)

    /**
     * ゲーム開始を通知します。
     * GameService.setActive(true) から呼び出されます。
     */
    fun notifyGameStarted() {
        if (gameRunning) return
        gameRunning = true
        listeners.forEach { it.onGameStateChanged(true) }
    }

    /**
     * ゲーム終了を通知します。
     * GameService.setActive(false) から呼び出されます。
     */
    fun notifyGameStopped() {
        if (!gameRunning) return
        gameRunning = false
        listeners.forEach { it.onGameStateChanged(false) }
    }
}
