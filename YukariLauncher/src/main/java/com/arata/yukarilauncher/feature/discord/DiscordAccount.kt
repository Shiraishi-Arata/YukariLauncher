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
    /** 認証トークン */
    val token: String
) : Parcelable, Serializable {

    /** UI表示用の名前。globalNameがなければusernameをフォールバック。 */
    val displayName: String get() = globalName ?: username

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(username)
        parcel.writeString(globalName)
        parcel.writeString(discriminator)
        parcel.writeString(token)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DiscordAccount> {
        override fun createFromParcel(parcel: Parcel): DiscordAccount = DiscordAccount(parcel)
        override fun newArray(size: Int): Array<DiscordAccount?> = arrayOfNulls(size)
    }
}
