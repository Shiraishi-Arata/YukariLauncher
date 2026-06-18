package com.arata.yukarilauncher.task

import java.util.concurrent.Callable

/**
 * Callableをラップしたシンプルなタスク実装
 * @param V 結果の型
 */
class SimpleTask<V>(private val callable: Callable<V>) : Task<V>() {
    /**
     * Callableを実行し、結果を設定する
     */
    override fun performMainTask() {
        setResult(callable.call())
    }
}