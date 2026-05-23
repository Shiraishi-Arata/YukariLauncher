package com.arata.yukarilauncher.utils.platform

import android.app.ActivityManager
import android.content.Context

class MemoryUtils {
    companion object {
        private var activityManager: ActivityManager? = null

        /**
         * ActivityManagerを初期化する
         */
        private fun init(context: Context) {
            activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        }

        /**
         * デバイスの総メモリ容量を取得する
         */
        @JvmStatic
        fun getTotalDeviceMemory(context: Context): Long {
            activityManager ?: run { init(context) }

            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            return memInfo.totalMem
        }

        /**
         * 使用中のデバイスメモリ容量を取得する
         */
        @JvmStatic
        fun getUsedDeviceMemory(context: Context): Long {
            activityManager ?: run { init(context) }

            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            return memInfo.totalMem - memInfo.availMem
        }

        /**
         * 空きデバイスメモリ容量を取得する
         */
        @JvmStatic
        fun getFreeDeviceMemory(context: Context): Long {
            activityManager ?: run { init(context) }

            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            return memInfo.availMem
        }
    }
}
