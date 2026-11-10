package com.vislupus.dream_records.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded

/**
 * One audio recording + its transcript.
 * [nightDate] is "yyyy-MM-dd" of the night the recording belongs to (see AppConfig.Nights).
 */
@Entity(tableName = "recordings", indices = [Index("nightDate"), Index("createdAt")])
data class Recording(
    @PrimaryKey val id: String,
    val createdAt: Long,            // epoch millis
    val durationMs: Long,
    val fileName: String,           // relative to the audio dir inside the sandbox; "" if no audio
    val transcript: String = "",
    val transcriptSource: String = "", // "gemini" | "groq:<model>" | "elevenlabs" | "manual" | "import"
    val nightDate: String,
    val autoStopped: Boolean = false,
    val updatedAt: Long = createdAt,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int,
    /** Non-null for built-in (protected) tags; matches DefaultTag.key. */
    val protectedKey: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "recording_tags",
    primaryKeys = ["recordingId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(entity = Recording::class, parentColumns = ["id"], childColumns = ["recordingId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class RecordingTag(val recordingId: String, val tagId: String)

/** Optional combined text for a whole night (audios stay separate). */
@Entity(tableName = "nights")
data class Night(
    @PrimaryKey val nightDate: String,
    val combinedText: String = "",
    val updatedAt: Long,
)

data class RecordingWithTags(
    @Embedded val recording: Recording,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(RecordingTag::class, parentColumn = "recordingId", entityColumn = "tagId"),
    )
    val tags: List<Tag>,
)

data class TagWithCount(val id: String, val name: String, val colorIndex: Int, val protectedKey: String?, val createdAt: Long, val count: Int) {
    fun toTag() = Tag(id, name, colorIndex, protectedKey, createdAt)
}
