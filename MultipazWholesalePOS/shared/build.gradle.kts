import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "org.multipaz.pos.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val commonMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.compose.ui)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.multipaz)
                implementation(libs.multipaz.compose)
                implementation(libs.multipaz.doctypes)
                implementation(libs.multipaz.utopia)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.multipaz.cbor.rpc)
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    if (name != "kspCommonMainKotlinMetadata")
        dependsOn("kspCommonMainKotlinMetadata")
}

tasks.matching { it.name != "kspCommonMainKotlinMetadata" && it.name.startsWith("ksp") }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

listOf(
    "compileKotlinIosArm64",
    "compileKotlinIosSimulatorArm64",
    "compileKotlinIosX64",
    "compileKotlinJvm"
)
    .forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn("kspCommonMainKotlinMetadata")
    }