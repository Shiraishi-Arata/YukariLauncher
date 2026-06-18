package com.kdt.mcgui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.task.ProgressKeeper;
import com.arata.yukarilauncher.task.ProgressListener;
import com.arata.yukarilauncher.task.TaskCountListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressLayout extends ConstraintLayout implements View.OnClickListener, TaskCountListener {
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_MINECRAFT = "download_minecraft";
    public static final String DOWNLOAD_VERSION_LIST = "download_verlist";
    public static final String LOGIN_ACCOUNT = "login_account";
    public static final String INSTALL_RESOURCE = "install_resource";
    public static final String CHECKING_MODS = "checking_mods";

    private final Map<String, LayoutProgressListener> mMap = new HashMap<>();
    private final TextView mTaskNumberDisplayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog mDialog;
    private LinearLayout popupContainer;

/**
 * ProgressLayoutを構築します
 */
    public ProgressLayout(@NonNull Context context) {
        this(context, null);
    }

/**
 * ProgressLayoutを構築します
 */
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

/**
 * ProgressLayoutを構築します
 */
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        FrameLayout indicator = new FrameLayout(context);
        LayoutParams lp = new LayoutParams(dp(40), dp(40));
        indicator.setLayoutParams(lp);

        ProgressBar spinner = new ProgressBar(context);
        FrameLayout.LayoutParams spinnerLp = new FrameLayout.LayoutParams(dp(40), dp(40));
        spinnerLp.gravity = Gravity.CENTER;
        indicator.addView(spinner, spinnerLp);

        mTaskNumberDisplayer = new TextView(context);
        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textLp.gravity = Gravity.CENTER;
        mTaskNumberDisplayer.setTextColor(getContext().getColor(R.color.primary_text));
        mTaskNumberDisplayer.setTextSize(12);
        indicator.addView(mTaskNumberDisplayer, textLp);

        addView(indicator);
        setOnClickListener(this);
        setVisibility(GONE);
    }

/**
 * observeメソッド
 */
    public void observe(String progressKey) {
        if (mMap.containsKey(progressKey)) return;
        mMap.put(progressKey, new LayoutProgressListener(progressKey));
    }

/**
 * unObserveメソッド
 */
    public void unObserve(String progressKey) {
        LayoutProgressListener listener = mMap.remove(progressKey);
        if (listener != null) ProgressKeeper.removeListener(progressKey, listener);
    }

/**
 * cleanUpObserversメソッド
 */
    public void cleanUpObservers() {
        mMap.forEach((key, listener) -> ProgressKeeper.removeListener(key, listener));
        handler.removeCallbacksAndMessages(null);
    }

/**
 * hasProcessesメソッド
 */
    public boolean hasProcesses() {
        return ProgressKeeper.getTaskCount() > 0;
    }

/**
 * progressを設定する
 * @param progress 設定値
 */
    public static void setProgress(String progressKey, int progress, @StringRes int resource, Object... message) {
        ProgressKeeper.submitProgress(progressKey, progress, resource, message);
    }

/**
 * clearProgressメソッド
 */
    public static void clearProgress(String progressKey) {
        setProgress(progressKey, -1, -1);
    }

    @Override
    public void onClick(View v) {
        if (ProgressKeeper.getTaskCount() <= 0) return;
        showProgressPopup();
    }

/**
 * showProgressPopupメソッド
 */
    private void showProgressPopup() {
        if (mDialog != null && mDialog.isShowing()) mDialog.dismiss();

        popupContainer = new LinearLayout(getContext());
        popupContainer.setOrientation(LinearLayout.VERTICAL);
        popupContainer.setPadding(dp(12), dp(12), dp(12), dp(12));

        mDialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle(getContext().getString(R.string.progresslayout_tasks_in_progress, ProgressKeeper.getTaskCount()))
                .setView(popupContainer)
                .setPositiveButton(R.string.generic_close, null)
                .create();
        mDialog.setOnDismissListener(dialog -> handler.removeCallbacks(refreshPopupRunnable));
        mDialog.show();

        refreshPopupRunnable.run();
    }

    private final Runnable refreshPopupRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDialog == null || !mDialog.isShowing()) return;

            List<ProgressKeeper.ProgressRecord> records = ProgressKeeper.getProgressRecords();
            if (records.isEmpty()) {
                mDialog.dismiss();
                return;
            }

            popupContainer.removeAllViews();
            for (ProgressKeeper.ProgressRecord record : records) {
                MaterialCardView card = new MaterialCardView(getContext());
                card.setCardBackgroundColor(getContext().getColor(R.color.background_menu_element));
                card.setStrokeColor(getContext().getColor(R.color.settings_category));
                card.setStrokeWidth(dp(1));
                card.setRadius(dp(10));

                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(10), dp(10), dp(10), dp(10));

                TextView label = new TextView(getContext());
                label.setText(record.key);
                label.setTextColor(getContext().getColor(R.color.primary_text));
                row.addView(label);

                LinearProgressIndicator progressIndicator = new LinearProgressIndicator(getContext());
                progressIndicator.setTrackThickness(dp(8));
                progressIndicator.setTrackCornerRadius(dp(4));
                progressIndicator.setMax(100);
                progressIndicator.setProgressCompat(Math.max(0, Math.min(100, record.progress)), true);
                row.addView(progressIndicator, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                card.addView(row);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                cardParams.bottomMargin = dp(8);
                popupContainer.addView(card, cardParams);
            }

            mDialog.setTitle(getContext().getString(R.string.progresslayout_tasks_in_progress, records.size()));
            handler.postDelayed(this, 500);
        }
    };

    @Override
    public void onUpdateTaskCount(int tc) {
        post(() -> {
            if (tc > 0) {
                mTaskNumberDisplayer.setText(String.valueOf(tc));
                setVisibility(VISIBLE);
            } else {
                setVisibility(GONE);
                if (mDialog != null && mDialog.isShowing()) mDialog.dismiss();
            }
        });
    }

/**
 * dpメソッド
 */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

/**
 * LayoutProgressListener内部クラス
 */
    class LayoutProgressListener implements ProgressListener {
        final String progressKey;

        LayoutProgressListener(String progressKey) {
            this.progressKey = progressKey;
            ProgressKeeper.addListener(progressKey, this);
        }

        @Override
        public void onProgressStarted() {}

        @Override
        public void onProgressUpdated(int progress, int resid, Object... va) {}

        @Override
        public void onProgressEnded() {}
    }
}
