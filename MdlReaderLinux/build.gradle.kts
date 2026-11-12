import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

val javaVersion = JavaVersion.VERSION_21

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(compose.desktop.currentOs)
                implementation(libs.compose.material.icons.extended)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.bytestring)

                implementation(libs.multipaz)
                implementation(libs.multipaz.bthci)
                implementation(libs.multipaz.doctypes)
                implementation(libs.multipaz.compose)

                implementation(libs.jna)
                implementation(libs.bouncy.castle.bcprov)
                implementation(libs.bouncy.castle.bcpkix)
                implementation(libs.ktor.client.java)
                implementation(libs.sqlite.jdbc)
            }
        }
    }
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

compose.desktop {
    application {
        mainClass = "org.multipaz.samples.mdlreaderlinux.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

// Writes the full java command line for running the JVM app to build/jvmRunCommand.txt
// so launch-with-hci can build as a normal user and only invoke java under capsh.
tasks.register("prepareJvmRun") {
    dependsOn("jvmJar")
    doLast {
        val jvmCompilation = kotlin.jvm().compilations.getByName("main")
        val classpath = files(
            tasks.named("jvmJar"),
            jvmCompilation.runtimeDependencyFiles
        ).asPath
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val outFile = layout.buildDirectory.file("jvmRunCommand.txt").get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            listOf(
                javaBin,
                "--enable-native-access=ALL-UNNAMED",
                "-cp",
                classpath,
                "org.multipaz.samples.mdlreaderlinux.MainKt"
            ).joinToString(" ") { arg ->
                "'" + arg.replace("'", "'\\''") + "'"
            } + "\n"
        )
    }
}
