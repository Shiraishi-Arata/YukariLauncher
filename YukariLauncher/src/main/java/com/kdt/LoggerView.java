package com.kdt;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.arata.anim.animations.Animations;
import com.arata.yukarilauncher.databinding.ViewLoggerBinding;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils;

import com.arata.yukarilauncher.feature.log.Logger;

/**
 * ユーザーにログを表示するためのクラス
 * Loggerクラスに対応しており、ログの表示・非表示をアニメーションで切り替える機能を持つ
 */
public class LoggerView extends ConstraintLayout {
    private Logger.eventLogListener mLogListener;
    private ViewLoggerBinding binding;
    private boolean isShowing = false;

    /**
     * コンストラクタ
     */
    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * コンストラクタ（属性指定）
     */
    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    /**
     * 現在の表示状態をトグルで切り替える（アニメーション付き）
     */
    public void toggleViewWithAnim() {
        setVisibilityWithAnim(!isShowing);
    }

    /**
     * アニメーション付きで表示・非表示を設定する
     */
    public void setVisibilityWithAnim(boolean visibility) {
        if (isShowing == visibility) return;
        isShowing = visibility;

        ViewAnimUtils.setViewAnim(this,
                visibility ? Animations.BounceInUp : Animations.SlideOutDown,
                (long) (AllSettings.getAnimationSpeed().getValue() * 0.7),
                () -> setVisibility(VISIBLE),
                () -> setVisibility(visibility ? VISIBLE : GONE));
    }

    /**
     * ログ表示を強制する
     * 閉じるボタンが押された場合のコールバックを設定できる
     */
    public void forceShow(OnCloseClickListener listener) {
        setVisibilityWithAnim(true);
        binding.cancel.setOnClickListener(v -> listener.onClick());
    }

    /**
     * レイアウトをインフレートし、コンポーネントの動作を設定する
     */
    private void init() {
        binding = ViewLoggerBinding.inflate(LayoutInflater.from(getContext()), this, true);

        binding.logView.setTypeface(Typeface.MONOSPACE);
        //TODO 最大テキスト数を制限して画面からはみ出さないようにする
        binding.logView.setMaxLines(Integer.MAX_VALUE);
        binding.logView.setEllipsize(null);
        binding.logView.setVisibility(VISIBLE);

        // ログクリアボタン
        binding.clearLog.setOnClickListener(v -> binding.logView.setText(""));

        // ユーザーのビューからLoggerViewを削除する
        binding.cancel.setOnClickListener(view -> setVisibilityWithAnim(false));

        // スクロールビューの設定
        binding.scroll.setKeepFocusing(true);

        // 自動スクロールスイッチの設定
        binding.toggleAutoscroll.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) binding.scroll.fullScroll(View.FOCUS_DOWN);
                    binding.scroll.setKeepFocusing(isChecked);
                }
        );
        binding.toggleAutoscroll.setChecked(true);

        // ログのリスナー設定
        mLogListener = new Logger.eventLogListener() {
            @Override
            public void onEventLogged(String text) {
                if (binding.logView.getVisibility() != VISIBLE) return;
                post(() -> {
                    binding.logView.append(text + '\n');
                    if (binding.scroll.isKeepFocusing())
                        binding.scroll.fullScroll(View.FOCUS_DOWN);
                });
            }
        };
        Logger.setLogListener(mLogListener);
    }

    /**
     * ViewLoggerBindingを取得する
     */
    public ViewLoggerBinding getBinding() {
        return binding;
    }

    /**
     * 閉じるボタンクリック時のコールバックインターフェース
     */
    public interface OnCloseClickListener {
        void onClick();
    }
}
