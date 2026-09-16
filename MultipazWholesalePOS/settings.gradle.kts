<<<<<<<< HEAD:MultipazCodelab/Reader/settings.gradle.kts
rootProject.name = "MultipazPhotoIDIdentityReader"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
========
rootProject.name = "MultipazWholesalePOS"
>>>>>>>> main:MultipazWholesalePOS/settings.gradle.kts

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("org.multipaz")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

<<<<<<<< HEAD:MultipazCodelab/Reader/settings.gradle.kts
include(":libbackend")
include(":composeApp")
include(":backend")
========
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":shared")

include(":terminalBackend")
>>>>>>>> main:MultipazWholesalePOS/settings.gradle.kts
