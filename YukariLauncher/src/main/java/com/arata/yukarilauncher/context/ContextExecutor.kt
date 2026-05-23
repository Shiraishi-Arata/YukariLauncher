package com.arata.yukarilauncher.context

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import com.arata.yukarilauncher.task.TaskExecutors
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import java.lang.ref.WeakReference

class ContextExecutor {
    companion object {
        private var sApplication: WeakReference<Application>? = null
        private var sActivity: WeakReference<Activity>? = null

        /**
         * Activityが利用できない場合にタスクを実行するためのApplicationを設定する
         * @param application フォールバックとして使用するApplication
         */
        @JvmStatic
        fun setApplication(application: Application) {
            this.sApplication = WeakReference(application)
        }

        /**
         * 設定されたApplicationをクリアする
         * アプリケーションがシステムによって終了された後にコードが実行される重大なエラーを
         * ContextExecutorがユーザーに通知できるようにする
         */
        @JvmStatic
        fun clearApplication() {
            this.sApplication?.clear()
        }

        /**
         * ContextExecutorがタスクの実行に使用するActivityを設定する
         * @param activity 使用するActivity
         */
        @JvmStatic
        fun setActivity(activity: Activity) {
            this.sActivity = WeakReference(activity)
        }

        /**
         * 設定されたActivityをクリアする
         * ContextExecutorがタスクの実行にそれを使用しないようにする
         */
        @JvmStatic
        fun clearActivity() {
            this.sActivity?.clear()
        }

        /**
         * ContextExecutorTaskを実行するようにスケジュールする
         * @see ContextExecutorTask タスクの詳細についてはこちら
         * @param task 実行するタスク
         */
        @JvmStatic
        fun executeTask(task: ContextExecutorTask) {
            execute(
                activity = { activity ->
                    task.executeWithActivity(activity)
                },
                application = { application ->
                    task.executeWithApplication(application)
                }
            )
        }

        /**
         * Contextの種類に関わらず、このContextを使用してタスクを実行する
         * @see AllContextExecutorTask
         * @param task 実行したいタスク
         */
        @JvmStatic
        fun executeTaskWithAllContext(task: AllContextExecutorTask) {
            execute(
                activity = { task.execute(it) },
                application = { task.execute(it) }
            )
        }

        /**
         * ActivityまたはApplicationのContextを使用してタスクを実行する
         * 内部処理用。まずActivityを試し、なければApplicationを使用する
         */
        private fun execute(activity: (Activity) -> Unit, application: (Application) -> Unit) {
            TaskExecutors.runInUIThread {
                Tools.getWeakReference(this.sActivity)?.let {
                    activity(it)
                    return@runInUIThread
                }
                Tools.getWeakReference(this.sApplication)?.let {
                    application(it)
                    return@runInUIThread
                }
                throw RuntimeException("The Context has not been set!")
            }
        }

        /**
         * 保存されたActivityからリソース文字列を取得する
         * Activityが設定されていない、またはリソースが見つからない場合はApplicationを試す
         * それでも失敗した場合は例外がスローされる
         */
        @JvmStatic
        fun getString(resId: Int): String {
            return (this.sActivity?.get()?.getString(resId) ?: this.sApplication?.get()?.getString(resId))!!
        }

        /**
         * Java言語からこのクラスを使ってToastを表示するための簡易メソッド
         * @param resId 表示するテキストのリソースID
         * @param duration 表示時間（LENGTH_SHORT / LENGTH_LONG）
         */
        @JvmStatic
        fun showToast(resId: Int, duration: Int) {
            executeTaskWithAllContext { context -> Toast.makeText(context, context.getString(resId), duration).show() }
        }

        /**
         * Java言語からこのクラスを使ってToastを表示するための簡易メソッド
         * @param string 表示するテキスト
         * @param duration 表示時間（LENGTH_SHORT / LENGTH_LONG）
         */
        @JvmStatic
        fun showToast(string: String, duration: Int) {
            executeTaskWithAllContext { context -> Toast.makeText(context, string, duration).show() }
        }

        /**
         * Activityを取得する
         * @throws RuntimeException Activityが存在しない場合にスローされる
         */
        @JvmStatic
        fun getActivity(): Activity {
            return this.sActivity?.get() ?: throw RuntimeException("Activity does not exist.")
        }

        /**
         * Applicationを取得する
         * @throws RuntimeException Applicationが存在しない場合にスローされる
         */
        @JvmStatic
        fun getApplication(): Application {
            return this.sApplication?.get() ?: throw RuntimeException("Application does not exist.")
        }
    }

    /**
     * AllContextExecutorTaskは、実行に使用されるContextに基づいて動作を動的に変更できるタスク
     * 例えば、Activityが死んだ後にServiceと共に生存するバックグラウンドスレッドからの
     * エラー通知や終了通知を実装するために使用できる
     */
    fun interface AllContextExecutorTask {
        /**
         * ActivityまたはApplicationのContextと共に実行されるタスク
         * @param context ActivityまたはApplicationのContext
         */
        fun execute(context: Context)
    }
}
