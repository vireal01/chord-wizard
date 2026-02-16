package com.vireal.chordwizard.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.vireal.chordwizard.Greeting
import dev.zacsweers.metro.Provides

/**
 * Provides basic application dependencies
 */
interface AppProvides {
  @Provides
  fun provideGreeting(): Greeting = Greeting()

  @Provides
  fun provideStoreFactory(): StoreFactory = LoggingStoreFactory(DefaultStoreFactory())
}