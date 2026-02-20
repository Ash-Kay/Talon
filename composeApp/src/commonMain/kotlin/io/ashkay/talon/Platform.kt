package io.ashkay.talon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform