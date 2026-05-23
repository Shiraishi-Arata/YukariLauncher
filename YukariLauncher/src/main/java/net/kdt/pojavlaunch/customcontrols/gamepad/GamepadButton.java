package net.kdt.pojavlaunch.customcontrols.gamepad;

/**
 *  このクラスは、ゲームパッドに物理的に存在するボタンに対応します
 */
public class GamepadButton extends GamepadEmulatedButton {
    public boolean isToggleable = false;
    private boolean mIsToggled = false;
/**
 * 「on Down State Changed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

    @Override
    protected void onDownStateChanged(boolean isDown) {
        if(isToggleable) {
            if(!isDown) return;
            mIsToggled = !mIsToggled;
            Gamepad.sendInput(keycodes, mIsToggled);
            return;
        }
        super.onDownStateChanged(isDown);
    }
/**
 * 「reset Button State」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void resetButtonState() {
        if(!mIsDown && mIsToggled) {
            Gamepad.sendInput(keycodes, false);
            mIsToggled = false;
        } else {
            super.resetButtonState();
        }
    }
}
