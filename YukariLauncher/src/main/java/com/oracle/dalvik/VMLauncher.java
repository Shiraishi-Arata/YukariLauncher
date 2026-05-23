package com.oracle.dalvik;

/**
 * JVMを起動するためのネイティブメソッドを提供するファサードクラス。
 */
public final class VMLauncher {
    /**
     * インスタンス化を防ぐプライベートコンストラクタ。
     */
	private VMLauncher() {
	}

    /**
     * 指定された引数でJVMを起動するネイティブメソッド。
     * @param args JVMに渡す起動引数
     * @return 起動結果コード
     */
	public static native int launchJVM(String[] args);
}
