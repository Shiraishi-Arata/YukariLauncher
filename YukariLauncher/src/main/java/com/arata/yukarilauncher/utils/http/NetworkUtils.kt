package com.arata.yukarilauncher.utils.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkUtils {
    companion object {
        /**
         * 現在のネットワークが接続されているかどうかを返す
         * Wi-Fi、モバイルデータ、イーサネットのいずれかに接続している場合にtrueを返す
         */
        @JvmStatic
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return activeNetwork != null && (
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false
                    )
        }
    }
}