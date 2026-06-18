package com.arata.yukarilauncher.feature.unpack

interface OnTaskRunningListener {
    fun onTaskStart()
    fun onTaskEnd()
    fun onProgress(curr: Long, max: Long) {}
}