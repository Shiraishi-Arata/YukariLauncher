package net.kdt.pojavlaunch.authenticator.listener;

import net.kdt.pojavlaunch.value.MinecraftAccount;

/**
 * ログインが完了しアカウントが受信されたときに呼び出されます。UIスレッドで実行されることが保証されています。
 */
public interface DoneListener {
    /**
     * ログインが完了したときに呼び出されます。
     * @param account ログインしたアカウント
     */
    void onLoginDone(MinecraftAccount account);
}
