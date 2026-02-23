package com.vireal.chordwizard.navigation

import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerAdvanceMode
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerChordFamily
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerRoot
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerSessionConfig
import com.vireal.chordwizard.ui.screens.chordtrainer.model.TrainerMode

private const val ROUTE_CSV_SEPARATOR = ","

fun TrainerSessionConfig.toRoute(): Route.ChordTrainerSession =
  Route.ChordTrainerSession(
    familyNamesCsv = selectedFamilies.joinToString(ROUTE_CSV_SEPARATOR) { it.name },
    rootNamesCsv = selectedRoots.joinToString(ROUTE_CSV_SEPARATOR) { it.name },
    modeName = mode.name,
    advanceModeName = advanceMode.name,
    cardCount = cardCount.coerceAtLeast(1),
  )

fun Route.ChordTrainerSession.toTrainerSessionConfig(): TrainerSessionConfig {
  val families = parseEnumSet(familyNamesCsv, TrainerChordFamily.entries)
  val roots = parseEnumSet(rootNamesCsv, TrainerRoot.entries)
  val mode = TrainerMode.entries.firstOrNull { it.name == modeName } ?: TrainerMode.Guided
  val advanceMode = TrainerAdvanceMode.entries.firstOrNull { it.name == advanceModeName } ?: TrainerAdvanceMode.Manual

  return TrainerSessionConfig(
    selectedFamilies = if (families.isNotEmpty()) families else setOf(TrainerChordFamily.MAJOR),
    selectedRoots = if (roots.isNotEmpty()) roots else setOf(TrainerRoot.C),
    mode = mode,
    advanceMode = advanceMode,
    cardCount = cardCount.coerceAtLeast(1),
  )
}

private fun <T : Enum<T>> parseEnumSet(
  csv: String,
  candidates: List<T>,
): Set<T> {
  if (csv.isBlank()) return emptySet()
  val byName = candidates.associateBy { it.name }
  return csv
    .split(ROUTE_CSV_SEPARATOR)
    .mapNotNull(byName::get)
    .toSet()
}
