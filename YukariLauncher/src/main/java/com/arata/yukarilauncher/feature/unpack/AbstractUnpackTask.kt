package com.arata.yukarilauncher.feature.unpack

abstract class AbstractUnpackTask: Runnable {
/**
 * isNeedUnpackする
 */
    abstract fun isNeedUnpack(): Boolean
    protected var listener: OnTaskRunningListener? = null

/**
 * setTaskRunningListenerする
 */
    fun setTaskRunningListener(listener: OnTaskRunningListener) {
        this.listener = listener
    }
}