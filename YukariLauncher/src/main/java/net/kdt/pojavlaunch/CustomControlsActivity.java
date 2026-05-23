package net.kdt.pojavlaunch;

import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.drawerlayout.widget.DrawerLayout;

import com.arata.yukarilauncher.databinding.ActivityCustomControlsBinding;
import com.arata.yukarilauncher.databinding.ViewControlMenuBinding;
import com.arata.yukarilauncher.feature.background.BackgroundManager;
import com.arata.yukarilauncher.feature.background.BackgroundType;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.ui.activity.BaseActivity;
import com.arata.yukarilauncher.ui.subassembly.menu.ControlMenu;
import com.arata.yukarilauncher.ui.subassembly.view.GameMenuViewWrapper;

import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;

import java.io.File;
import java.io.IOException;

/**
 * カスタムコントロールの編集を行うアクティビティ。
 * コントロールレイアウトの編集・保存を提供します。
 */
public class CustomControlsActivity extends BaseActivity implements EditorExitable {
    public static final String BUNDLE_CONTROL_PATH = "control_path";
    private ActivityCustomControlsBinding binding;
    private String mControlPath = null;
    private boolean isVideoBackgroundPlaying;

    /**
     * アクティビティ作成時に呼び出されます。レイアウトの初期化、コントロールの読み込み、バックキーハンドラを設定します。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseBundle();
        binding = ActivityCustomControlsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ControlLayout controlLayout = binding.customctrlControllayout;
        DrawerLayout drawerLayout = binding.customctrlDrawerlayout;
        FrameLayout drawerNavigationView = binding.customctrlNavigationView;

        new GameMenuViewWrapper(this, v -> {
            boolean open = drawerLayout.isDrawerOpen(drawerNavigationView);
            if (open) drawerLayout.closeDrawer(drawerNavigationView);
            else drawerLayout.openDrawer(drawerNavigationView);
        }, false).setVisibility(true);

        refreshBackground();

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        ViewControlMenuBinding controlMenuBinding = ViewControlMenuBinding.inflate(getLayoutInflater());
        new ControlMenu(this, this, controlMenuBinding, controlLayout, true);

        drawerNavigationView.addView(controlMenuBinding.getRoot());
        controlLayout.setModifiable(true);
        try {
            if (mControlPath == null) controlLayout.loadLayout((String) null);
            else controlLayout.loadLayout(mControlPath);
        } catch (IOException e) {
            Tools.showError(this, e);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.customctrlControllayout.askToExit(CustomControlsActivity.this);
            }
        });
    }

    /**
     * インテントからコントロールパスを解析します。
     */
    private void parseBundle() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mControlPath = bundle.getString(BUNDLE_CONTROL_PATH);
        }
    }

    /**
     * ノッチを無視するかどうかを返します。
     */
    @Override
    public boolean shouldIgnoreNotch() {
        return AllSettings.getIgnoreNotch().getValue();
    }

    /**
     * エディタを終了し、アクティビティを閉じます。
     */
    @Override
    public void exitEditor() {
        finish();
    }

    /**
     * アクティビティ再開時にビデオ背景を再生します。
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (isVideoBackgroundPlaying) {
            binding.backgroundVideoView.start();
        }
    }

    /**
     * アクティビティ一時停止時にビデオ背景を一時停止します。
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (isVideoBackgroundPlaying && binding.backgroundVideoView.isPlaying()) {
            binding.backgroundVideoView.pause();
        }
    }

    /**
     * アクティビティ破棄時にビデオ背景を停止します。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideoBackground();
    }

    /**
     * 背景画像またはビデオをリフレッシュして表示します。
     */
    private void refreshBackground() {
        File mediaFile = BackgroundManager.getBackgroundImage(BackgroundType.CUSTOM_CONTROLS);
        if (mediaFile != null && BackgroundManager.isVideo(mediaFile)) {
            playVideoBackground(mediaFile);
            return;
        }
        stopVideoBackground();
        BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null);
    }

    /**
     * ビデオ背景を再生します。
     * @param videoFile 再生するビデオファイル
     */
    private void playVideoBackground(File videoFile) {
        binding.backgroundView.setImageDrawable(null);
        binding.backgroundView.setVisibility(View.GONE);
        binding.backgroundVideoView.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.backgroundVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        }
        binding.backgroundVideoView.setVideoPath(videoFile.getAbsolutePath());
        binding.backgroundVideoView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.setLooping(true);
            binding.backgroundVideoView.start();
            isVideoBackgroundPlaying = true;
        });
        binding.backgroundVideoView.setOnErrorListener((mp, what, extra) -> {
            stopVideoBackground();
            BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null);
            return true;
        });
    }

    /**
     * ビデオ背景を停止し、静止画背景に切り替えます。
     */
    private void stopVideoBackground() {
        if (isVideoBackgroundPlaying) {
            binding.backgroundVideoView.stopPlayback();
        }
        isVideoBackgroundPlaying = false;
        binding.backgroundVideoView.setVisibility(View.GONE);
        binding.backgroundView.setVisibility(View.VISIBLE);
    }
}
