package com.vireal.chordwizard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform