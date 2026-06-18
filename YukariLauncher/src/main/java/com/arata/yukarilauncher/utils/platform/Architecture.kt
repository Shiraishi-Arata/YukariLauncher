package com.arata.yukarilauncher.utils.platform

import android.os.Build

/**
 * デバイスのCPUアーキテクチャを検出し、アーキテクチャ関連の情報を提供するユーティリティオブジェクト。
 * ARM、ARM64、x86、x86_64の各アーキテクチャを識別し、アドレス空間の制限やビット幅の判定を行います。
 */
object Architecture {
    /** サポートされていないアーキテクチャを示す値。 */
    const val UNSUPPORTED_ARCH = -1
    /** ARM64アーキテクチャを示す値。 */
    const val ARCH_ARM64 = 0x1
    /** ARMアーキテクチャを示す値。 */
    const val ARCH_ARM = 0x2
    /** x86アーキテクチャを示す値。 */
    const val ARCH_X86 = 0x4
    /** x86_64アーキテクチャを示す値。 */
    const val ARCH_X86_64 = 0x8

    /** 32ビットデバイスのアドレス空間制限値。 */
    private const val ADDRESS_SPACE_LIMIT_32_BIT = 0xbfffffffL
    /** 64ビットデバイスのアドレス空間制限値。 */
    private const val ADDRESS_SPACE_LIMIT_64_BIT = 0x7fffffffffL

    /**
     * デバイスのアドレス空間制限を取得します。
     * @return 64ビットデバイスの場合は0x7fffffffff、32ビットデバイスの場合は0xbfffffff
     */
    fun getAddressSpaceLimit(): Long {
        return if (is64BitsDevice()) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT
    }

    /**
     * デバイスが64ビットアーキテクチャをサポートしているかどうかを判定します。
     * @return 64ビットをサポートしている場合はtrue
     */
    fun is64BitsDevice(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }

    /**
     * デバイスが32ビットアーキテクチャのみをサポートしているかどうかを判定します。
     * @return 32ビットのみの場合はtrue
     */
    fun is32BitsDevice(): Boolean {
        return !is64BitsDevice()
    }

    /**
     * デバイスのCPUアーキテクチャを取得します。
     * @return アーキテクチャを示す整数値（ARCH_ARM64、ARCH_ARM、ARCH_X86、ARCH_X86_64のいずれか）
     */
    fun getDeviceArchitecture(): Int {
        return if (isx86Device()) {
            if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        } else {
            if (is64BitsDevice()) ARCH_ARM64 else ARCH_ARM
        }
    }

    /**
     * デバイスがx86系アーキテクチャかどうかを判定します。
     * @return x86系の場合はtrue
     */
    fun isx86Device(): Boolean {
        val ABI = if (is64BitsDevice()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        for (str in ABI) {
            if (archAsInt(str) == comparedArch) return true
        }
        return false
    }

    /**
     * アーキテクチャ名の文字列を対応する整数値に変換します。
     * @param arch アーキテクチャ名（例: "arm64", "x86_64"）
     * @return 対応するアーキテクチャ定数、未知の場合はUNSUPPORTED_ARCH
     */
    fun archAsInt(arch: String): Int {
        val a = arch.lowercase().trim().replace(" ", "")
        return when {
            a.contains("arm64") || a == "aarch64" -> ARCH_ARM64
            a.contains("arm") || a == "aarch32" -> ARCH_ARM
            a.contains("x86_64") || a.contains("amd64") -> ARCH_X86_64
            a.contains("x86") || (a.startsWith("i") && a.endsWith("86")) -> ARCH_X86
            else -> UNSUPPORTED_ARCH
        }
    }

    /**
     * アーキテクチャの整数値を人間が読める文字列に変換します。
     * @param arch アーキテクチャの整数値
     * @return アーキテクチャ名の文字列（例: "arm64", "x86"）
     */
    fun archAsString(arch: Int): String {
        return when (arch) {
            ARCH_ARM64 -> "arm64"
            ARCH_ARM -> "arm"
            ARCH_X86_64 -> "x86_64"
            ARCH_X86 -> "x86"
            else -> "UNSUPPORTED_ARCH"
        }
    }
}