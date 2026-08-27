package com.opxl.sleepslide.domain.repository

import kotlinx.coroutines.flow.Flow

interface VolumeMemoryRepository {

    fun observeVolume(soundId: String): Flow<Float>

    suspend fun getVolume(soundId: String): Float

    suspend fun getVolumesForSounds(soundIds: List<String>): Map<String, Float>

    suspend fun saveVolume(soundId: String, volume: Float)

    suspend fun saveVolumes(volumes: Map<String, Float>)

    suspend fun clearVolume(soundId: String):Int

    suspend fun clearAll():Int
}
