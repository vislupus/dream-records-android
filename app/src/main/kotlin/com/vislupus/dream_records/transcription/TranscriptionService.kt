package com.vislupus.dream_records.transcription

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.data.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** The three speech-to-text providers the user can choose from. */
enum class Provider(val id: String, val displayName: String) {
    GEMINI(AppConfig.Transcription.Gemini.ID, AppConfig.Transcription.Gemini.DISPLAY_NAME),
    GROQ(AppConfig.Transcription.Groq.ID, AppConfig.Transcription.Groq.DISPLAY_NAME),
    ELEVENLABS(AppConfig.Transcription.ElevenLabs.ID, AppConfig.Transcription.ElevenLabs.DISPLAY_NAME);

    fun hasKey(s: Settings): Boolean = when (this) {
        GEMINI -> s.geminiApiKey.isNotBlank()
        GROQ -> s.groqApiKey.isNotBlank()
        ELEVENLABS -> s.elevenLabsApiKey.isNotBlank()
    }
}

class TranscriptionException(message: String) : Exception(message)

/**
 * Sends an audio file to the chosen provider and returns the transcript.
 * All network calls run on Dispatchers.IO. Keys come from Settings.
 */
class TranscriptionService(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(AppConfig.Transcription.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.Transcription.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** @return Pair(transcript, sourceLabel) */
    suspend fun transcribe(provider: Provider, file: File, settings: Settings): Pair<String, String> =
        withContext(Dispatchers.IO) {
            if (!file.exists()) throw TranscriptionException("audio file missing")
            if (file.length() > AppConfig.Transcription.MAX_UPLOAD_BYTES) throw TranscriptionException("file too large")
            val result = when (provider) {
                Provider.GEMINI -> gemini(file, settings) to AppConfig.Transcription.Gemini.MODEL
                Provider.GROQ -> groq(file, settings) to "groq:${settings.groqModel}"
                Provider.ELEVENLABS -> elevenLabs(file, settings) to AppConfig.Transcription.ElevenLabs.MODEL
            }
            // Never report success with nothing in it — and never wipe the existing transcript with "".
            if (result.first.isBlank()) throw TranscriptionException("the model returned an empty transcript")
            result
        }

    // ------------------------------------------------------------- Gemini
    private fun gemini(file: File, s: Settings): String {
        val cfg = AppConfig.Transcription.Gemini
        val audioB64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val langHint = if (s.spokenLanguage.isNotBlank()) " The language spoken is '${s.spokenLanguage}'." else ""
        val body = JSONObject().put(
            "contents", JSONArray().put(
                JSONObject().put(
                    "parts", JSONArray()
                        .put(JSONObject().put("text", AppConfig.Transcription.PROMPT + langHint + " Return only the transcript."))
                        .put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject().put("mime_type", AppConfig.Recording.MIME_TYPE).put("data", audioB64),
                            )
                        )
                )
            )
        )
        val req = Request.Builder()
            .url("${cfg.ENDPOINT}${cfg.MODEL}:generateContent")
            .addHeader("x-goog-api-key", s.geminiApiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val json = execute(req)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw TranscriptionException("no candidates (${json.optJSONObject("promptFeedback")?.optString("blockReason") ?: "empty response"})")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            ?: throw TranscriptionException("no content (finishReason: ${candidate.optString("finishReason", "?")})")
        return parseGeminiParts(parts)
    }

    /**
     * Dedicated transcription models (gemini-3.5-transcribe) put the transcript in an
     * `audioTranscription` part next to an empty `text` part:
     *   parts: [ {"text": ""}, {"audioTranscription": {"text": "..."}} ]
     * General models (e.g. gemini-3.5-flash-lite) return it as plain `text`.
     * Thinking parts (`"thought": true`) are skipped.
     */
    private fun parseGeminiParts(parts: JSONArray): String {
        val transcription = StringBuilder()
        val plain = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            val at = p.optJSONObject("audioTranscription") ?: p.optJSONObject("audio_transcription")
            if (at != null) transcription.append(at.optString("text"))
            else if (!p.optBoolean("thought", false)) plain.append(p.optString("text"))
        }
        return transcription.toString().trim().ifBlank { plain.toString().trim() }
    }

    // --------------------------------------------------------------- Groq
    private fun groq(file: File, s: Settings): String {
        val cfg = AppConfig.Transcription.Groq
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(AppConfig.Recording.MIME_TYPE.toMediaType()))
            .addFormDataPart("model", s.groqModel.ifBlank { cfg.DEFAULT_MODEL })
            .addFormDataPart("response_format", "json")
            .addFormDataPart("temperature", "0")
            .addFormDataPart("prompt", AppConfig.Transcription.PROMPT)
        if (s.spokenLanguage.isNotBlank()) form.addFormDataPart("language", s.spokenLanguage)
        val req = Request.Builder()
            .url(cfg.ENDPOINT)
            .addHeader("Authorization", "Bearer ${s.groqApiKey}")
            .post(form.build())
            .build()
        return execute(req).optString("text").trim()
    }

    // ---------------------------------------------------------- ElevenLabs
    private fun elevenLabs(file: File, s: Settings): String {
        val cfg = AppConfig.Transcription.ElevenLabs
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(AppConfig.Recording.MIME_TYPE.toMediaType()))
            .addFormDataPart("model_id", cfg.MODEL)
            .addFormDataPart("tag_audio_events", "false")
            .addFormDataPart("diarize", "false")
        if (s.spokenLanguage.isNotBlank()) form.addFormDataPart("language_code", s.spokenLanguage)
        val req = Request.Builder()
            .url(cfg.ENDPOINT)
            .addHeader("xi-api-key", s.elevenLabsApiKey)
            .post(form.build())
            .build()
        return execute(req).optString("text").trim()
    }

    private fun execute(req: Request): JSONObject {
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = try { JSONObject(text).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                throw TranscriptionException("HTTP ${resp.code}${if (!msg.isNullOrBlank()) ": $msg" else ""}")
            }
            return try { JSONObject(text) } catch (e: Exception) { throw TranscriptionException("bad JSON response") }
        }
    }
}
