package net.kdt.pojavlaunch.lifecycle;

import android.app.Activity;
import android.content.Context;

/**
 * ContextExecutorTaskは、実行に使用されるコンテキストに基づいて動作を動的に変更できるタスクです。
 * 例えば、アクティビティが死んだ後にサービスとともに生き残るバックグラウンドスレッドからの
 * エラー通知や完了通知を実装するために使用できます。
 */
public interface ContextExecutorTask {
    /**
     * ContextExecutorは、フォアグラウンドのActivityが利用可能な場合に最初にこの関数を実行します。
     * @param activity アクティビティ
     */
    void executeWithActivity(Activity activity);

    /**
     * フォアグラウンドのActivityが利用できないが、アプリがまだ実行中の場合に
     * ContextExecutorはこの関数を実行します。
     * @param context アプリケーションコンテキスト
     */
    void executeWithApplication(Context context);
}
