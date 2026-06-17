package com.arata.yukarilauncher.feature.discord.gateway

/** Discordのアクティビティ（Rich Presence）情報。 */
data class Activity(
    /** アクティビティ名 */
    val name: String,
    /** アクティビティタイプ（0=Game, 1=Streaming, 2=Listening, 3=Watching, 5=Competing） */
    val type: Int = 0,
    /** ストリーミングURL（type=1の場合） */
    val url: String? = null,
    /** ステート（2行目） */
    val state: String? = null,
    /** 詳細（1行目） */
    val details: String? = null,
    /** タイムスタンプ */
    val timestamps: Timestamps? = null,
    /** 画像アセット */
    val assets: Assets? = null,
    /** パーティー情報 */
    val party: Party? = null,
    /** メタデータ */
    val metadata: Metadata? = null,
    /** Application ID（アセット使用時に設定、nullの場合はKizzyのアイコンが表示される） */
    val applicationId: String? = null
)

/** Rich Presenceの画像アセット。large_image/small_imageはexternal_asset_path（"mp:..."）を指定します。 */
data class Assets(
    val large_image: String? = null,
    val large_text: String? = null,
    val small_image: String? = null,
    val small_text: String? = null
)

/** パーティー情報。 */
data class Party(
    val id: String? = null,
    val size: List<Int>? = null
)

/** 経過時間/残り時間。 */
data class Timestamps(
    val start: Long? = null,
    val end: Long? = null
)

/** アクティビティメタデータ（ボタン等）。 */
data class Metadata(
    val button_url: String? = null,
    val button_label: String? = null
)
