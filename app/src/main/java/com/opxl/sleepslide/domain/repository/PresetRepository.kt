package com.opxl.sleepslide.domain.repository

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow

interface PresetRepository {

    fun observeAll(): Flow<List<Domain.Preset>>

    fun observePinned(): Flow<List<Domain.Preset>>

    fun observeRecentlyUsed(limit: Int = 5): Flow<List<Domain.Preset>>

    fun observeById(id: Long): Flow<Domain.Preset?>

    fun observeCount(): Flow<Int>

    suspend fun getById(id: Long): Domain.Preset?

    suspend fun getLastUsed(): Domain.Preset?

    suspend fun getCount(): Int

    suspend fun save(preset: Domain.Preset): Long

    suspend fun update(preset: Domain.Preset)

    suspend fun updateMix(presetId: Long, mix: Domain.SoundMix)

    suspend fun rename(presetId: Long, name: String)

    suspend fun setEmoji(presetId: Long, emoji: String?)

    suspend fun pin(presetId: Long)

    suspend fun unpin(presetId: Long)

    suspend fun recordUsed(presetId: Long)

    suspend fun reorder(orderedIds: List<Long>)

    suspend fun delete(presetId: Long)

    suspend fun deleteAll()
}