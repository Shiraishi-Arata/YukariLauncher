/**
 * プロジェクトの設定
 * プラグイン管理、依存関係解決、サブプロジェクトの構成を行います。
 */
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "YukariLauncher"
include(":LWJGL")
include(":LWJGL:3.3.6")
include(":LWJGL:3.4.1")
include(":YukariLauncher")
