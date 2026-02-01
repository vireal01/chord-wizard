package com.vireal.chordwizard.di

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import me.tatarka.inject.annotations.Inject

/**
 * Main ViewModel demonstrating kotlin-inject usage
 */
@Inject
class MainViewModel(
  private val repository: AppRepository,
) : ViewModel() {
  var showContent by mutableStateOf(false)
    private set

  fun toggleContent() {
    showContent = !showContent
  }

  fun getGreeting(): String = repository.getGreeting()

  fun getAppInfo(): String = repository.getAppInfo()
}