/**
 * ルートプロジェクトのビルド設定
 * プラグインと依存関係のリポジトリを定義します。
 */
plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.github.megatronking.stringfog:gradle-plugin:5.2.0")
        classpath("com.github.megatronking.stringfog:xor:5.0.0")
        classpath("com.android.tools.build:gradle:8.12.3")
    }
}
