import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    // Create unified iOS source set hierarchy so 'iosMain' exists
    applyDefaultHierarchyTemplate()
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.ktor.client.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.io.bytestring)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multipaz)
            implementation(libs.multipaz.doctypes)
            implementation(libs.multipaz.models)
            implementation(libs.multipaz.compose)
            implementation(libs.ktor.client.core)
            // CIO for JVM/Android; Darwin engine for iOS in iosMain
            implementation(libs.ktor.client.cio)
            implementation(libs.coil.compose)
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.androidx.sqlite)
                implementation(libs.androidx.sqlite.framework)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.multipaz.samples.wallet.cmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.multipaz.samples.wallet.cmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += listOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

