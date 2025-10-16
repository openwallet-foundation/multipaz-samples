plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktor)
    alias(libs.plugins.buildconfig)
}

project.setProperty("mainClassName", "org.multipaz.identityreader.backend.Main")

val projectVersionCode: Int by rootProject.extra
val projectVersionName: String by rootProject.extra

buildConfig {
    packageName("org.multipaz.identityreader")
    buildConfigField("VERSION", projectVersionName)
    // This server-side clientId works with APKs signed with the signing key in devkey.keystore
    buildConfigField("IDENTITY_READER_BACKEND_CLIENT_ID",
        System.getenv("IDENTITY_READER_BACKEND_CLIENT_ID")
            ?: "332546139643-p9h5vs340rbmb5c6edids3euclfm4i41.apps.googleusercontent.com"
    )
    useKotlinOutput { internalVisibility = false }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors = true
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    ksp(libs.multipaz.cbor.rpc)
    implementation(libs.multipaz)
    implementation(project(":libbackend"))

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.io.bytestring)
    implementation(libs.hsqldb)
    implementation(libs.mysql)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.logging)
    implementation(libs.logback.classic)
    implementation(libs.identity.google.api.client)
}

ktor {
}