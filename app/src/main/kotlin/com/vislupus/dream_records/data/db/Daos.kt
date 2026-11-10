package com.vislupus.dream_records.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Transaction
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingWithTags>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeById(id: String): Flow<RecordingWithTags?>

    @Transaction
    @Query("SELECT * FROM recordings WHERE nightDate = :nightDate ORDER BY createdAt ASC")
    fun observeByNight(nightDate: String): Flow<List<RecordingWithTags>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): Recording?

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAll(): List<Recording>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<Recording?>

    @Query("SELECT DISTINCT nightDate FROM recordings")
    fun observeNightDates(): Flow<List<String>>

    @Upsert
    suspend fun upsert(recording: Recording)

    @Update
    suspend fun update(recording: Recording)

    @Query("UPDATE recordings SET transcript = :text, transcriptSource = :source, updatedAt = :now WHERE id = :id")
    suspend fun updateTranscript(id: String, text: String, source: String, now: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY protectedKey IS NULL, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Tag>>

    @Query(
        """SELECT t.id, t.name, t.colorIndex, t.protectedKey, t.createdAt,
                  (SELECT COUNT(*) FROM recording_tags rt WHERE rt.tagId = t.id) AS count
           FROM tags t ORDER BY t.protectedKey IS NULL, t.name COLLATE NOCASE"""
    )
    fun observeAllWithCount(): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): Tag?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Query("SELECT * FROM tags WHERE protectedKey = :key LIMIT 1")
    suspend fun getByProtectedKey(key: String): Tag?

    @Upsert
    suspend fun upsert(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id AND protectedKey IS NULL")
    suspend fun deleteIfNotProtected(id: String)

    // --- recording <-> tag links
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: RecordingTag)

    @Delete
    suspend fun unlink(link: RecordingTag)

    @Query("DELETE FROM recording_tags WHERE recordingId = :recordingId")
    suspend fun unlinkAll(recordingId: String)

    @Query("SELECT * FROM recording_tags")
    suspend fun getAllLinks(): List<RecordingTag>

    @Query("SELECT recordingId FROM recording_tags WHERE tagId = :tagId")
    suspend fun recordingIdsForTag(tagId: String): List<String>

    /** Merge: every recording tagged with [from] gets [into], then [from] is deleted. */
    @Transaction
    suspend fun merge(from: String, into: String) {
        recordingIdsForTag(from).forEach { link(RecordingTag(it, into)) }
        deleteIfNotProtected(from)
    }

    @Query("SELECT DISTINCT r.nightDate FROM recordings r JOIN recording_tags rt ON rt.recordingId = r.id WHERE rt.tagId = :tagId")
    suspend fun nightDatesForTag(tagId: String): List<String>

    /** For the calendar: (nightDate, tagId) pairs of protected tags only. */
    @Query(
        """SELECT DISTINCT r.nightDate AS nightDate, t.id AS tagId, t.colorIndex AS colorIndex
           FROM recordings r JOIN recording_tags rt ON rt.recordingId = r.id JOIN tags t ON t.id = rt.tagId
           WHERE t.protectedKey IS NOT NULL"""
    )
    fun observeCalendarDots(): Flow<List<CalendarDot>>
}

data class CalendarDot(val nightDate: String, val tagId: String, val colorIndex: Int)

@Dao
interface NightDao {
    @Query("SELECT * FROM nights WHERE nightDate = :nightDate")
    fun observe(nightDate: String): Flow<Night?>

    @Query("SELECT * FROM nights")
    suspend fun getAll(): List<Night>

    @Query("SELECT * FROM nights WHERE combinedText != ''")
    fun observeAllWithText(): Flow<List<Night>>

    @Upsert
    suspend fun upsert(night: Night)
}
