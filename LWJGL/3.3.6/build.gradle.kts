plugins {
    java
    id("com.diffplug.spotless")
}

group = "org.lwjgl.glfw"

configurations.getByName("default").isCanBeResolved = true

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-glfw-classes")
    destinationDirectory.set(file("../../YukariLauncher/src/main/assets/components/lwjgl/3.3.6/"))
    // Auto update the version with a timestamp so the project jar gets updated by Pojav
    doLast {
        val assetsDir = file("../../YukariLauncher/src/main/assets/components/lwjgl/3.3.6/")
        val versionFile = file("$assetsDir/version")
        versionFile.writeText(System.currentTimeMillis().toString())

        // Extract native .so files from AAR
        val aarFile = file("libs/lwjgl-3.3.6.aar")
        val nativeOutputDir = file("$assetsDir/native/")
        if (aarFile.exists()) {
            val tempDir = file("$assetsDir/.extract_temp")
            tempDir.mkdirs()
            copy {
                from(zipTree(aarFile)) {
                    include("jni/**/*.so")
                }
                into(tempDir)
            }
            val jniDir = file("$tempDir/jni")
            if (jniDir.exists()) {
                jniDir.listFiles()?.forEach { abiDir ->
                    if (abiDir.isDirectory) {
                        val targetDir = file("$nativeOutputDir/${abiDir.name}")
                        targetDir.mkdirs()
                        abiDir.copyRecursively(targetDir, overwrite = true)
                    }
                }
            }
            tempDir.deleteRecursively()
        }
    }
    from({
        configurations.getByName("default").map {
            println(it.name)
            if (it.isDirectory) it else zipTree(it)
        }
    })
    exclude("net/java/openjdk/cacio/ctc/**")
    manifest {
        attributes("Manifest-Version" to "3.3.6")
        attributes("Automatic-Module-Name" to "org.lwjgl")
    }
}

spotless {
    java {
        target("src/**/*.java")
        importOrder("android", "androidx", "com", "io", "java", "javax", "net", "org")
        removeUnusedImports()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
