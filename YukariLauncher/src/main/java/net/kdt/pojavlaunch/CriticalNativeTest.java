package net.kdt.pojavlaunch;

import dalvik.annotation.optimization.CriticalNative;

/**
 * CriticalNativeアノテーションのテストクラス。
 */
public class CriticalNativeTest {
    /**
     * CriticalNativeを使用したネイティブメソッドのテスト。
     */
    @CriticalNative
    public static native void testCriticalNative(int arg0, int arg1);

    /**
     * CriticalNativeテストメソッドを呼び出します。
     */
    public static void invokeTest() {
        testCriticalNative(0, 0);
    }
}
