package com.vislupus.dream_records.io

import android.content.Context
import android.net.Uri
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.TagColors
import com.vislupus.dream_records.data.Repository
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.data.db.Night
import com.vislupus.dream_records.data.db.Recording
import com.vislupus.dream_records.data.db.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag as YamlTag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Export: a zip with dream_records.yaml + audio/ files.
 * Import: a .yaml file, or a zip containing one. Transcripts, tags and combined night texts
 * are overwritten from the file. Audio in the zip only fills gaps: a file the phone is missing
 * is restored, a file the phone already has is never replaced (AppConfig.ExportImport).
 *
 * YAML layout (edit freely on the computer):
 *
 *   format: dream_records
 *   version: 1
 *   tags:
 *     - { name: Lucid dream, color: 7, protected: lucid }
 *   recordings:
 *     - id: <uuid>
 *       created_at: 2026-10-14T03:12:40
 *       night: 2026-10-14
 *       duration_ms: 65000
 *       audio: dream_2026-10-14T03-12-40.m4a
 *       source: groq:whisper-large-v3
 *       tags: [Lucid dream, Flying]
 *       transcript: |
 *         I was flying over...
 *   nights:
 *     - { date: 2026-10-14, combined_text: "..." }
 */
class ExportImport(private val context: Context, private val repo: Repository) {

    data class ImportResult(val recordings: Int, val tags: Int, val audioRestored: Int = 0)

    /** What a pass over the zip produced: the YAML text and the audio files restored into the sandbox. */
    private class ZipScan(val yaml: String, val restored: List<File>)

    private val isoDateTime: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ------------------------------------------------------------ EXPORT
    suspend fun exportZip(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, AppConfig.ExportImport.EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest export in the sandbox
        val stamp = TimeUtils.toLocal(TimeUtils.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val zip = File(dir, "${AppConfig.ExportImport.ZIP_PREFIX}$stamp.zip")

        val yaml = buildYaml()
        ZipOutputStream(FileOutputStream(zip).buffered()).use { out ->
            out.putNextEntry(ZipEntry(AppConfig.ExportImport.YAML_FILE_NAME))
            out.write(yaml.toByteArray(Charsets.UTF_8))
            out.closeEntry()
            if (AppConfig.ExportImport.EXPORT_INCLUDE_AUDIO) {
                repo.allRecordings().forEach { r ->
                    repo.audioFile(r)?.let { f ->
                        out.putNextEntry(ZipEntry("${AppConfig.ExportImport.ZIP_AUDIO_DIR}/${f.name}"))
                        f.inputStream().use { it.copyTo(out) }
                        out.closeEntry()
                    }
                }
            }
        }
        zip
    }

    private suspend fun buildYaml(): String {
        val tags = repo.getTags().associateBy { it.id }
        val links = repo.allLinks().groupBy({ it.recordingId }, { it.tagId })
        val root = linkedMapOf<String, Any>(
            "format" to "dream_records",
            "version" to AppConfig.ExportImport.FORMAT_VERSION,
            "exported_at" to TimeUtils.isoNow(),
            "tags" to tags.values.sortedBy { it.name.lowercase() }.map { t ->
                linkedMapOf<String, Any>("name" to t.name, "color" to t.colorIndex).also { m ->
                    t.protectedKey?.let { m["protected"] = it }
                }
            },
            "recordings" to repo.allRecordings().sortedBy { it.createdAt }.map { r ->
                linkedMapOf<String, Any>(
                    "id" to r.id,
                    "created_at" to TimeUtils.toLocal(r.createdAt).format(isoDateTime),
                    "night" to r.nightDate,
                    "duration_ms" to r.durationMs,
                    "audio" to r.fileName,
                    "source" to r.transcriptSource,
                    "auto_stopped" to r.autoStopped,
                    "tags" to (links[r.id] ?: emptyList()).mapNotNull { tags[it]?.name },
                    "transcript" to r.transcript,
                )
            },
            "nights" to repo.allNights().filter { it.combinedText.isNotBlank() }.sortedBy { it.nightDate }.map { n ->
                linkedMapOf<String, Any>("date" to n.nightDate, "combined_text" to n.combinedText)
            },
        )
        val opts = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isAllowUnicode = true
            indent = 2
            width = 100
            isPrettyFlow = true
        }
        return Yaml(LiteralStringRepresenter(opts), opts).dump(root)
    }

    /** Multi-line strings are written as `|` literal blocks so they stay readable on the computer. */
    private class LiteralStringRepresenter(opts: DumperOptions) : Representer(opts) {
        init {
            representers[String::class.java] = Represent { data ->
                val s = data as String
                // SnakeYAML 2.x requires a non-null style. PLAIN falls back to quotes automatically when needed.
                val style = if (s.contains('\n')) DumperOptions.ScalarStyle.LITERAL else DumperOptions.ScalarStyle.PLAIN
                representScalar(YamlTag.STR, s, style)
            }
        }
    }

    // ------------------------------------------------------------ IMPORT
    /**
     * Streams the file — it is never loaded into memory as a whole, so a zip with
     * hundreds of MB of audio is fine. The YAML entry is read as text; audio entries are
     * either copied straight to disk (when the phone is missing that file) or skipped.
     */
    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val scan = if (isZip(uri)) {
            try {
                openStream(uri).use { scanZipStreaming(it) }
            } catch (e: java.util.zip.ZipException) {
                // Some archivers write zips that ZipInputStream can't stream (STORED entries with
                // data descriptors). Fall back to a temp copy + ZipFile, which reads the central directory.
                scanZipViaTempFile(uri)
            }
        } else {
            ZipScan(readYamlText(openStream(uri)), emptyList())
        }

        val restoredNames = scan.restored.map { it.name }.toSet()
        try {
            val (result, referenced) = importYaml(scan.yaml, restoredNames)
            // Restored files that no recording points to are just wasted space.
            scan.restored.filter { it.name !in referenced }.forEach { it.delete() }
            result
        } catch (e: Throwable) {
            scan.restored.forEach { it.delete() } // a failed import leaves nothing behind
            throw e
        }
    }

    private fun openStream(uri: Uri): java.io.InputStream =
        context.contentResolver.openInputStream(uri)?.buffered(BUFFER_SIZE) ?: throw IllegalArgumentException("cannot open file")

    /** Zip files start with "PK". Reads only the first two bytes. */
    private fun isZip(uri: Uri): Boolean = openStream(uri).use { s ->
        val b = ByteArray(2)
        s.read(b) == 2 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte()
    }

    private fun baseName(entryName: String) = entryName.substringAfterLast('/').substringAfterLast('\\')
    private fun isYamlName(name: String) = name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true)

    /**
     * Audio entry name that is safe to write into the sandbox audio dir: a plain file name
     * (no folders, no "..", no hidden files) with a known audio extension.
     */
    private fun safeAudioName(entryName: String): String? {
        val name = baseName(entryName)
        if (!SAFE_NAME.matches(name) || name.startsWith(".")) return null
        return name.takeIf { it.substringAfterLast('.', "").lowercase() in AppConfig.ExportImport.AUDIO_EXTENSIONS }
    }

    /**
     * Copies one zip entry to the audio dir if the phone doesn't have that file yet.
     * Writes to a ".part" file first, so an interrupted import never leaves a broken audio file.
     * @return the restored file, or null if skipped.
     */
    private fun restoreAudioIfMissing(entryName: String, input: java.io.InputStream): File? {
        if (!AppConfig.ExportImport.IMPORT_RESTORE_MISSING_AUDIO) return null
        val name = safeAudioName(entryName) ?: return null
        val target = File(repo.audioDir, name)
        if (target.exists()) return null // the phone's own copy always wins
        val part = File(repo.audioDir, "$name.part")
        try {
            part.outputStream().buffered(BUFFER_SIZE).use { input.copyTo(it, BUFFER_SIZE) }
            if (!part.renameTo(target)) throw java.io.IOException("cannot save $name")
        } catch (e: Exception) {
            part.delete()
            throw java.io.IOException("could not restore $name: ${e.message}", e)
        }
        return target
    }

    /** Reads a YAML stream as text, refusing anything absurdly large (a mis-picked file). */
    private fun readYamlText(input: java.io.InputStream): String = input.use { s ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val n = s.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_YAML_BYTES) throw IllegalArgumentException("YAML file is larger than ${MAX_YAML_BYTES / 1024 / 1024} MB")
            out.write(buf, 0, n)
        }
        out.toString(Charsets.UTF_8.name())
    }

    /** One pass over the zip: read the YAML, restore missing audio, skip everything else. */
    private fun scanZipStreaming(input: java.io.InputStream): ZipScan {
        val zin = ZipInputStream(input)
        // ZipInputStream stops at the end of the current entry; this wrapper keeps readers from closing it.
        val entryStream = object : java.io.FilterInputStream(zin) { override fun close() {} }
        var main: String? = null
        var fallback: String? = null
        val restored = mutableListOf<File>()
        try {
            var entry: ZipEntry? = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = baseName(entry.name)
                    when {
                        isYamlName(name) -> {
                            val text = readYamlText(entryStream)
                            if (name == AppConfig.ExportImport.YAML_FILE_NAME && main == null) main = text
                            else if (fallback == null) fallback = text
                        }
                        else -> restoreAudioIfMissing(entry.name, entryStream)?.let { restored += it }
                    }
                }
                zin.closeEntry() // skips whatever wasn't read
                entry = zin.nextEntry
            }
        } catch (e: Throwable) {
            restored.forEach { it.delete() }
            throw e
        }
        val yaml = main ?: fallback ?: run {
            restored.forEach { it.delete() }
            throw IllegalArgumentException("no YAML file inside the zip")
        }
        return ZipScan(yaml, restored)
    }

    private fun scanZipViaTempFile(uri: Uri): ZipScan {
        val tmp = File.createTempFile("import_", ".zip", context.cacheDir)
        val restored = mutableListOf<File>()
        try {
            openStream(uri).use { input -> tmp.outputStream().use { input.copyTo(it, BUFFER_SIZE) } }
            java.util.zip.ZipFile(tmp).use { zip ->
                val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
                val yamlEntries = entries.filter { isYamlName(baseName(it.name)) }
                val yamlEntry = yamlEntries.firstOrNull { baseName(it.name) == AppConfig.ExportImport.YAML_FILE_NAME }
                    ?: yamlEntries.firstOrNull()
                    ?: throw IllegalArgumentException("no YAML file inside the zip")
                val yaml = readYamlText(zip.getInputStream(yamlEntry))
                entries.filter { it !in yamlEntries }.forEach { e ->
                    zip.getInputStream(e).use { restoreAudioIfMissing(e.name, it) }?.let { restored += it }
                }
                return ZipScan(yaml, restored)
            }
        } catch (e: Throwable) {
            restored.forEach { it.delete() }
            throw e
        } finally {
            tmp.delete()
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        /** Text-only YAML; even thousands of dreams are a few MB. */
        const val MAX_YAML_BYTES = 50L * 1024 * 1024
        val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,128}$")
    }

    @Suppress("UNCHECKED_CAST")
    /** @return the result and the set of audio file names the imported recordings point to. */
    private suspend fun importYaml(text: String, restoredNames: Set<String>): Pair<ImportResult, Set<String>> {
        val loader = LoaderOptions().apply { codePointLimit = 64 * 1024 * 1024 }
        val root = Yaml(SafeConstructor(loader)).load<Any>(text) as? Map<String, Any?>
            ?: throw IllegalArgumentException("not a dream_records YAML")
        if (root["format"] != "dream_records") throw IllegalArgumentException("unknown format")

        val now = TimeUtils.now()
        var tagCount = 0
        val tagIdByName = HashMap<String, String>()

        // 1. Tags: match protected ones by key, others by name (case-insensitive).
        (root["tags"] as? List<Map<String, Any?>>)?.forEach { t ->
            val name = t["name"]?.toString()?.trim().orEmpty()
            if (name.isEmpty()) return@forEach
            val color = (t["color"] as? Number)?.toInt()?.coerceIn(0, TagColors.PALETTE.lastIndex) ?: AppConfig.Tags.DEFAULT_COLOR_INDEX
            val protectedKey = t["protected"]?.toString()
            val existing = protectedKey?.let { repo.tagByProtectedKey(it) } ?: repo.tagByName(name)
            val tag = if (existing != null) existing.copy(name = name, colorIndex = color)
            else Tag(UUID.randomUUID().toString(), name, color, protectedKey, now)
            // Rename collision: a different tag already has this name -> keep the existing name.
            if (repo.tagNameExists(name, tag.id)) tagIdByName[name.lowercase()] = repo.tagByName(name)!!.id
            else { repo.upsertTag(tag); tagIdByName[name.lowercase()] = tag.id }
            tagCount++
        }

        // 2. Recordings: match by id; the transcript in the file wins.
        var recCount = 0
        val referenced = HashSet<String>()
        (root["recordings"] as? List<Map<String, Any?>>)?.forEach { r ->
            val id = r["id"]?.toString() ?: UUID.randomUUID().toString()
            val createdAt = parseDateTime(r["created_at"]) ?: now
            val existing = repo.getRecording(id)
            val fileFromYaml = r["audio"]?.toString()?.let { safeAudioName(it) }.orEmpty()
            // Use a file that is actually on the phone: the phone's own first, then one just restored.
            // "" means no audio (the UI shows a warning); a later zip import can still fill it in.
            val fileName = listOfNotNull(existing?.fileName, fileFromYaml)
                .firstOrNull { it.isNotBlank() && File(repo.audioDir, it).exists() }.orEmpty()
            if (fileName.isNotBlank()) referenced += fileName
            val rec = Recording(
                id = id,
                createdAt = existing?.createdAt ?: createdAt,
                durationMs = existing?.durationMs ?: (r["duration_ms"] as? Number)?.toLong() ?: 0L,
                fileName = fileName,
                transcript = r["transcript"]?.toString().orEmpty(),
                transcriptSource = r["source"]?.toString().orEmpty().ifBlank { "import" },
                nightDate = parseDateKey(r["night"]) ?: TimeUtils.nightDateFor(createdAt),
                autoStopped = r["auto_stopped"] as? Boolean ?: existing?.autoStopped ?: false,
                updatedAt = now,
            )
            repo.upsertRecording(rec)

            // Tag links (unknown tag names are created on the fly).
            val tagIds = (r["tags"] as? List<Any?>)?.mapNotNull { it?.toString()?.trim() }?.filter { it.isNotEmpty() }?.map { name ->
                tagIdByName[name.lowercase()] ?: repo.createTag(name, AppConfig.Tags.DEFAULT_COLOR_INDEX).id.also {
                    tagIdByName[name.lowercase()] = it; tagCount++
                }
            } ?: emptyList()
            repo.replaceLinks(id, tagIds)
            recCount++
        }

        // 3. Nights.
        (root["nights"] as? List<Map<String, Any?>>)?.forEach { n ->
            val date = parseDateKey(n["date"]) ?: return@forEach
            repo.upsertNight(Night(date, n["combined_text"]?.toString().orEmpty(), now))
        }

        return ImportResult(recCount, tagCount, audioRestored = restoredNames.count { it in referenced }) to referenced
    }

    /**
     * SnakeYAML turns unquoted `2026-10-14T03:12:40` / `2026-10-14` into java.util.Date (UTC).
     * We always mean *local* wall-clock time, so a Date is read back through UTC to undo that.
     */
    private fun scalarAsLocal(v: Any?): String? = when (v) {
        null -> null
        is java.util.Date -> java.time.Instant.ofEpochMilli(v.time).atZone(java.time.ZoneOffset.UTC).toLocalDateTime().format(isoDateTime)
        else -> v.toString()
    }

    private fun parseDateTime(v: Any?): Long? {
        val s = scalarAsLocal(v) ?: return null
        return try {
            LocalDateTime.parse(s.replace(' ', 'T'), isoDateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try { java.time.Instant.parse(s).toEpochMilli() } catch (_: Exception) { null }
        }
    }

    private fun parseDateKey(v: Any?): String? = scalarAsLocal(v)?.take(10)
}
