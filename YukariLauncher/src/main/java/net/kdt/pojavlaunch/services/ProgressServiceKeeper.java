package net.kdt.pojavlaunch.services;

import android.content.Context;

import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

/**
 * タスク数を監視し、タスクが存在する場合にProgressServiceを開始するリスナー。
 */
public class ProgressServiceKeeper implements TaskCountListener {
    private final Context context;

    /**
     * @param ctx サービス開始に使用するコンテキスト
     */
    public ProgressServiceKeeper(Context ctx) {
        this.context = ctx;
    }

    /**
     * タスク数が0より大きい場合にProgressServiceを開始します。
     */
    @Override
    public void onUpdateTaskCount(int taskCount) {
        if(taskCount > 0) ProgressService.startService(context);
    }
}
