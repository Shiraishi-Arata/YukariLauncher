package com.arata.yukarilauncher.feature.discord

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * Discordアカウント情報を保持するデータクラス。
 * Parcelableを実装しており、Intent経由での受け渡しが可能です。
 */
data class DiscordAccount(
    /** DiscordのユーザーID */
    val id: String,
    /** Discordのユーザー名（ログイン名） */
    val username: String,
    /** 表示名（global_name）。nullの場合はusernameを表示に使用 */
    val globalName: String? = null,
    /** ディスクリミネーター（#0000形式） */
    val discriminator: String,
    /** アバターハッシュ。CDN URL: https://cdn.discordapp.com/avatars/{id}/{avatar}.png */
    val avatar: String? = null,
    /** バナーハッシュ。CDN URL: https://cdn.discordapp.com/banners/{id}/{banner}.png */
    val banner: String? = null,
    /** 認証トークン */
    val token: String
) : Parcelable, Serializable {

    /** UI表示用の名前。globalNameがなければusernameをフォールバック。 */
    val displayName: String get() = globalName ?: username

    /** Discord CDNのアバターURL。avatarがnullの場合はnullを返す */
    val avatarUrl: String?
        get() = if (avatar != null) {
            val ext = if (avatar!!.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/avatars/$id/$avatar.$ext?size=128"
        } else null

    /** Discord CDNのバナーURL。bannerがnullの場合はnullを返す */
    val bannerUrl: String?
        get() = if (banner != null) {
            val ext = if (banner!!.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/banners/$id/$banner.$ext?size=480"
        } else null

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(username)
        parcel.writeString(globalName)
        parcel.writeString(discriminator)
        parcel.writeString(avatar)
        parcel.writeString(banner)
        parcel.writeString(token)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DiscordAccount> {
        override fun createFromParcel(parcel: Parcel): DiscordAccount = DiscordAccount(parcel)
        override fun newArray(size: Int): Array<DiscordAccount?> = arrayOfNulls(size)
    }
}
