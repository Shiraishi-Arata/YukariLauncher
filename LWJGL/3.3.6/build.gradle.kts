plugins {
    java
    id("com.diffplug.spotless")
}

// 共通ビルドロジックは common.gradle.kts に集約
apply(from = "../common.gradle.kts")
