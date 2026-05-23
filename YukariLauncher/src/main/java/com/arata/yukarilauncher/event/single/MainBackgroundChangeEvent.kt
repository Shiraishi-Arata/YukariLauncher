package com.arata.yukarilauncher.event.single

/**
 * メインActivityの背景画像が変更されたときに通知するイベント
 * 背景画像変更時はユーザーの画面が必ずメインActivityであるため、
 * メインActivityのみが通知を行う必要がある
 * @see net.kdt.pojavlaunch.LauncherActivity
 * @see com.arata.yukarilauncher.ui.fragment.CustomBackgroundFragment
 */
class MainBackgroundChangeEvent
