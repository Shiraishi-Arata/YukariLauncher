package com.arata.yukarilauncher.feature.unpack

import com.arata.yukarilauncher.R

/**
 * ランチャーコンポーネントを定義する列挙型。
 * 各コンポーネントの識別子、表示名、説明リソースID、プライベートディレクトリ使用フラグを保持する。
 */
enum class Components(val component: String, val displayName: String, val summary: Int?, val privateDirectory: Boolean) {
    OTHER_LOGIN("other_login", "authlib-injector", R.string.splash_screen_authlib_injector, true),
    CACIOCAVALLO("caciocavallo", "caciocavallo", R.string.splash_screen_cacio, true),
    CACIOCAVALLO17("caciocavallo17", "caciocavallo 17", R.string.splash_screen_cacio, true),
    LWJGL3("lwjgl3", "LWJGL 3.3.6", R.string.splash_screen_lwjgl, true),
    COMPONENTS("components", "Launcher Components", R.string.splash_screen_components, true)
}
