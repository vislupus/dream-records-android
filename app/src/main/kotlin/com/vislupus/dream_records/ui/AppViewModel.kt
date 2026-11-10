package com.vislupus.dream_records.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vislupus.dream_records.DreamRecordsApp
import com.vislupus.dream_records.data.settings.Settings
import com.vislupus.dream_records.io.ExportImport
import com.vislupus.dream_records.transcription.Provider
import com.vislupus.dream_records.transcription.TranscriptionService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** One app-wide ViewModel: settings, data access and the services screens need. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as DreamRecordsApp
    val repo = container.repository
    val settingsStore = container.settingsStore
    val transcription: TranscriptionService = container.transcription
    val exportImport = ExportImport(app, repo)

    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** One-shot messages shown in the snackbar. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    fun toast(msg: String) { messages.tryEmit(msg) }

    // -------------------------------------------------------- Transcription
    suspend fun transcribe(provider: Provider, file: File): Pair<String, String> =
        transcription.transcribe(provider, file, settings.value)

    fun isOnline() = transcription.isOnline()

    // -------------------------------------------------------- Export/Import
    /** True while an export or import runs; the Settings buttons are disabled meanwhile. */
    val dataBusy = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun export(onDone: (File) -> Unit, onError: (String) -> Unit) = runDataJob(onError) { onDone(exportImport.exportZip()) }

    fun import(uri: Uri, onDone: (ExportImport.ImportResult) -> Unit, onError: (String) -> Unit) =
        runDataJob(onError) { onDone(exportImport.importFrom(uri)) }

    private fun runDataJob(onError: (String) -> Unit, block: suspend () -> Unit) {
        if (dataBusy.value) return
        dataBusy.value = true
        viewModelScope.launch {
            try { block() }
            catch (e: OutOfMemoryError) { onError("out of memory") }   // never crash the app on a huge file
            catch (e: Exception) { onError(e.message ?: e.javaClass.simpleName) }
            finally { dataBusy.value = false }
        }
    }

    fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
