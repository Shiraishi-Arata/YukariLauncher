package net.kdt.pojavlaunch.authenticator.listener;

/**
 * 完全な失敗が発生したときに呼び出されます。UIスレッドで実行されることが保証されています。
 */
public interface ErrorListener {
    /**
     * ログインエラーが発生したときに呼び出されます。
     * @param errorMessage エラーの詳細
     */
    void onLoginError(Throwable errorMessage);
}
