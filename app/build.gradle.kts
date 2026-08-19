plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.huidou.util"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.huidou.util"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 260027
        versionName = "26.1.27"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 指定要包含的 ABI 架构（所有架构）
        ndk {
            // 包含所有支持的架构：armeabi-v7a, arm64-v8a, x86, x86_64
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    val useSecKey = rootProject.hasProperty("SecKeyFile") &&
        rootProject.hasProperty("SecKeyPasswd") &&
        rootProject.hasProperty("SecAlias") &&
        rootProject.hasProperty("SecPassword")

    signingConfigs {
        if (useSecKey) {
            create("sec_sign_key") {
                storeFile = file(rootProject.property("SecKeyFile") as String)
                storePassword = rootProject.property("SecKeyPasswd") as String
                keyAlias = rootProject.property("SecAlias") as String
                keyPassword = rootProject.property("SecPassword") as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = if (useSecKey) {
                signingConfigs.getByName("sec_sign_key")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = false
            }
            signingConfig = if (useSecKey) {
                signingConfigs.getByName("sec_sign_key")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //noinspection UseTomlInstead
    debugImplementation("com.github.L-JINBIN:MTDataFilesProvider:v1.0.0")
}