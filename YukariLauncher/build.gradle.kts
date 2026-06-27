import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.github.megatronking.stringfog.plugin.StringFogExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.stringfog)
    alias(libs.plugins.spotless)
}

buildscript {
    dependencies {
        classpath(libs.stringfog.xor)
    }
}

val getCFApiKey = {
    System.getenv("CURSEFORGE_API_KEY") ?: run {
        val curseforgeKeyFile = File(rootDir, "curseforge_key.txt")
        if (curseforgeKeyFile.canRead() && curseforgeKeyFile.isFile) {
            curseforgeKeyFile.readText()
        } else {
            logger.warn("BUILD: You have no CurseForge key, the curseforge api will get disabled !")
            "DUMMY"
        }
    }
}

/**
 * ビルドタイプを環境変数から取得します。
 */
val getBuildType = {
    val buildType = System.getenv("YL_BUILD_TYPE") ?: "DEBUG"
    logger.warn("BUILD: Build Type --> $buildType")
    buildType
}

val nameId = "com.arata.yukarilauncher"
val generatedYukariDir = layout.buildDirectory.dir("generated/source/yukari/java").get().asFile
val launcherAPPName = project.findProperty("launcher_app_name") as? String ?: error("The \"launcher_app_name\" property is not set in gradle.properties.")
val launcherName = project.findProperty("launcher_name") as? String ?: error("The \"launcher_name\" property is not set in gradle.properties.")
val launcherVersionCode = (project.findProperty("launcher_version_code") as? String)?.toIntOrNull() ?: error("The \"launcher_version_code\" property is not set as an integer in gradle.properties.")
val launcherVersionName = project.findProperty("launcher_version_name") as? String ?: error("The \"launcher_version_name\" property is not set in gradle.properties.")

configurations {
    create("instrumentedClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    fogPackages = arrayOf(nameId)
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

android {
    namespace = nameId
    compileSdk = 35

    signingConfigs {
        create("releaseBuild") {
            val pwd = System.getenv("ARATA_KEYSTORE_PASSWORD")
            storeFile = file("arata-key.jks")
            storePassword = pwd
            keyAlias = "mtp"
            keyPassword = pwd
        }
        create("customDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = nameId
        minSdk = 26
        targetSdk = 35
        versionCode = launcherVersionCode
        versionName = launcherVersionName
        multiDexEnabled = true
        manifestPlaceholders["launcher_name"] = launcherAPPName
    }

    buildTypes {
        val storageProviderId = "$nameId.storage_provider"

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("customDebug")
            resValue("string", "storageProviderAuthorities", "$storageProviderId.debug")
        }
        create("proguard") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
        }
        create("proguardNoDebug") {
            initWith(getByName("proguard"))
            isDebuggable = false
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            resValue("string", "storageProviderAuthorities", storageProviderId)
            signingConfig = signingConfigs.getByName("releaseBuild")
        }
    }

    sourceSets["main"].java.srcDirs(generatedYukariDir)

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
                    afterEvaluate {
                        val task = tasks.named("merge${variantName}Assets").get() as MergeSourceSetFolders
                        task.doLast {
                            val assetsDir = task.outputDir.get().asFile

                            // Remove JRE and LWJGL from APK assets (downloaded at runtime)
                            val jreList = listOf("jre-8", "jre-17", "jre-21", "jre-25")
                            jreList.forEach { jreVersion ->
                                val runtimeDir = File("$assetsDir/components/$jreVersion")
                                if (runtimeDir.exists()) {
                                    println("delete jre assets:${runtimeDir} : ${runtimeDir.deleteRecursively()}")
                                }
                            }
                            val lwjglVersions = listOf("lwjgl/3.3.6", "lwjgl/3.4.1")
                            lwjglVersions.forEach { lwjglDir ->
                                val lwjglAssetDir = File("$assetsDir/components/$lwjglDir")
                                if (lwjglAssetDir.exists()) {
                                    println("delete lwjgl assets:${lwjglAssetDir} : ${lwjglAssetDir.deleteRecursively()}")
                                }
                            }
                        }
                    }

                    (output.getFilter(ABI)?.identifier ?: "all").let { abi ->
                        val baseName = "$launcherName-${if (variant.buildType == "release") defaultConfig.versionName else "Debug-${defaultConfig.versionName}"}"
                        output.outputFileName = if (abi == "all") "$baseName.apk" else "$baseName-$abi.apk"
                    }
                }
            }
        }
    }

    splits {
        val arch = System.getProperty("arch", "all")
        if (arch != "all") {
            abi {
                isEnable = true
                reset()
                when (arch) {
                    "arm" -> include("armeabi-v7a")
                    "arm64" -> include("arm64-v8a")
                    "x86" -> include("x86")
                    "x86_64" -> include("x86_64")
                }
            }
        }
    }
    ndkVersion = "27.1.12297006"
    
    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }

    buildFeatures {
        prefab = true
        buildConfig = true
        viewBinding = true
        compose = true
    }

    buildToolsVersion = "35.0.0"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

spotless {
    java {
        target("src/**/*.java")
        importOrder("android", "androidx", "com", "io", "java", "javax", "net", "org")
        removeUnusedImports()
    }
    kotlin {
        target("src/**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ij_kotlin_imports_layout" to "^android,^androidx,^com,^io,^java,^javax,^net,^org,*",
                "ktlint_standard" to "disabled"
            )
        )
    }
}

/**
 * 指定された定数マップからJavaクラスを生成します。
 * @param sourceOutputDir 出力ディレクトリ
 * @param packageName パッケージ名
 * @param className クラス名
 * @param constantMap 定数マップ
 */
fun generateJavaClass(sourceOutputDir: File, packageName: String, className: String, constantMap: Map<String, String>) {
    val outputDir = File(sourceOutputDir, packageName.replace(".", "/"))
    outputDir.mkdirs()
    val javaFile = File(outputDir, "$className.java")
    val constants = constantMap.entries.joinToString("\n") { (key, value) ->
        "\tpublic static final String $key = \"$value\";"
    }
    javaFile.writeText(
        """
        |/**
        | * 自動生成ファイル。変更しないでください。
        | */
        |package $packageName;
        |
        |public class $className {
        |$constants
        |}
        """.trimMargin()
    )
    println("Generated Java file: ${javaFile.absolutePath}")
}

/**
 * ランチャー情報を保持するInfoDistributorクラスを生成するタスク
 */
tasks.register("generateInfoDistributor") {
    doLast {
        val constantMap = mapOf(
            "CURSEFORGE_API_KEY" to getCFApiKey(),
            "LAUNCHER_NAME" to project.property("launcher_name").toString(),
            "APP_NAME" to project.property("launcher_app_name").toString(),
            "BUILD_TYPE" to getBuildType()
        )
        generateJavaClass(generatedYukariDir, "com.arata.yukarilauncher", "InfoDistributor", constantMap)
    }
}

tasks.named("preBuild") {
    dependsOn("generateInfoDistributor")
}

dependencies {
    implementation(libs.javax.annotation.api)
    implementation(libs.commons.codec)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.checkerboard.drawable)
    implementation(libs.portrait.sdp)
    implementation(libs.portrait.ssp)
    implementation(libs.extended.view)
    implementation(libs.gamepad.remapper)
    implementation(libs.virtual.joystick.android)
    implementation(libs.powerspinner)
    implementation(libs.glide)
    implementation(libs.dsl.tablayout)

    implementation(libs.stringfog.xor)

    implementation(libs.touchcontroller.proxy)

    implementation(libs.glide.transformations)

    implementation(libs.xz)
    implementation(libs.htmlcleaner)
    implementation(libs.bytehook)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.okhttp)
    implementation(libs.commonmark)
    implementation(libs.material)
    implementation(libs.flexbox)

    implementation(libs.taptargetview)
    implementation(libs.floatingx)
    implementation(libs.eventbus)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.toml4j) {
        exclude(group = "com.google.code.gson", module = "gson")
    }

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
}
