package com.vislupus.dream_records.config

import android.media.MediaRecorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================================
 *  APP CONFIG — the single place that controls how the app looks and behaves.
 * ============================================================================
 *
 * Everything that is a "knob" lives here: colours, sizes, timings, audio
 * quality, API endpoints, default tags, file names, calendar rules.
 * Change a value here and the whole app follows; you never need to touch the
 * screens or the data layer.
 *
 * Related config files in this package:
 *   - Strings.kt   : every piece of UI text (English + Bulgarian)
 *   - TagColors.kt : the 30-colour palette for tags (3 rows x 10)
 *   - DefaultTags  : the protected, always-available tags (bottom of this file)
 */
object AppConfig {

    // ------------------------------------------------------------------ App
    object App {
        /** Shown in the recorder notification. */
        const val NAME = "Dream Records"
        /** Default UI language code: "en" or "bg". */
        const val DEFAULT_LANGUAGE = "en"
        /** Bottom-menu tab the app always opens on. */
        val START_TAB = Tab.RECORD
    }

    enum class Tab { RECORD, LIBRARY, TAGS, CALENDAR, SETTINGS }

    // ---------------------------------------------------------------- Theme
    /**
     * Dark-only palette tuned for use at night. All colours are defined once here.
     * Material3 maps them in ui/theme/Theme.kt.
     */
    object Theme {
        val background = Color(0xFF0B0D12)
        val surface = Color(0xFF12151D)
        val surfaceElevated = Color(0xFF1A1E29)
        val surfaceBorder = Color(0xFF262B38)
        val onBackground = Color(0xFFE6E8EF)
        val onSurface = Color(0xFFE6E8EF)
        val onSurfaceMuted = Color(0xFF8B91A3)
        val onSurfaceFaint = Color(0xFF555B6C)
        val accent = Color(0xFF9DB4FF)       // primary action colour (calm blue, easy on the eyes at night)
        val onAccent = Color(0xFF0B0D12)
        val recording = Color(0xFFFF6B6B)    // mic button while recording
        val success = Color(0xFF7ED9A4)
        val warning = Color(0xFFFFC46B)
        val danger = Color(0xFFFF6B6B)
        val weekendText = Color(0xFFB8A1FF)  // Saturday/Sunday labels in the calendar
        val todayRing = accent

        /** Overall brightness multiplier for the whole UI (1.0 = normal). 0.85 dims slightly for 3 AM eyes. */
        const val NIGHT_DIM_ALPHA = 1.0f
    }

    // ------------------------------------------------------------ Typography
    object Typography {
        /** Roboto is the Android system sans-serif, so no font file is bundled. */
        val titleSize = 22.sp
        val subtitleSize = 16.sp
        val bodySize = 16.sp
        val transcriptSize = 17.sp
        val transcriptLineHeight = 26.sp
        val captionSize = 13.sp
        val labelSize = 12.sp
        val timerSize = 48.sp
    }

    // ---------------------------------------------------------------- Layout
    object Layout {
        val screenPadding = 20.dp
        val itemSpacing = 12.dp
        val sectionSpacing = 24.dp
        val cornerRadius = 16.dp
        val chipCornerRadius = 10.dp
        val cardPadding = 16.dp
        val bottomBarHeight = 64.dp
        val iconSize = 22.dp
        val iconSizeSmall = 18.dp
        val iconSizeLarge = 28.dp

        /** Big mic button on the Record screen. */
        val micButtonSize = 168.dp
        val micIconSize = 64.dp
        val micRingWidth = 3.dp

        /** Tag colour grid on the tag editor. */
        const val TAG_COLOR_COLUMNS = 10
        const val TAG_COLOR_ROWS = 3
        val tagColorCellSize = 28.dp
        val tagColorCellSpacing = 6.dp

        /** Calendar. */
        val calendarDaySize = 44.dp
        val calendarDotSize = 6.dp
        const val CALENDAR_MAX_DOTS_PER_DAY = 4
    }

    // -------------------------------------------------------------- Recording
    object Recording {
        /**
         * Audio source. MediaRecorder.AudioSource.MIC applies the device's automatic gain
         * control, which helps a lot for whispering. VOICE_RECOGNITION is cleaner but quieter
         * on many Xiaomi phones; UNPROCESSED is fully raw (not recommended for whispers).
         */
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val SAMPLE_RATE_HZ = 48_000
        const val BIT_RATE_BPS = 256_000     // AAC at 256 kbps mono ~ near-transparent for speech
        const val CHANNELS = 1
        const val FILE_EXTENSION = "m4a"
        const val MIME_TYPE = "audio/mp4"

        /** Options offered in Settings for auto-stop after silence (minutes). */
        val SILENCE_TIMEOUT_OPTIONS_MIN = listOf(1, 5, 10, 15, 20, 30)
        const val DEFAULT_SILENCE_TIMEOUT_MIN = 10

        /**
         * Silence detection: amplitude (0..32767) below which a poll counts as silence.
         * Keep this low — whispering must NOT count as silence. Tune on your phone.
         */
        const val SILENCE_AMPLITUDE_THRESHOLD = 900
        /** How often the recorder polls the microphone level. */
        const val LEVEL_POLL_INTERVAL_MS = 250L
        /** Hard safety limit for a single recording (minutes), in case silence detection never fires. */
        const val MAX_RECORDING_MINUTES = 120
        /** Minimum length; shorter recordings (accidental taps) are discarded. */
        const val MIN_RECORDING_SECONDS = 2

        /** Keep the screen awake while recording? false = screen may turn off, recording continues. */
        const val KEEP_SCREEN_ON_WHILE_RECORDING = false

        /** Notification channel for the foreground recorder service. */
        const val NOTIFICATION_CHANNEL_ID = "recorder"
        const val NOTIFICATION_ID = 1001

        /** Folder (inside the app sandbox) where audio files are stored. */
        const val AUDIO_DIR = "audio"
    }

    // ---------------------------------------------------------------- Player
    object Player {
        const val SKIP_SECONDS = 10
        const val POSITION_POLL_MS = 200L
        /** Delay before an edited transcript is auto-saved. */
        const val AUTOSAVE_DEBOUNCE_MS = 700L
    }

    // ---------------------------------------------------------------- Nights
    /**
     * A "night" groups all recordings of one sleep. A recording made before
     * NIGHT_BOUNDARY_HOUR belongs to that same calendar date; one made at or after it
     * belongs to the NEXT date (an 11 PM recording on the 13th is "the night of the 14th").
     * Calendar dots and library groups use this date.
     */
    object Nights {
        const val NIGHT_BOUNDARY_HOUR = 18
        /** Separator placed between recordings when combining a night into one text. */
        const val COMBINED_TEXT_SEPARATOR = "\n\n"
        /** Prefix each part with its time (HH:mm) when combining. */
        const val COMBINED_TEXT_WITH_TIMESTAMPS = true
    }

    // -------------------------------------------------------------- Calendar
    object Calendar {
        /** 1 = Monday ... 7 = Sunday (java.time.DayOfWeek values). */
        const val FIRST_DAY_OF_WEEK = 1
        val WEEKEND_DAYS = setOf(6, 7)
        const val TIME_PATTERN = "HH:mm"
        const val DATE_PATTERN = "dd.MM.yyyy"
        const val DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm"
        const val MONTH_PATTERN = "LLLL yyyy"
    }

    // ---------------------------------------------------------- Transcription
    object Transcription {
        /** Generic prompt sent to models that accept one. Mention whispering + dream context. */
        const val PROMPT =
            "This is a quiet, possibly whispered, sleepy voice note describing a dream, recorded at night. " +
                "Transcribe it verbatim, keep sentence structure, do not summarise."

        /** Max audio file size (bytes) to send inline. */
        const val MAX_UPLOAD_BYTES = 24L * 1024 * 1024
        const val HTTP_TIMEOUT_SECONDS = 180L

        object Gemini {
            const val ID = "gemini"
            const val DISPLAY_NAME = "Gemini 3.5 Transcribe"
            /**
             * Dedicated transcription model. If it keeps returning an empty transcript for you,
             * switch to a general model such as "gemini-3.5-flash-lite" — the app reads both formats.
             */
            const val MODEL = "gemini-3.5-transcribe"
            const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/"
        }

        object Groq {
            const val ID = "groq"
            const val DISPLAY_NAME = "Groq Whisper"
            const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
            /** The two Groq models the user can pick from in Settings. */
            val MODELS = listOf("whisper-large-v3", "whisper-large-v3-turbo")
            const val DEFAULT_MODEL = "whisper-large-v3"
        }

        object ElevenLabs {
            const val ID = "elevenlabs"
            const val DISPLAY_NAME = "ElevenLabs Scribe v2"
            const val MODEL = "scribe_v2"
            const val ENDPOINT = "https://api.elevenlabs.io/v1/speech-to-text"
        }
    }

    // ------------------------------------------------------------ Export/Import
    object ExportImport {
        const val EXPORT_DIR = "exports"
        const val YAML_FILE_NAME = "dream_records.yaml"
        const val ZIP_PREFIX = "dream_records_"
        const val ZIP_AUDIO_DIR = "audio"
        const val FORMAT_VERSION = 1
        /** Include audio files in the exported zip. */
        const val EXPORT_INCLUDE_AUDIO = true
        /**
         * On import from a zip, copy audio files the phone is missing (e.g. after reinstalling).
         * Audio the phone already has is never replaced. false = ignore all audio in the zip.
         */
        const val IMPORT_RESTORE_MISSING_AUDIO = true
        /** Extensions treated as audio inside an imported zip. */
        val AUDIO_EXTENSIONS = setOf("m4a", "mp3", "wav", "ogg", "opus", "aac", "flac", "webm", "3gp")
    }

    // ------------------------------------------------------------------ Tags
    object Tags {
        /** Max tag name length. */
        const val MAX_NAME_LENGTH = 32
        /** Index into TagColors.PALETTE used for a brand-new tag. */
        const val DEFAULT_COLOR_INDEX = 20
    }

    // -------------------------------------------------------------- Database
    object Database {
        /** Lives in the app sandbox: /data/data/com.vislupus.dream_records/databases/ */
        const val NAME = "dream_records.db"
        const val VERSION = 1
    }
}

/**
 * Protected tags: always exist, cannot be deleted or merged away, and are shown as
 * coloured dots on the calendar. Names can still be renamed/recoloured by the user.
 * `key` is stable and is what the calendar and import/export use.
 */
data class DefaultTag(val key: String, val nameEn: String, val nameBg: String, val colorIndex: Int)

object DefaultTags {
    val ALL = listOf(
        DefaultTag("lucid", "Lucid dream", "Осъзнат сън", TagColors.SKY_BLUE),
        DefaultTag("excursion", "Excursion", "Екскурзия", TagColors.RED),
        DefaultTag("hiking", "Hiking", "Поход", TagColors.BROWN),
        DefaultTag("sick", "Sick", "Болен", TagColors.YELLOW_GREEN),
        DefaultTag("nightmare", "Nightmare", "Кошмар", TagColors.PURPLE),
        DefaultTag("recurring", "Recurring", "Повтарящ се", TagColors.ORANGE),
        DefaultTag("nap", "Nap", "Дрямка", TagColors.TEAL),
    )
}
