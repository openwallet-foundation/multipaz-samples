package org.multipaz.samples.securearea

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
