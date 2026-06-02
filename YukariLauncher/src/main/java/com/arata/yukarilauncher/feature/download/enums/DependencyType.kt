package com.arata.yukarilauncher.feature.download.enums

/**
 * Modの依存タイプを定義する列挙型。
 * 各タイプに識別用の色を割り当て、視覚的な区別を容易にする。
 * @param curseforge CurseForge上のタイプ識別子
 * @param modrinth Modrinth上のタイプ識別子
 * @param color 当該タイプの識別色
 */
enum class DependencyType(val curseforge: String?, val modrinth: String?, val color: Int) {
    /**
     * 必須：この依存はプロジェクトに必須であり、欠けると正常に動作しない。
     *
     * CurseForge: "3"
     * Modrinth: "required"
     * 色：0x4CFF9800（オレンジ、Alpha 30%）
     */
    REQUIRED("3", "required", 0x4CFF9800),

    /**
     * 任意：必須ではないが、追加機能や特徴を提供する。
     *
     * CurseForge: "2"
     * Modrinth: "optional"
     * 色：0x4C34C759（薄緑、Alpha 30%）
     */
    OPTIONAL("2", "optional", 0x4C34C759),

    /**
     * 互換性なし：この依存は他のプロジェクトと競合する。
     * 同時に使用するとエラーや障害が発生する可能性がある。
     *
     * CurseForge: "5"
     * Modrinth: "incompatible"
     * 色：0x4CEF5350（薄赤、Alpha 30%）
     */
    INCOMPATIBLE("5", "incompatible", 0x4CEF5350),

    /**
     * 内蔵：これらの依存はプロジェクトに既に含まれており、ユーザーが個別にインストールする必要はない。
     *
     * CurseForge: "1"
     * Modrinth: "embedded"
     * 色：0x4CFFD54F（薄黄、Alpha 30%）
     */
    EMBEDDED("1", "embedded", 0x4CFFD54F),

    /**
     * ツール：プロジェクトの開発や操作に使用されるツールであり、実行には必須ではない。
     *
     * CurseForge: "4"
     * Modrinth: null
     * 色：0x4CBDBDBD（灰色、Alpha 30%）
     */
    TOOL("4", null, 0x4CBDBDBD),

    /**
     * 包含：プロジェクトに含まれるファイルやリソース。
     * コア機能ではないが、追加のサポートを提供する。
     *
     * CurseForge: "6"
     * Modrinth: null
     * 色：0x4C9575CD（紫、Alpha 30%）
     */
    INCLUDE("6", null, 0x4C9575CD)
}