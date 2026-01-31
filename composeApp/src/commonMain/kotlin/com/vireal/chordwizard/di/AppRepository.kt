package com.vireal.chordwizard.di

import com.vireal.chordwizard.Greeting
import me.tatarka.inject.annotations.Inject

/**
 * Example repository demonstrating dependency injection
 */
@Inject
class AppRepository(
    private val greeting: Greeting
) {
    fun getGreeting(): String {
        return greeting.greet()
    }

    fun getAppInfo(): String {
        return "ChordWizard App with kotlin-inject DI"
    }
}
