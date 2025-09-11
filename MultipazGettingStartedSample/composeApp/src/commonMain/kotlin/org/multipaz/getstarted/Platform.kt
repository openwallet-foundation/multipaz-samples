package org.multipaz.getstarted

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform