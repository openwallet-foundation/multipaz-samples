plugins {
    id("java-library")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktor)
}

val dockerImage = providers.gradleProperty("dockerImage")
    .orElse("multipaz-pos-terminal")
val dockerImageTag = providers.gradleProperty("imageTag")
    .orElse("dev")

tasks.register<Exec>("buildDockerImageAmd64") {
    group = "distribution"
    description = "Builds the POS terminal backend fat JAR and loads a linux/amd64 Docker image locally."
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
    mainClass.set("org.multipaz.pos.terminal.Main")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
//    implementation(project(":shared"))

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
