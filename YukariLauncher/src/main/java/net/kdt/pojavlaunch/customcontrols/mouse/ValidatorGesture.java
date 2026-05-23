package net.kdt.pojavlaunch.customcontrols.mouse;

import android.os.Handler;

/**
 * このクラスは抽象的な「バリデータージェスチャー」を実装し、
 * 指の位置追跡などを使用したより複雑なジェスチャーのベースとして使用されます。
 */
public abstract class ValidatorGesture implements Runnable{
    private final Handler mHandler;
    private boolean mGestureActive;

    /**
     * @param mHandler checkAndTrigger()メソッドのコールバックに使用されるHandler。
     * このHandlerはsubmit()/cancel()の呼び出し元と同じスレッドで実行する必要があります。
     */
    public ValidatorGesture(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * ジェスチャーを送信し、タイマーを開始してこのジェスチャーを「アクティブ」としてマークします。
     * ジェスチャーが既にアクティブだった場合、この呼び出しは無視されます
     * @return ジェスチャーが送信された場合はtrue、無視された場合はfalse
     */
    public final boolean submit() {
        if(mGestureActive) return false;
        mHandler.postDelayed(this, getGestureDelay());
        mGestureActive = true;
        return true;
    }

    /**
     * ジェスチャーをキャンセルし、タイマーを停止して「非アクティブ」としてマークします。
     * ジェスチャーが既に非アクティブだった場合、この呼び出しは無視されます。
     * @param isSwitching ユーザー操作によりジェスチャーがキャンセルされた場合はtrue（ユーザーが指を離した）、
     * プログラマーまたはOSからの要求でキャンセルされた場合はfalse。
     * checkAndTrigger()からfalseを返すこともユーザー操作としてカウントされます。
     */
    public final void cancel(boolean isSwitching) {
        if(!mGestureActive) return;
        mHandler.removeCallbacks(this);
        onGestureCancelled(isSwitching);
        mGestureActive = false;
    }
/**
 * 「run」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    @Override
    public final void run() {
        if(checkAndTrigger()) return;
        mGestureActive = false;
        onGestureCancelled(false);
    }

    /**
     * このメソッドはジェスチャー送信時に呼び出され、チェック時間を決定します。
     * @return 必要なジェスチャーチェック時間（ミリ秒）
     */
    protected abstract int getGestureDelay();

    /**
     * このメソッドは、ジェスチャーがキャンセルされなかった場合、getGestureDelay()ミリ秒後に呼び出されます。
     * @return このジェスチャーを「非アクティブ」としてマークする場合はfalse
     * それ以外の場合はtrue
     */
    public abstract boolean checkAndTrigger();

    /**
     * このメソッドは、cancel()メソッドまたはfalseの返却によってジェスチャーがキャンセルされた場合に呼び出されます。
     * checkAndTrigger()から呼び出されます。
     * @param isSwitching ユーザー操作によりジェスチャーがキャンセルされた場合はtrue（ユーザーが指を離した）、
     * プログラマーまたはOSからの要求でキャンセルされた場合はfalse。
     */
    public abstract void onGestureCancelled(boolean isSwitching);
}
