// プロジェクト名（3.3.6 / 3.4.1）をLWJGLバージョンとして使用
val lwjglVersion = project.name

group = "org.lwjgl.glfw"

configurations.getByName("default").isCanBeResolved = true

val jarTask = tasks.named<Jar>("jar")
jarTask.configure {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-glfw-classes")
    destinationDirectory.set(file("../../YukariLauncher/src/main/assets/components/lwjgl/$lwjglVersion/"))
    doLast {
        val assetsDir = file("../../YukariLauncher/src/main/assets/components/lwjgl/$lwjglVersion/")
        val versionFile = file("$assetsDir/version")
        versionFile.writeText(System.currentTimeMillis().toString())

        val aarFile = file("libs/lwjgl-$lwjglVersion.aar")
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
        attributes("Manifest-Version" to lwjglVersion)
        attributes("Automatic-Module-Name" to "org.lwjgl")
    }
}

tasks.register<Zip>("packageZip") {
    dependsOn("jar")
    val assetsDir = file("../../YukariLauncher/src/main/assets/components/lwjgl/$lwjglVersion/")
    from(assetsDir) {
        into(lwjglVersion)
    }
    archiveFileName.set("$lwjglVersion.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}
