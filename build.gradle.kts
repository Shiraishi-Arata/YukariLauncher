val mobileGluesDir = rootProject.projectDir.resolve("MobileGlues")
val libsDir = rootProject.projectDir.resolve("YukariLauncher/libs")

/**
 * MobileGluesネイティブライブラリをビルドし、出力AARをYukariLauncher/libsにコピーするタスク。
 */
tasks.register("buildMG") {
    group = "MobileGlues"
    description = "Build MobileGlues native library and copy AAR to YukariLauncher/libs"

    doLast {
        val gradlew = mobileGluesDir.resolve("gradlew")
        providers.exec {
            workingDir = mobileGluesDir
            commandLine(gradlew.absolutePath, "assembleRelease", "--no-daemon")
        }.result.get()

        val aarSource = mobileGluesDir.resolve("build/outputs/aar/MobileGlues-release.aar")
        val aarTarget = libsDir.resolve("mobileglues-release.aar")

        if (!aarSource.exists()) {
            throw GradleException("MobileGlues AAR not found at $aarSource")
        }

        libsDir.mkdirs()
        aarSource.copyTo(aarTarget, overwrite = true)
        logger.lifecycle("Copied MobileGlues AAR to $aarTarget")
    }
}
