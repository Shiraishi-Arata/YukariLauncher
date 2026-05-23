package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.arata.yukarilauncher.R;

import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;
import net.kdt.pojavlaunch.utils.NotificationUtils;

import java.io.Serializable;

/**
 * エラーを表示するためのアクティビティ。リモートエラーハンドリングもサポートします。
 */
public class ShowErrorActivity extends Activity {

    private static final String ERROR_ACTIVITY_REMOTE_TASK = "remoteTask";

    /**
     * インテントからエラータスクを取得し、アクティビティとともに実行します。
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent == null) {
            finish();
            return;
        }
        RemoteErrorTask remoteErrorTask = (RemoteErrorTask) intent.getSerializableExtra(ERROR_ACTIVITY_REMOTE_TASK);
        if(remoteErrorTask == null) {
            finish();
            return;
        }
        remoteErrorTask.executeWithActivity(this);
    }

    /**
     * コンテキストに応じてエラーを表示するリモートエラータスク。シリアライズ可能。
     */
    public static class RemoteErrorTask implements ContextExecutorTask, Serializable {
        private final Throwable mThrowable;
        private final String mRolledMsg;

        public RemoteErrorTask(Throwable mThrowable, String mRolledMsg) {
            this.mThrowable = mThrowable;
            this.mRolledMsg = mRolledMsg;
        }

        /**
         * アクティビティが利用可能な場合、エラーダイアログを表示します。
         */
        @Override
        public void executeWithActivity(Activity activity) {
            if(mThrowable instanceof ContextExecutorTask) {
                ((ContextExecutorTask)mThrowable).executeWithActivity(activity);
            }else {
                Tools.showError(activity, mRolledMsg, mThrowable, activity instanceof ShowErrorActivity);
            }
        }

        /**
         * アクティビティが利用できない場合、通知でエラーを表示します。
         */
        @Override
        public void executeWithApplication(Context context) {
            Intent showErrorIntent = new Intent(context, ShowErrorActivity.class);
            showErrorIntent.putExtra(ERROR_ACTIVITY_REMOTE_TASK, this);
            NotificationUtils.sendBasicNotification(context,
                    R.string.notif_error_occured,
                    R.string.notif_error_occured_desc,
                    showErrorIntent,
                    NotificationUtils.PENDINGINTENT_CODE_SHOW_ERROR,
                    NotificationUtils.NOTIFICATION_ID_SHOW_ERROR
            );
        }
    }

    /**
     * リモートダイアログ処理をダイアログにインストールします。
     * ShowErrorActivity経由で表示されるダイアログは、閉じられたらアクティビティも終了します。
     * @param callerActivity ContextExecutorTask.executeWithActivityから提供されるアクティビティ
     * @param builder アラートダイアログビルダー
     */
    public static void installRemoteDialogHandling(Activity callerActivity, @NonNull AlertDialog.Builder builder) {
        if (callerActivity instanceof ShowErrorActivity) {
            builder.setOnDismissListener(d -> callerActivity.finish());
        }
    }
}
