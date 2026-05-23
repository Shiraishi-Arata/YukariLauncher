package net.kdt.pojavlaunch;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.arata.yukarilauncher.InfoCenter;
import com.arata.yukarilauncher.R;

/**
 * ストレージが利用できない場合に表示するアクティビティ。
 */
public class MissingStorageActivity extends AppCompatActivity {
    /**
     * アクティビティ作成時にレイアウトを設定し、警告テキストを表示します。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_test_no_sdcard);
        ((TextView) findViewById(R.id.warning_text)).setText(InfoCenter.replaceName(this, R.string.storage_required));
    }
}
