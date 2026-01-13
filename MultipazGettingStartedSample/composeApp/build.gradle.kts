import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
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

            implementation(libs.androidx.fragment)

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

            implementation(libs.multipaz)
            implementation(libs.multipaz.compose)
            implementation(libs.multipaz.doctypes)
            implementation(libs.multipaz.vision)
            implementation(libs.multipaz.dcapi)

            implementation(libs.coil.compose)
            implementation(libs.ktor.client.core)
            // CIO for JVM/Android; Darwin engine for iOS in iosMain
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.serialization.json)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite)
            implementation(libs.androidx.sqlite.framework)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.multipaz.getstarted"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.multipaz.getstarted"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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

// Task to copy framework to CocoaPods expected location
tasks.register<Copy>("prepareCocoaPodsFramework") {
    description = "Prepares the framework for CocoaPods by copying it to the expected location"
    group = "cocoapods"

    // Build only for arm64 (physical devices) - Credentials are stored in hardware-backed secure storage
    dependsOn(
        "linkDebugFrameworkIosArm64"
    )
    
    val frameworkDir = layout.buildDirectory.dir("cocoapods/framework")
    val sourceFrameworkDir = layout.buildDirectory.dir("bin/iosArm64/debugFramework")
    
    // Copy the framework directory itself, preserving the directory structure
    from(sourceFrameworkDir) {
        include("ComposeApp.framework/**")
    }
    into(frameworkDir)
    
    doLast {
        println("✓ Framework copied to ${frameworkDir.get().asFile.absolutePath}/ComposeApp.framework")
    }
}

// Make the prepareCocoaPodsFramework task run automatically after framework linking
tasks.named("linkDebugFrameworkIosArm64") {
    finalizedBy("prepareCocoaPodsFramework")
}

// Alias task for compatibility with podspec error message
tasks.register("generateDummyFramework") {
    description = "Generates dummy framework for CocoaPods (alias for prepareCocoaPodsFramework)"
    group = "cocoapods"
    dependsOn("prepareCocoaPodsFramework")
}

