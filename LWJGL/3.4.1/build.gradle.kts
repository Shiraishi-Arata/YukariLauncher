plugins {
    java
    id("com.diffplug.spotless") version "6.25.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

spotless {
    java {
        target("src/**/*.java")
        importOrder("android", "androidx", "com", "io", "java", "javax", "net", "org")
        removeUnusedImports()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

apply(from = "../common.gradle.kts")