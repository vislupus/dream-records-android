package com.vislupus.dream_records.data

import android.content.Context
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.DefaultTags
import com.vislupus.dream_records.data.db.AppDatabase
import com.vislupus.dream_records.data.db.Night
import com.vislupus.dream_records.data.db.Recording
import com.vislupus.dream_records.data.db.RecordingTag
import com.vislupus.dream_records.data.db.RecordingWithTags
import com.vislupus.dream_records.data.db.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

/**
 * Single entry point for all data operations. Everything is stored inside the
 * app sandbox: the SQLite database and the audio files under files/audio/.
 */
class Repository(private val context: Context, private val db: AppDatabase) {

    private val recordings = db.recordingDao()
    private val tags = db.tagDao()
    private val nights = db.nightDao()

    // ------------------------------------------------------------- Files
    val audioDir: File get() = File(context.filesDir, AppConfig.Recording.AUDIO_DIR).apply { mkdirs() }
    fun audioFile(recording: Recording): File? =
        if (recording.fileName.isBlank()) null else File(audioDir, recording.fileName).takeIf { it.exists() }

    fun newAudioFile(createdAt: Long): File {
        val stamp = TimeUtils.toLocal(createdAt).toString().replace(":", "-").replace(".", "-")
        return File(audioDir, "dream_$stamp.${AppConfig.Recording.FILE_EXTENSION}")
    }

    // -------------------------------------------------------- Recordings
    fun observeRecordings(): Flow<List<RecordingWithTags>> = recordings.observeAll()
    fun observeRecording(id: String): Flow<RecordingWithTags?> = recordings.observeById(id)
    fun observeNight(nightDate: String): Flow<List<RecordingWithTags>> = recordings.observeByNight(nightDate)
    fun observeLatest(): Flow<Recording?> = recordings.observeLatest()
    fun observeNightDates(): Flow<List<String>> = recordings.observeNightDates()
    fun observeCalendarDots() = tags.observeCalendarDots()
    suspend fun getRecording(id: String) = recordings.getById(id)

    suspend fun createRecording(file: File, createdAt: Long, durationMs: Long, autoStopped: Boolean): Recording {
        val rec = Recording(
            id = UUID.randomUUID().toString(),
            createdAt = createdAt,
            durationMs = durationMs,
            fileName = file.name,
            nightDate = TimeUtils.nightDateFor(createdAt),
            autoStopped = autoStopped,
        )
        recordings.upsert(rec)
        return rec
    }

    suspend fun updateTranscript(id: String, text: String, source: String) =
        recordings.updateTranscript(id, text, source, TimeUtils.now())

    suspend fun deleteRecording(id: String) {
        recordings.getById(id)?.let { r -> audioFile(r)?.delete() }
        recordings.delete(id)
    }

    // -------------------------------------------------------------- Tags
    fun observeTags(): Flow<List<Tag>> = tags.observeAll()
    fun observeTagsWithCount() = tags.observeAllWithCount()
    suspend fun getTags() = tags.getAll()

    /** Creates a tag (or returns the existing one with the same name). */
    suspend fun createTag(name: String, colorIndex: Int): Tag {
        val clean = name.trim().take(AppConfig.Tags.MAX_NAME_LENGTH)
        tags.getByName(clean)?.let { return it }
        val tag = Tag(UUID.randomUUID().toString(), clean, colorIndex, null, TimeUtils.now())
        tags.upsert(tag)
        return tag
    }

    suspend fun tagNameExists(name: String, exceptId: String?): Boolean =
        tags.getByName(name.trim())?.let { it.id != exceptId } ?: false

    suspend fun updateTag(tag: Tag) = tags.upsert(tag.copy(name = tag.name.trim().take(AppConfig.Tags.MAX_NAME_LENGTH)))
    suspend fun deleteTag(id: String) = tags.deleteIfNotProtected(id)
    suspend fun mergeTags(fromId: String, intoId: String) = tags.merge(fromId, intoId)
    suspend fun addTagToRecording(recordingId: String, tagId: String) = tags.link(RecordingTag(recordingId, tagId))
    suspend fun removeTagFromRecording(recordingId: String, tagId: String) = tags.unlink(RecordingTag(recordingId, tagId))

    /** Makes sure the protected tags from AppConfig exist. Called once at app start. */
    suspend fun ensureDefaultTags(languageCode: String) {
        val now = TimeUtils.now()
        DefaultTags.ALL.forEach { d ->
            if (tags.getByProtectedKey(d.key) == null) {
                val name = if (languageCode == "bg") d.nameBg else d.nameEn
                // Avoid a name clash with a user tag of the same name.
                val finalName = if (tags.getByName(name) == null) name else "$name ·"
                tags.upsert(Tag(UUID.randomUUID().toString(), finalName, d.colorIndex, d.key, now))
            }
        }
    }

    // ------------------------------------------------------------ Nights
    fun observeNightEntry(nightDate: String): Flow<Night?> = nights.observe(nightDate)
    fun observeNightsWithText(): Flow<List<Night>> = nights.observeAllWithText()

    suspend fun saveNightText(nightDate: String, text: String) =
        nights.upsert(Night(nightDate, text, TimeUtils.now()))

    /** Builds the combined text of a night from its individual transcripts. */
    suspend fun combineNight(nightDate: String): String {
        val parts = recordings.observeByNight(nightDate).first()
        val text = parts.joinToString(AppConfig.Nights.COMBINED_TEXT_SEPARATOR) { r ->
            val body = r.recording.transcript.trim()
            if (AppConfig.Nights.COMBINED_TEXT_WITH_TIMESTAMPS) "[${TimeUtils.formatTime(r.recording.createdAt)}]\n$body" else body
        }
        saveNightText(nightDate, text)
        return text
    }

    // ---------------------------------------------------- Raw access (import/export)
    suspend fun allRecordings() = recordings.getAll()
    suspend fun allLinks() = tags.getAllLinks()
    suspend fun allNights() = nights.getAll()
    suspend fun upsertRecording(r: Recording) = recordings.upsert(r)
    suspend fun upsertTag(t: Tag) = tags.upsert(t)
    suspend fun tagByName(name: String) = tags.getByName(name)
    suspend fun tagByProtectedKey(key: String) = tags.getByProtectedKey(key)
    suspend fun tagById(id: String) = tags.getById(id)
    suspend fun replaceLinks(recordingId: String, tagIds: List<String>) {
        tags.unlinkAll(recordingId)
        tagIds.forEach { tags.link(RecordingTag(recordingId, it)) }
    }
    suspend fun upsertNight(n: Night) = nights.upsert(n)
}
