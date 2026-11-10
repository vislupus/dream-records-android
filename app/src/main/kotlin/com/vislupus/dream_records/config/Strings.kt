package com.vislupus.dream_records.config

import androidx.compose.runtime.compositionLocalOf

/**
 * Every piece of UI text, in one place. The app ships English and Bulgarian;
 * the language is switched from Settings. To add a language, add another object
 * implementing [Strings] and register it in [Strings.forCode].
 */
interface Strings {
    val languageCode: String
    val languageName: String

    // Bottom menu
    val tabRecord: String
    val tabLibrary: String
    val tabTags: String
    val tabCalendar: String
    val tabSettings: String

    // Record screen
    val tapToRecord: String
    val recording: String
    val tapToStop: String
    val saved: String
    val discardedTooShort: String
    val micPermissionNeeded: String
    val grantPermission: String
    val autoStopAfter: String
    val stoppedSilence: String
    val lastRecording: String

    // Library
    val libraryTitle: String
    val searchHint: String
    val noRecordings: String
    val noResults: String
    val filterByTag: String
    val clearFilters: String
    val recordingsCount: (Int) -> String
    val night: String
    val nightOf: (String) -> String
    val combineNight: String
    val combinedText: String
    val untranscribed: String
    val durationLabel: String

    // Recording detail
    val transcript: String
    val transcriptHint: String
    val transcribeWith: String
    val transcribing: String
    val offline: String
    val noApiKey: String
    val transcriptionFailed: (String) -> String
    val transcriptionDone: String
    val replaceTranscript: String
    val replaceTranscriptBody: String
    val replace: String
    val tags: String
    val addTag: String
    val newTag: String
    val noTagsYet: String
    val deleteRecording: String
    val deleteRecordingBody: String
    val audioMissing: String
    val recordedAt: String

    // Night detail
    val nightDetailTitle: String
    val combinedTextHint: String
    val combineNow: String
    val combineConfirm: String
    val combineConfirmBody: String
    val partsOfNight: String

    // Tags screen
    val tagsTitle: String
    val createTag: String
    val editTag: String
    val tagName: String
    val tagColor: String
    val protectedTag: String
    val protectedTagHint: String
    val mergeTag: String
    val mergeInto: String
    val mergeConfirm: (String, String) -> String
    val deleteTag: String
    val deleteTagBody: (String) -> String
    val usedIn: (Int) -> String
    val tagExists: String

    // Calendar
    val calendarTitle: String
    val today: String
    val noRecordingsThisDay: String
    val dayNames: List<String>   // Mon..Sun, 2-letter
    val legend: String

    // Settings
    val settingsTitle: String
    val language: String
    val sectionRecording: String
    val silenceTimeout: String
    val silenceTimeoutHint: String
    val minutesShort: String
    val sectionTranscription: String
    val geminiKey: String
    val groqKey: String
    val groqModel: String
    val elevenLabsKey: String
    val apiKeyHint: String
    val transcriptionLanguage: String
    val transcriptionLanguageHint: String
    val sectionData: String
    val exportData: String
    val exportHint: String
    val importData: String
    val importHint: String
    val exportDone: (String) -> String
    val importDone: (Int, Int, Int) -> String
    val importFailed: (String) -> String
    val sectionAbout: String
    val storageLocation: String
    val version: String
    val share: String
    val working: String

    // Generic
    val save: String
    val cancel: String
    val delete: String
    val ok: String
    val edit: String
    val done: String
    val back: String
    val close: String
    val play: String
    val pause: String
    val yes: String
    val no: String
    val error: String
    val merge: String
    val select: String
    val unavailable: String

    companion object {
        val ALL: List<Strings> get() = listOf(En, Bg)
        fun forCode(code: String): Strings = ALL.firstOrNull { it.languageCode == code } ?: En
    }
}

/** Provides the active language to every composable. See MainActivity. */
val LocalStrings = compositionLocalOf<Strings> { En }

object En : Strings {
    override val languageCode = "en"
    override val languageName = "English"

    override val tabRecord = "Record"
    override val tabLibrary = "Library"
    override val tabTags = "Tags"
    override val tabCalendar = "Calendar"
    override val tabSettings = "Settings"

    override val tapToRecord = "Tap to record"
    override val recording = "Recording"
    override val tapToStop = "Tap to stop"
    override val saved = "Saved"
    override val discardedTooShort = "Too short, discarded"
    override val micPermissionNeeded = "Microphone access is needed to record your dreams."
    override val grantPermission = "Allow microphone"
    override val autoStopAfter = "Stops after %s min of silence"
    override val stoppedSilence = "Stopped automatically after silence"
    override val lastRecording = "Last recording"

    override val libraryTitle = "Library"
    override val searchHint = "Search dreams…"
    override val noRecordings = "No recordings yet.\nTap the microphone to capture your first dream."
    override val noResults = "Nothing matches your search."
    override val filterByTag = "Filter by tag"
    override val clearFilters = "Clear"
    override val recordingsCount = { n: Int -> if (n == 1) "1 recording" else "$n recordings" }
    override val night = "Night"
    override val nightOf = { d: String -> "Night of $d" }
    override val combineNight = "Combine night"
    override val combinedText = "Combined text"
    override val untranscribed = "Not transcribed yet"
    override val durationLabel = "Duration"

    override val transcript = "Transcript"
    override val transcriptHint = "Listen and type or correct the text here…"
    override val transcribeWith = "Transcribe with"
    override val transcribing = "Transcribing…"
    override val offline = "Offline"
    override val noApiKey = "No API key (add it in Settings)"
    override val transcriptionFailed = { e: String -> "Transcription failed: $e" }
    override val transcriptionDone = "Transcript updated"
    override val replaceTranscript = "Replace transcript?"
    override val replaceTranscriptBody = "The current text will be overwritten by the new transcription."
    override val replace = "Replace"
    override val tags = "Tags"
    override val addTag = "Add tag"
    override val newTag = "New tag"
    override val noTagsYet = "No tags"
    override val deleteRecording = "Delete recording"
    override val deleteRecordingBody = "The audio and its text will be deleted permanently."
    override val audioMissing = "Audio file not found (imported without audio)"
    override val recordedAt = "Recorded"

    override val nightDetailTitle = "Night"
    override val combinedTextHint = "Combined text of the whole night. Edit freely."
    override val combineNow = "Combine recordings into one text"
    override val combineConfirm = "Rebuild combined text?"
    override val combineConfirmBody = "The combined text will be rebuilt from the individual transcripts. Your edits to the combined text will be lost."
    override val partsOfNight = "Recordings this night"

    override val tagsTitle = "Tags"
    override val createTag = "Create tag"
    override val editTag = "Edit tag"
    override val tagName = "Name"
    override val tagColor = "Colour"
    override val protectedTag = "Built-in tag"
    override val protectedTagHint = "Shown as a dot on the calendar. Can be renamed, not deleted."
    override val mergeTag = "Merge into another tag"
    override val mergeInto = "Merge into"
    override val mergeConfirm = { a: String, b: String -> "Move all recordings from \"$a\" to \"$b\" and delete \"$a\"?" }
    override val deleteTag = "Delete tag"
    override val deleteTagBody = { n: String -> "Delete \"$n\"? It will be removed from all recordings." }
    override val usedIn = { n: Int -> if (n == 1) "1 recording" else "$n recordings" }
    override val tagExists = "A tag with this name already exists."

    override val calendarTitle = "Calendar"
    override val today = "Today"
    override val noRecordingsThisDay = "No recordings on this day."
    override val dayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    override val legend = "Legend"

    override val settingsTitle = "Settings"
    override val language = "Language"
    override val sectionRecording = "Recording"
    override val silenceTimeout = "Auto-stop after silence"
    override val silenceTimeoutHint = "If you fall asleep, the recording is saved and stopped after this much silence."
    override val minutesShort = "min"
    override val sectionTranscription = "Transcription"
    override val geminiKey = "Gemini API key"
    override val groqKey = "GroqCloud API key"
    override val groqModel = "Groq model"
    override val elevenLabsKey = "ElevenLabs API key"
    override val apiKeyHint = "Stored only on this phone."
    override val transcriptionLanguage = "Spoken language"
    override val transcriptionLanguageHint = "ISO code sent to the models (e.g. en, bg). Leave empty for auto-detect."
    override val sectionData = "Data"
    override val exportData = "Export (zip)"
    override val exportHint = "YAML with all recordings, texts and tags, plus the audio files."
    override val importData = "Import (yaml or zip)"
    override val importHint = "Texts and tags are updated from the file. Audio missing on this phone is restored from a zip; existing audio is kept."
    override val exportDone = { f: String -> "Exported $f" }
    override val importDone = { r: Int, t: Int, a: Int -> "Imported $r recordings, $t tags" + if (a > 0) ", restored $a audio files" else "" }
    override val importFailed = { e: String -> "Import failed: $e" }
    override val sectionAbout = "About"
    override val storageLocation = "All data stays in the app's private folder"
    override val version = "Version"
    override val share = "Share"
    override val working = "Working…"

    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val ok = "OK"
    override val edit = "Edit"
    override val done = "Done"
    override val back = "Back"
    override val close = "Close"
    override val play = "Play"
    override val pause = "Pause"
    override val yes = "Yes"
    override val no = "No"
    override val error = "Error"
    override val merge = "Merge"
    override val select = "Select"
    override val unavailable = "Unavailable"
}

object Bg : Strings {
    override val languageCode = "bg"
    override val languageName = "Български"

    override val tabRecord = "Запис"
    override val tabLibrary = "Библиотека"
    override val tabTags = "Тагове"
    override val tabCalendar = "Календар"
    override val tabSettings = "Настройки"

    override val tapToRecord = "Натисни за запис"
    override val recording = "Записва"
    override val tapToStop = "Натисни за стоп"
    override val saved = "Записано"
    override val discardedTooShort = "Твърде кратък, изтрит"
    override val micPermissionNeeded = "Нужен е достъп до микрофона, за да записваш сънищата си."
    override val grantPermission = "Разреши микрофона"
    override val autoStopAfter = "Спира след %s мин тишина"
    override val stoppedSilence = "Спрян автоматично след тишина"
    override val lastRecording = "Последен запис"

    override val libraryTitle = "Библиотека"
    override val searchHint = "Търси в сънищата…"
    override val noRecordings = "Още няма записи.\nНатисни микрофона, за да запишеш първия си сън."
    override val noResults = "Нищо не отговаря на търсенето."
    override val filterByTag = "Филтър по таг"
    override val clearFilters = "Изчисти"
    override val recordingsCount = { n: Int -> if (n == 1) "1 запис" else "$n записа" }
    override val night = "Нощ"
    override val nightOf = { d: String -> "Нощта на $d" }
    override val combineNight = "Обедини нощта"
    override val combinedText = "Обединен текст"
    override val untranscribed = "Още не е транскрибиран"
    override val durationLabel = "Продължителност"

    override val transcript = "Текст"
    override val transcriptHint = "Слушай и пиши или поправяй текста тук…"
    override val transcribeWith = "Транскрибирай с"
    override val transcribing = "Транскрибира…"
    override val offline = "Няма интернет"
    override val noApiKey = "Няма API ключ (добави го в Настройки)"
    override val transcriptionFailed = { e: String -> "Транскрипцията се провали: $e" }
    override val transcriptionDone = "Текстът е обновен"
    override val replaceTranscript = "Да замени ли текста?"
    override val replaceTranscriptBody = "Сегашният текст ще бъде презаписан с новата транскрипция."
    override val replace = "Замени"
    override val tags = "Тагове"
    override val addTag = "Добави таг"
    override val newTag = "Нов таг"
    override val noTagsYet = "Няма тагове"
    override val deleteRecording = "Изтрий записа"
    override val deleteRecordingBody = "Аудиото и текстът ще бъдат изтрити завинаги."
    override val audioMissing = "Аудио файлът липсва (импортиран без аудио)"
    override val recordedAt = "Записан"

    override val nightDetailTitle = "Нощ"
    override val combinedTextHint = "Обединен текст на цялата нощ. Редактирай свободно."
    override val combineNow = "Обедини записите в един текст"
    override val combineConfirm = "Да се пресъздаде ли обединеният текст?"
    override val combineConfirmBody = "Обединеният текст ще бъде построен отново от отделните текстове. Промените в него ще се загубят."
    override val partsOfNight = "Записи през тази нощ"

    override val tagsTitle = "Тагове"
    override val createTag = "Нов таг"
    override val editTag = "Редакция на таг"
    override val tagName = "Име"
    override val tagColor = "Цвят"
    override val protectedTag = "Вграден таг"
    override val protectedTagHint = "Показва се като точка в календара. Може да се преименува, но не и да се трие."
    override val mergeTag = "Обедини с друг таг"
    override val mergeInto = "Обедини в"
    override val mergeConfirm = { a: String, b: String -> "Всички записи от „$a“ да минат към „$b“ и „$a“ да се изтрие?" }
    override val deleteTag = "Изтрий тага"
    override val deleteTagBody = { n: String -> "Да се изтрие ли „$n“? Ще бъде премахнат от всички записи." }
    override val usedIn = { n: Int -> if (n == 1) "1 запис" else "$n записа" }
    override val tagExists = "Таг с това име вече съществува."

    override val calendarTitle = "Календар"
    override val today = "Днес"
    override val noRecordingsThisDay = "Няма записи през този ден."
    override val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
    override val legend = "Легенда"

    override val settingsTitle = "Настройки"
    override val language = "Език"
    override val sectionRecording = "Запис"
    override val silenceTimeout = "Автоматично спиране при тишина"
    override val silenceTimeoutHint = "Ако заспиш, записът се запазва и спира след толкова тишина."
    override val minutesShort = "мин"
    override val sectionTranscription = "Транскрипция"
    override val geminiKey = "Gemini API ключ"
    override val groqKey = "GroqCloud API ключ"
    override val groqModel = "Groq модел"
    override val elevenLabsKey = "ElevenLabs API ключ"
    override val apiKeyHint = "Пази се само на този телефон."
    override val transcriptionLanguage = "Език на говора"
    override val transcriptionLanguageHint = "ISO код, който се праща на моделите (напр. en, bg). Празно = автоматично."
    override val sectionData = "Данни"
    override val exportData = "Експорт (zip)"
    override val exportHint = "YAML с всички записи, текстове и тагове, плюс аудио файловете."
    override val importData = "Импорт (yaml или zip)"
    override val importHint = "Текстовете и таговете се обновяват от файла. Липсващото на телефона аудио се възстановява от zip; наличното се запазва."
    override val exportDone = { f: String -> "Експортирано: $f" }
    override val importDone = { r: Int, t: Int, a: Int -> "Импортирани $r записа, $t тага" + if (a > 0) ", възстановени $a аудио файла" else "" }
    override val importFailed = { e: String -> "Импортът се провали: $e" }
    override val sectionAbout = "За приложението"
    override val storageLocation = "Всички данни остават в личната папка на приложението"
    override val version = "Версия"
    override val share = "Сподели"
    override val working = "Обработва…"

    override val save = "Запази"
    override val cancel = "Отказ"
    override val delete = "Изтрий"
    override val ok = "ОК"
    override val edit = "Редактирай"
    override val done = "Готово"
    override val back = "Назад"
    override val close = "Затвори"
    override val play = "Пусни"
    override val pause = "Пауза"
    override val yes = "Да"
    override val no = "Не"
    override val error = "Грешка"
    override val merge = "Обедини"
    override val select = "Избери"
    override val unavailable = "Недостъпно"
}
