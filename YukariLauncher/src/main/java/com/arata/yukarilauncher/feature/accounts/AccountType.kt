package com.arata.yukarilauncher.feature.accounts

/**
 * アカウントの種類を定義する列挙型
 * 各アカウントがMicrosoftアカウントかローカルアカウントかを区別するために使用する
 */
enum class AccountType(val type: String) {
    MICROSOFT("Microsoft"),
    LOCAL("Local")
}