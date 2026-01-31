package com.vireal.chordwizard

class Greeting {
  private val platform = getPlatform()

  fun greet(): String = "Hello, ${platform.name}!"
}