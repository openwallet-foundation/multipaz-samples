plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.0.1")
        android.set(true)
        outputColorName.set("RED")

        // Exclude auto-generated files and build directories
        filter {
            exclude { element -> element.file.path.contains("/generated/") }
            exclude { element -> element.file.path.contains("generated") }
            exclude { element -> element.file.path.contains("/build/") }
            exclude { element -> element.file.path.contains("\\build\\") }
            exclude("**/build/**")
            exclude("**/generated/**")
        }
    }
}
