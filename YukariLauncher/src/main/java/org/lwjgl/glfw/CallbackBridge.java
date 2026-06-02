package org.lwjgl.glfw;

import android.content.ClipData;
import android.content.ClipDescription;
import android.view.Choreographer;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.arata.yukarilauncher.ui.activity.MainActivity;
import com.arata.yukarilauncher.ui.view.GrabListener;
import com.arata.yukarilauncher.utils.LwjglGlfwKeycode;

import java.util.ArrayList;

import dalvik.annotation.optimization.CriticalNative;

public class CallbackBridge {
    public static final Choreographer sChoreographer = Choreographer.getInstance();
    private static boolean isGrabbing = false;
    private static final ArrayList<GrabListener> grabListeners = new ArrayList<>();
    
    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    public static final int CLIPBOARD_OPEN = 2002;
    public static final int GLFW_ARROW_CURSOR = 0x36001;
    
    public static volatile int windowWidth, windowHeight;
    public static volatile int physicalWidth, physicalHeight;
    public static float mouseX, mouseY;
    private static volatile int currentCursorType = GLFW_ARROW_CURSOR;
    public volatile static boolean holdingAlt, holdingCapslock, holdingCtrl,
            holdingNumlock, holdingShift;

/**
 * putMouseEventWithCoordsメソッド
 */
    public static void putMouseEventWithCoords(int button, float x, float y) {
        putMouseEventWithCoords(button, true, x, y);
        sChoreographer.postFrameCallbackDelayed(l -> putMouseEventWithCoords(button, false, x, y), 33);
    }
    
/**
 * putMouseEventWithCoordsメソッド
 */
    public static void putMouseEventWithCoords(int button, boolean isDown, float x, float y /* , int dz, long nanos */) {
        sendCursorPos(x, y);
        sendMouseKeycode(button, CallbackBridge.getCurrentMods(), isDown);
    }


/**
 * sendCursorPosメソッド
 */
    public static void sendCursorPos(float x, float y) {
        mouseX = x;
        mouseY = y;
        nativeSendCursorPos(mouseX, mouseY);
    }

/**
 * sendKeycodeメソッド
 */
    public static void sendKeycode(int keycode, char keychar, int scancode, int modifiers, boolean isDown) {
        // TODO CHECK: This may cause input issue, not receive input!
        if(keycode != 0)  nativeSendKey(keycode,scancode,isDown ? 1 : 0, modifiers);
        if(isDown && keychar != '\u0000') {
            nativeSendCharMods(keychar,modifiers);
            nativeSendChar(keychar);
        }
    }

/**
 * sendCharメソッド
 */
    public static void sendChar(char keychar, int modifiers){
        nativeSendCharMods(keychar,modifiers);
        nativeSendChar(keychar);
    }

/**
 * sendKeyPressメソッド
 */
    public static void sendKeyPress(int keyCode, int modifiers, boolean status) {
        sendKeyPress(keyCode, 0, modifiers, status);
    }

/**
 * sendKeyPressメソッド
 */
    public static void sendKeyPress(int keyCode, int scancode, int modifiers, boolean status) {
        sendKeyPress(keyCode, '\u0000', scancode, modifiers, status);
    }

/**
 * sendKeyPressメソッド
 */
    public static void sendKeyPress(int keyCode, char keyChar, int scancode, int modifiers, boolean status) {
        CallbackBridge.sendKeycode(keyCode, keyChar, scancode, modifiers, status);
    }

/**
 * sendKeyPressメソッド
 */
    public static void sendKeyPress(int keyCode) {
        sendKeyPress(keyCode, CallbackBridge.getCurrentMods(), true);
        sendKeyPress(keyCode, CallbackBridge.getCurrentMods(), false);
    }

/**
 * sendMouseButtonメソッド
 */
    public static void sendMouseButton(int button, boolean status) {
        CallbackBridge.sendMouseKeycode(button, CallbackBridge.getCurrentMods(), status);
    }

/**
 * sendMouseKeycodeメソッド
 */
    public static void sendMouseKeycode(int button, int modifiers, boolean isDown) {
        // if (isGrabbing()) DEBUG_STRING.append("MouseGrabStrace: " + android.util.Log.getStackTraceString(new Throwable()) + "\n");
        nativeSendMouseButton(button, isDown ? 1 : 0, modifiers);
    }

/**
 * sendMouseKeycodeメソッド
 */
    public static void sendMouseKeycode(int keycode) {
        sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), true);
        sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), false);
    }
    
/**
 * sendScrollメソッド
 */
    public static void sendScroll(double xoffset, double yoffset) {
        nativeSendScroll(xoffset, yoffset);
    }

/**
 * sendUpdateWindowSizeメソッド
 */
    public static void sendUpdateWindowSize(int w, int h) {
        nativeSendScreenSize(w, h);
    }

/**
 * grabbingを取得する
 * @return grabbingの値
 */
    public static boolean isGrabbing() {
        // Avoid going through the JNI each time.
        return isGrabbing;
    }

/**
 * currentCursorTypeを取得する
 * @return currentCursorTypeの値
 */
    public static int getCurrentCursorType() {
        return currentCursorType;
    }

    // Called from JRE side
    @SuppressWarnings("unused")
    public static @Nullable String accessAndroidClipboard(int type, String copy) {
        switch (type) {
            case CLIPBOARD_COPY:
                MainActivity.GLOBAL_CLIPBOARD.setPrimaryClip(ClipData.newPlainText("Copy", copy));
                return null;

            case CLIPBOARD_PASTE:
                if (MainActivity.GLOBAL_CLIPBOARD.hasPrimaryClip() && MainActivity.GLOBAL_CLIPBOARD.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    return MainActivity.GLOBAL_CLIPBOARD.getPrimaryClip().getItemAt(0).getText().toString();
                } else {
                    return "";
                }

            case CLIPBOARD_OPEN:
                MainActivity.openLink(copy);
                return null;
            default: return null;
        }
    }


/**
 * currentModsを取得する
 * @return currentModsの値
 */
    public static int getCurrentMods() {
        int currMods = 0;
        if (holdingAlt) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_ALT;
        } if (holdingCapslock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK;
        } if (holdingCtrl) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CONTROL;
        } if (holdingNumlock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK;
        } if (holdingShift) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_SHIFT;
        }
        return currMods;
    }

/**
 * modifiersを設定する
 * @param modifiers 設定値
 */
    public static void setModifiers(int keyCode, boolean isDown){
        switch (keyCode){
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT:
                CallbackBridge.holdingShift = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL:
                CallbackBridge.holdingCtrl = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT:
                CallbackBridge.holdingAlt = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK:
                CallbackBridge.holdingCapslock = isDown;
                return;

            case LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK:
                CallbackBridge.holdingNumlock = isDown;
        }
    }

    //Called from JRE side
    @SuppressWarnings("unused")
/**
 * onGrabStateChangedメソッド
 */
    private static void onGrabStateChanged(final boolean grabbing) {
        isGrabbing = grabbing;
        sChoreographer.postFrameCallbackDelayed((time) -> {
            // If the grab re-changed, skip notify process
            if(isGrabbing != grabbing) return;

            System.out.println("Grab changed : " + grabbing);
            synchronized (grabListeners) {
                for (GrabListener g : grabListeners) g.onGrabState(grabbing);
            }

        }, 16);

    }

    // Called from JRE side
    @SuppressWarnings("unused")
/**
 * onCursorTypeChangedメソッド
 */
    private static void onCursorTypeChanged(int cursorType) {
        currentCursorType = cursorType;
    }
/**
 * addGrabListenerメソッド
 */
    public static void addGrabListener(GrabListener listener) {
        synchronized (grabListeners) {
            listener.onGrabState(isGrabbing);
            grabListeners.add(listener);
        }
    }
/**
 * removeGrabListenerメソッド
 */
    public static void removeGrabListener(GrabListener listener) {
        synchronized (grabListeners) {
            grabListeners.remove(listener);
        }
    }

    @Keep @CriticalNative public static native void nativeSetUseInputStackQueue(boolean useInputStackQueue);

    @Keep @CriticalNative private static native boolean nativeSendChar(char codepoint);
    // GLFW: GLFWCharModsCallback deprecated, but is Minecraft still use?
    @Keep @CriticalNative private static native boolean nativeSendCharMods(char codepoint, int mods);
    @Keep @CriticalNative private static native void nativeSendKey(int key, int scancode, int action, int mods);
    // private static native void nativeSendCursorEnter(int entered);
    @Keep @CriticalNative private static native void nativeSendCursorPos(float x, float y);
    @Keep @CriticalNative private static native void nativeSendMouseButton(int button, int action, int mods);
    @Keep @CriticalNative private static native void nativeSendScroll(double xoffset, double yoffset);
    @Keep @CriticalNative private static native void nativeSendScreenSize(int width, int height);
    @Keep public static native void nativeSetWindowAttrib(int attrib, int value);
    @Keep public static native int getCurrentFps();
}
