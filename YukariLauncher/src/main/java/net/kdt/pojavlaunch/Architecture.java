package net.kdt.pojavlaunch;

import android.os.Build;

/**
 * デバイスアーキテクチャを扱うためのシンプルで簡単な方法を提供することを目的としたクラス。
 */
public class Architecture {
	public static final int UNSUPPORTED_ARCH = -1;
	public static final int ARCH_ARM64 = 0x1;
	public static final int ARCH_ARM = 0x2;
	public static final int ARCH_X86 = 0x4;
	public static final int ARCH_X86_64 = 0x8;

	/* 32ビットARMおよびx86では、上位1GBはカーネル使用のために予約されています。 */
	public static final long ADDRESS_SPACE_LIMIT_32_BIT = 0xbfffffffL;
	/*
	 * 技術的には、x86_64では48ビットになるはずですが、Pojavで524288テラバイトのRAMを
	 * 割り当てる人はいないでしょう。
	 */
	public static final long ADDRESS_SPACE_LIMIT_64_BIT = 0x7fffffffffL;

	/**
	 * プロセスのアドレス空間内でアクセス可能な最大バイトを取得します。
	 * @return プロセスのアドレス空間内でアクセス可能な最大バイト。
	 */
	public static long getAddressSpaceLimit() {
		return is64BitsDevice() ? ADDRESS_SPACE_LIMIT_64_BIT : ADDRESS_SPACE_LIMIT_32_BIT;
	}

	/**
	 * デバイスが64ビットアーキテクチャをサポートしているかどうかを返します。
	 * @return デバイスが64ビットアーキテクチャをサポートしている場合true
	 */
	public static boolean is64BitsDevice(){
		return Build.SUPPORTED_64_BIT_ABIS.length != 0;
	}

	/**
	 * デバイスが32ビットアーキテクチャをサポートしているかどうかを返します。
	 * 64ビットデバイスは32ビットをサポートしているとは報告されません。
	 * @return デバイスが32ビットアーキテクチャをサポートしている場合true
	 */
	public static boolean is32BitsDevice(){
		return !is64BitsDevice();
	}

	/**
	 * デバイスがサポートするアーキテクチャを返します。
	 * mips(/64)はとっくに廃止されているため、ここではチェックされません。
	 *
	 * @return ARCH_ARM || ARCH_ARM64 || ARCH_X86 || ARCH_86_64
	 */
	public static int getDeviceArchitecture(){
		if(isx86Device()){
			return is64BitsDevice() ? ARCH_X86_64 : ARCH_X86;
		}
		return is64BitsDevice() ? ARCH_ARM64 : ARCH_ARM;
	}

	/**
	 * デバイスがx86プロセッサベースかどうかを返します。
	 * デバイスが64ビットか32ビットかは示しません。
	 * @return デバイスがx86ベースの場合はtrue
	 */
	public static boolean isx86Device(){
		//サポートされているABIの全範囲をチェックします。
		//ASUS Zenfoneはネイティブ命令セットの前にarmを配置できるためです。
		String[] ABI = is64BitsDevice() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
		int comparedArch = is64BitsDevice() ? ARCH_X86_64 : ARCH_X86;
		for (String str : ABI) {
			if (archAsInt(str) == comparedArch) return true;
		}
		return false;
	}



	/**
	 * アーキテクチャを文字列からintに変換します。
	 * @param arch 文字列としてのアーキテクチャ
	 * @return intとしてのアーキテクチャ。不明な場合はUNSUPPORTED_ARCH。
	 */
	public static int archAsInt(String arch){
		arch = arch.toLowerCase().trim().replace(" ", "");
		if(arch.contains("arm64") || arch.equals("aarch64")) return ARCH_ARM64;
		if(arch.contains("arm") || arch.equals("aarch32")) return ARCH_ARM;
		if(arch.contains("x86_64") || arch.contains("amd64")) return ARCH_X86_64;
		if(arch.contains("x86") || (arch.startsWith("i") && arch.endsWith("86"))) return ARCH_X86;
		//発生するべきではない
		return UNSUPPORTED_ARCH;
	}

	/**
	 * アーキテクチャを文字列に変換します。
	 * @param arch intとしてのアーキテクチャ。
	 * @return "arm64" || "arm" || "x86_64" || "x86" || "UNSUPPORTED_ARCH"
	 */
	public static String archAsString(int arch){
		if(arch == ARCH_ARM64) return "arm64";
		if(arch == ARCH_ARM) return "arm";
		if(arch == ARCH_X86_64) return "x86_64";
		if(arch == ARCH_X86) return "x86";
		return "UNSUPPORTED_ARCH";
	}

}
