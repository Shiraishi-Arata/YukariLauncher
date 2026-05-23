package net.kdt.pojavlaunch.customcontrols.mouse;

import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;

import android.os.Handler;

import com.arata.yukarilauncher.setting.AllStaticSettings;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

public class DropGesture implements Runnable{
    private final Handler mHandler;
    private boolean mActive;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
    public DropGesture(Handler mHandler) {
        this.mHandler = mHandler;
    }
/**
 * 「submit」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void submit() {
        if(!mActive) {
            mActive = true;
            mHandler.postDelayed(this, AllStaticSettings.timeLongPressTrigger);
        }
    }
/**
 * 「cancel」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void cancel() {
        mActive = false;
        mHandler.removeCallbacks(this);
    }
/**
 * 「run」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    @Override
    public void run() {
        if(!mActive) return;
        sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_Q);
        mHandler.postDelayed(this, 250);
    }
}
