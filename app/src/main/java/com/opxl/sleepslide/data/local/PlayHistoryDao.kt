package com.opxl.sleepslide.data.local


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Query("SELECT * FROM play_history ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Local.PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<Local.PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE id = :id")
    suspend fun getById(id: Long): Local.PlayHistoryEntity?

    @Query("SELECT * FROM play_history ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatest(): Local.PlayHistoryEntity?

    @Query("SELECT * FROM play_history WHERE presetId = :presetId ORDER BY startedAt DESC")
    fun observeByPreset(presetId: Long): Flow<List<Local.PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): Local.PlayHistoryEntity?

    @Query("""
        SELECT * FROM play_history
        WHERE startedAt >= :fromEpoch AND startedAt <= :toEpoch
        ORDER BY startedAt DESC
    """)
    fun observeInRange(fromEpoch: Long, toEpoch: Long): Flow<List<Local.PlayHistoryEntity>>

    @Query("SELECT SUM(durationPlayedMs) FROM play_history")
    fun observeTotalPlayedMs(): Flow<Long?>
//
    @Query("SELECT SUM(durationPlayedMs) FROM play_history WHERE presetId = :presetId")
    suspend fun getTotalPlayedMsForPreset(presetId: Long): Long?
//
    @Query("SELECT COUNT(*) FROM play_history")
    fun observeSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Local.PlayHistoryEntity): Long
//
    @Update
    suspend fun update(session: Local.PlayHistoryEntity)
//
    /**
     * Adds audible playing time to every open session. Called periodically while
     * playing, so durationPlayedMs is always a running total rather than something
     * derived at close time — the only way a session orphaned by process death can
     * still report an honest figure.
     */
    @Query("UPDATE play_history SET durationPlayedMs = durationPlayedMs + :deltaMs WHERE endedAt IS NULL")
    suspend fun addPlayedTimeToActiveSessions(deltaMs: Long)

    /** Live close — the caller has flushed the running counter; only the end stamp is set. */
    @Query("""
        UPDATE play_history
        SET endedAt = :endedAt,
            stoppedBy = :reason
        WHERE id = :id AND endedAt IS NULL
    """)
    suspend fun closeSession(id: Long, endedAt: Long, reason: String)

    /**
     * Stale close (cold start after a crash / force-kill). The real end time is
     * unknowable, so each row derives it from its own running counter instead of
     * "now" — a session killed at 23:00 and closed at 07:00 must not record 8 h.
     */
    @Query("""
        UPDATE play_history
        SET endedAt = startedAt + durationPlayedMs,
            stoppedBy = :reason
        WHERE endedAt IS NULL
    """)
    suspend fun closeAllActiveSessions(reason: String)
    @Query("DELETE FROM play_history WHERE startedAt < :beforeEpoch")
    suspend fun deleteOlderThan(beforeEpoch: Long): Int
    @Query("DELETE FROM play_history WHERE id = :id")
    suspend fun deleteById(id: Long):Int

    @Query("DELETE FROM play_history")
    suspend fun deleteAll(): Int
}