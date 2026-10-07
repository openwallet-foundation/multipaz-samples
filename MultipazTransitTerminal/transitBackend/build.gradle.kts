plugins {
    id("java-library")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktor)
}

val dockerImage = providers.gradleProperty("dockerImage")
    .orElse("multipaz-transit-terminal")
val dockerImageTag = providers.gradleProperty("imageTag")
    .orElse("dev")

tasks.register<Exec>("buildDockerImageAmd64") {
    group = "distribution"
    description = "Builds the Transit terminal backend fat JAR and loads a linux/amd64 Docker image locally."
    dependsOn("buildFatJar")
    workingDir = projectDir
    commandLine(
        "docker", "build",
        "--platform", "linux/amd64",
        "--tag", "${dockerImage.get()}:${dockerImageTag.get()}",
        ".",
    )
    outputs.upToDateWhen { false }
}

application {
    mainClass.set("org.multipaz.transit.backend.Main")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    ksp(libs.multipaz.cbor.rpc)
    implementation(libs.multipaz)

    implementation(libs.multipaz.server)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.io.bytestring)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.logback.classic)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

ktor {
}
