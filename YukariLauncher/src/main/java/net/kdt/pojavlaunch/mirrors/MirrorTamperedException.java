package net.kdt.pojavlaunch.mirrors;

import android.app.Activity;
import android.content.Context;
import android.text.Html;

import androidx.appcompat.app.AlertDialog;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.setting.AllSettings;

import net.kdt.pojavlaunch.ShowErrorActivity;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;

/**
 * ミラーが改ざんされたことを示す例外。ContextExecutorTaskとしてダイアログを表示します。
 */
public class MirrorTamperedException extends Exception implements ContextExecutorTask {
    // 変更しないでください。Androidは何らかの理由でこの値が変更されることを非常に嫌います。
    private static final long serialVersionUID = -7482301619612640658L;

    /**
     * アクティビティを使用して改ざん警告ダイアログを表示します。
     */
    @Override
    public void executeWithActivity(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme);
        builder.setTitle(R.string.dl_tampered_manifest_title);
        builder.setMessage(Html.fromHtml(activity.getString(R.string.dl_tampered_manifest)));
        addButtons(builder);
        ShowErrorActivity.installRemoteDialogHandling(activity, builder);
        builder.show();
    }

    /**
     * ダイアログにボタンを追加します。
     */
    private void addButtons(AlertDialog.Builder builder) {
        builder.setPositiveButton(R.string.dl_switch_to_official_site, (d,w) -> AllSettings.getDownloadSource().reset());
        builder.setNegativeButton(R.string.dl_turn_off_manifest_checks, (d,w) -> AllSettings.getVerifyManifest().put(false).save());
        builder.setNeutralButton(android.R.string.cancel, (d,w) -> {});
    }

    @Override
    public void executeWithApplication(Context context) {}
}
