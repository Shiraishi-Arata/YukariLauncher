/**
 * ストレージが利用できない場合に表示するアクティビティ。
 */
package com.arata.yukarilauncher.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.arata.yukarilauncher.InfoCenter
import com.arata.yukarilauncher.R

/**
 * ストレージが利用できない場合に表示するアクティビティ。
 */
class MissingStorageActivity : AppCompatActivity() {
    /** アクティビティ作成時にレイアウトを設定し、警告テキストを表示します */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_test_no_sdcard)
        findViewById<TextView>(R.id.warning_text).text = InfoCenter.replaceName(this, R.string.storage_required)
    }
}
