package com.opxl.sleepslide.data.repository

import com.opxl.sleepslide.data.local.UserPrefsDataStore
import com.opxl.sleepslide.domain.repository.TutorialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorialRepositoryImpl @Inject constructor(
    private val dataStore: UserPrefsDataStore,
) : TutorialRepository {

    override fun hasSeenCoachMark(screen: String): Flow<Boolean> =
        dataStore.hasSeenCoachMark(screen)

    override suspend fun markSeen(screen: String) =
        dataStore.markCoachMarkSeen(screen)

    override suspend fun resetAll() =
        dataStore.resetCoachMarks()
}
