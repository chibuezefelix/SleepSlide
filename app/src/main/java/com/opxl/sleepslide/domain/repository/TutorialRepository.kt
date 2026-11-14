package com.opxl.sleepslide.domain.repository

import kotlinx.coroutines.flow.Flow

/** Screen ids used as the DataStore key suffix for first-visit coach marks. */
object CoachMarkScreens {
    const val HOME     = "home"
    const val PLAYER   = "player"
    const val LIBRARY  = "library"
    const val PRESETS  = "presets"
    const val SETTINGS = "settings"

    val ALL = listOf(HOME, PLAYER, LIBRARY, PRESETS, SETTINGS)
}

interface TutorialRepository {

    /** True once the user has dismissed the coach marks for [screen]. */
    fun hasSeenCoachMark(screen: String): Flow<Boolean>

    suspend fun markSeen(screen: String)

    /** Forgets every screen so the coach marks show again on the next visit. */
    suspend fun resetAll()
}
