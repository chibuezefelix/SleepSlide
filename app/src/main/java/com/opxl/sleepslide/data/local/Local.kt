package com.opxl.sleepslide.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object Local {

    @Entity(tableName = "sounds")
    data class SoundEntity(
        @PrimaryKey
        val id: String,
        val title: String,
        val category: String,               // SoundCategory.name()
        val assetPath: String,
        val isPremium: Boolean,
        val isBundled: Boolean,
        val downloadedPath: String?,
        val durationMs: Long?,
        val tags: String,                   // JSON array — e.g. ["tinnitus","low"]
        val frequencyHz: Int?,
        // persistence extras
        val isDownloading: Boolean = false,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val playCount: Int = 0,
        val lastPlayedAt: Long? = null,
    )

    @Entity(tableName = "presets")
    data class PresetEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L,
        val name: String,
        val sortOrder: Int = 0,
        val isPinned: Boolean = false,
        val createdAt: Long,
        val lastUsedAt: Long?,
        val emoji: String?,
        // mix-level fields denormalised here
        val masterVolume: Float = 1.0f,
        val fadeInDurationMs: Long = 3_000L,
    )


    @Entity(
        tableName = "preset_layers",
        primaryKeys = ["presetId", "position"],
        foreignKeys = [
            ForeignKey(
                entity = PresetEntity::class,
                parentColumns = ["id"],
                childColumns = ["presetId"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = SoundEntity::class,
                parentColumns = ["id"],
                childColumns = ["soundId"],
                onDelete = ForeignKey.RESTRICT,     // don't silently break a preset
            ),
        ],
        indices = [
            Index("presetId"),
            Index("soundId"),
        ],
    )
    data class PresetLayerEntity(
        val presetId: Long,
        val soundId: String,
        val position: Int,                  // 0..2
        val volume: Float = 1.0f,
        val isMuted: Boolean = false,
    )

    data class PresetWithLayers(
        @Embedded val preset: PresetEntity,
        @Relation(
            parentColumn = "id",
            entityColumn = "presetId",
        )
        val layers: List<PresetLayerEntity>,
    )

    @Entity(
        tableName = "play_history",
        foreignKeys = [
            ForeignKey(
                entity = PresetEntity::class,
                parentColumns = ["id"],
                childColumns = ["presetId"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
        indices = [Index("presetId"), Index("startedAt")],
    )
    data class PlayHistoryEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L,
        val presetId: Long?,                // null = ephemeral mix
        val mixSnapshot: String,            // JSON of SoundMix at time of play
        val startedAt: Long,
        val endedAt: Long?,                 // null if session still active
        val durationPlayedMs: Long = 0L,
        val stoppedBy: StopReason = StopReason.USER,
    )

    enum class StopReason {
        USER,           // manual pause/stop
        TIMER,          // sleep timer expired
        AUDIO_FOCUS,    // lost focus to another app
        BLUETOOTH,      // headset disconnected
        ERROR,
    }

    @Entity(
        tableName = "sound_volume_memory",
        primaryKeys = ["soundId"],
        foreignKeys = [
            ForeignKey(
                entity = SoundEntity::class,
                parentColumns = ["id"],
                childColumns = ["soundId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
        indices = [Index("soundId")],
    )
    data class SoundVolumeMemoryEntity(
        val soundId: String,
        val volume: Float,                  // last set value 0.0 – 1.0
        val updatedAt: Long = System.currentTimeMillis(),
    )
}