package com.arata.yukarilauncher.ui.activity

import android.os.Bundle
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ActivityHostServerBinding
import com.arata.yukarilauncher.ui.fragment.HostServerFragment

/**
 * サーバーホストアクティビティ
 */
class HostServerActivity : BaseActivity() {

    private lateinit var binding: ActivityHostServerBinding

    /**
     * アクティビティ作成時にレイアウトを設定し、HostServerFragmentを追加する
     * @param savedInstanceState 保存されたインスタンス状態
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, HostServerFragment())
                .commit()
        }
    }

    /**
     * 戻るボタン押下時の処理
     * バックスタックに履歴がある場合はポップバックする
     */
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
    /**
     * ノッチ領域を無視するかどうかを返す
     * @return 常にtrue
     */
    override fun shouldIgnoreNotch(): Boolean = true
}
