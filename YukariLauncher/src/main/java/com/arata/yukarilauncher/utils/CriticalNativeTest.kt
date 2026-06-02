package com.arata.yukarilauncher.utils

import dalvik.annotation.optimization.CriticalNative

/**
 * CriticalNativeアノテーションを使用したJNI呼び出しのテストを行うオブジェクト。
 * ARTランタイムでのCriticalNative最適化が正しく機能するかを検証します。
 */
object CriticalNativeTest {
    /**
     * CriticalNativeアノテーションが付与されたテスト用ネイティブメソッド。
     * @param arg0 テスト引数1
     * @param arg1 テスト引数2
     */
    @CriticalNative
    @JvmStatic external fun testCriticalNative(arg0: Int, arg1: Int)

    /**
     * CriticalNativeテストを実行します。
     */
    @JvmStatic
    fun invokeTest() {
        testCriticalNative(0, 0)
    }
}