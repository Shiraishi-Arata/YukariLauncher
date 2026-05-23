package net.kdt.pojavlaunch.customcontrols.gamepad;

import android.view.KeyEvent;

/**
 * このクラスは、ゲームパッドに物理的には存在しないが、
 * emulated from other inputs on it (like WASD directional keys)
 */
public class GamepadEmulatedButton {
    public short[] keycodes;
    protected boolean mIsDown = false;
/**
 * 「update」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void update(KeyEvent event) {
        boolean isKeyDown = (event.getAction() == KeyEvent.ACTION_DOWN);
        update(isKeyDown);
    }
/**
 * 「update」メソッド。
 * このクラスに定義された機能メソッドです。
 */
    public void update(boolean isKeyDown){
        if(isKeyDown != mIsDown){
            mIsDown = isKeyDown;
            onDownStateChanged(mIsDown);
        }
    }
/**
 * 「reset Button State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    public void resetButtonState() {
        if(mIsDown) Gamepad.sendInput(keycodes, false);
        mIsDown = false;
    }
/**
 * 「on Down State Changed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    protected void onDownStateChanged(boolean isDown) {
        Gamepad.sendInput(keycodes, mIsDown);
    }
}
