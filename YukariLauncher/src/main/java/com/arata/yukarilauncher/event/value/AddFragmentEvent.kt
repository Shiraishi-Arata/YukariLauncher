package com.arata.yukarilauncher.event.value

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 新しいFragmentをトランザクション管理に追加するイベント
 * LauncherActivityが受け取り処理する
 * Fragment追加時に親Fragmentが現在のFragmentであることを保証する
 * @see com.arata.yukarilauncher.ui.activity.LauncherActivity
 * @see com.arata.yukarilauncher.utils.YLTools.addFragment
 */
class AddFragmentEvent(
    val fragmentClass: Class<out Fragment?>,
    val fragmentTag: String?,
    val bundle: Bundle?,
    val fragmentActivityCallback: FragmentActivityCallBack?
) {
    /**
     * 現在のFragmentのFragmentActivityに対するコールバック処理
     */
    fun interface FragmentActivityCallBack {
        /**
         * コールバックを実行する
         * @param fragmentActivity 現在のFragmentActivity
         */
        fun callBack(fragmentActivity: FragmentActivity)
    }
}
