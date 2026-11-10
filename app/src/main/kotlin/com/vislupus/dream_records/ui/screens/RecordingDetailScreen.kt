package com.vislupus.dream_records.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.audio.AudioPlayer
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.transcription.Provider
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.AppTextField
import com.vislupus.dream_records.ui.components.ConfirmDialog
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.SectionHeader
import com.vislupus.dream_records.ui.components.TagChip
import com.vislupus.dream_records.ui.components.TagPickerSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One recording: audio player, model picker, editable transcript (auto-saved), tags.
 * The transcript stays editable while the audio plays, and works offline.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingDetailScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val item by remember(id) { vm.repo.observeRecording(id) }.collectAsStateWithLifecycle(initialValue = null)
    val allTags by remember { vm.repo.observeTags() }.collectAsStateWithLifecycle(initialValue = emptyList())

    val player = remember { AudioPlayer(scope) }
    val pState by player.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { player.release() } }

    // --- transcript editing with auto-save
    var text by remember { mutableStateOf<String?>(null) }
    var savedText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item?.recording?.id) {
        item?.let { if (text == null) { text = it.recording.transcript; savedText = it.recording.transcript } }
    }
    LaunchedEffect(text) {
        val t = text ?: return@LaunchedEffect
        if (t == savedText) return@LaunchedEffect
        delay(AppConfig.Player.AUTOSAVE_DEBOUNCE_MS)
        vm.repo.updateTranscript(id, t, "manual")
        savedText = t
    }

    // --- load audio once
    var audioLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(item?.recording?.fileName) {
        val f = item?.let { vm.repo.audioFile(it.recording) }
        if (f != null && !audioLoaded) { runCatching { player.load(f) }; audioLoaded = true }
    }

    // --- connectivity (re-checked every few seconds)
    var online by remember { mutableStateOf(vm.isOnline()) }
    LaunchedEffect(Unit) { while (true) { online = vm.isOnline(); delay(3000) } }

    var busy by remember { mutableStateOf<Provider?>(null) }
    var pendingProvider by remember { mutableStateOf<Provider?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val rec = item?.recording ?: return
    val audioFile = vm.repo.audioFile(rec)

    fun runTranscription(p: Provider) {
        val f = audioFile ?: return
        busy = p
        scope.launch {
            try {
                val (result, source) = vm.transcribe(p, f)
                text = result
                savedText = result
                vm.repo.updateTranscript(id, result, source)
                vm.toast(s.transcriptionDone)
            } catch (e: Exception) {
                vm.toast(s.transcriptionFailed(e.message ?: "?"))
            } finally { busy = null }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = AppConfig.Layout.screenPadding).padding(top = 8.dp, bottom = AppConfig.Layout.sectionSpacing),
    ) {
        // ---- Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { LucideIcon(Lucide.ChevronLeft, size = AppConfig.Layout.iconSizeLarge) }
            Column(Modifier.weight(1f)) {
                Text(TimeUtils.formatDateTime(rec.createdAt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${s.durationLabel}: ${TimeUtils.formatDuration(rec.durationMs)}" + if (rec.autoStopped) " · ${s.stoppedSilence}" else "",
                    style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted,
                )
            }
            IconButton(onClick = { showDelete = true }) { LucideIcon(Lucide.Trash, tint = AppConfig.Theme.onSurfaceMuted) }
        }

        // ---- Player
        if (audioFile == null) {
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(Lucide.Alert, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.warning)
                Spacer(Modifier.width(8.dp))
                Text(s.audioMissing, color = AppConfig.Theme.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Slider(
                    value = pState.positionMs.toFloat(),
                    onValueChange = { player.seekTo(it.toInt()) },
                    valueRange = 0f..(pState.durationMs.coerceAtLeast(1)).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = AppConfig.Theme.accent, activeTrackColor = AppConfig.Theme.accent,
                        inactiveTrackColor = AppConfig.Theme.surfaceBorder,
                    ),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(TimeUtils.formatDuration(pState.positionMs.toLong()), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { player.skip(-AppConfig.Player.SKIP_SECONDS) }) { LucideIcon(Lucide.RotateCcw, tint = AppConfig.Theme.onSurfaceMuted) }
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(AppConfig.Theme.accent).clickable { player.toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        LucideIcon(if (pState.isPlaying) Lucide.Pause else Lucide.Play, tint = AppConfig.Theme.onAccent, size = AppConfig.Layout.iconSizeLarge)
                    }
                    IconButton(onClick = { player.skip(AppConfig.Player.SKIP_SECONDS) }) {
                        LucideIcon(Lucide.RotateCcw, tint = AppConfig.Theme.onSurfaceMuted, modifier = Modifier.graphicsLayer { scaleX = -1f })
                    }
                    Spacer(Modifier.weight(1f))
                    Text(TimeUtils.formatDuration(pState.durationMs.toLong()), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                }
            }
        }

        // ---- Model picker
        SectionHeader(s.transcribeWith)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppConfig.Layout.itemSpacing)) {
            Provider.entries.forEach { p ->
                val hasKey = p.hasKey(settings)
                val enabled = online && hasKey && audioFile != null && busy == null
                val subtitle = when {
                    !online -> s.offline
                    !hasKey -> s.noApiKey
                    p == Provider.GROQ -> settings.groqModel
                    else -> when (p) { Provider.GEMINI -> AppConfig.Transcription.Gemini.MODEL; else -> AppConfig.Transcription.ElevenLabs.MODEL }
                }
                ProviderButton(
                    modifier = Modifier.weight(1f),
                    provider = p, subtitle = subtitle, enabled = enabled, busy = busy == p, online = online,
                    onClick = { if (text.isNullOrBlank()) runTranscription(p) else pendingProvider = p },
                )
            }
        }

        // ---- Transcript
        SectionHeader(s.transcript)
        AppTextField(
            value = text.orEmpty(),
            onValueChange = { text = it },
            placeholder = s.transcriptHint,
            singleLine = false,
            minLines = 8,
        )
        if (rec.transcriptSource.isNotBlank()) {
            Text(rec.transcriptSource, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint, modifier = Modifier.padding(top = 4.dp))
        }

        // ---- Tags
        SectionHeader(s.tags)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item?.tags?.forEach { t ->
                TagChip(t, trailing = {
                    LucideIcon(Lucide.X, size = 14.dp, tint = com.vislupus.dream_records.config.TagColors.at(t.colorIndex).text,
                        modifier = Modifier.clickable { scope.launch { vm.repo.removeTagFromRecording(id, t.id) } })
                })
            }
            Row(
                Modifier.clip(MaterialTheme.shapes.small).clickable { showTagPicker = true }.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(Lucide.Plus, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
                Spacer(Modifier.width(6.dp))
                Text(s.addTag, color = AppConfig.Theme.accent, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showTagPicker) {
        val attached = item?.tags?.map { it.id }?.toSet() ?: emptySet()
        TagPickerSheet(
            tags = allTags.filter { it.id !in attached },
            onPick = { t -> scope.launch { vm.repo.addTagToRecording(id, t.id) } },
            onCreate = { name, color -> scope.launch { val t = vm.repo.createTag(name, color); vm.repo.addTagToRecording(id, t.id) } },
            onDismiss = { showTagPicker = false },
        )
    }
    pendingProvider?.let { p ->
        ConfirmDialog(
            title = s.replaceTranscript, body = s.replaceTranscriptBody, confirmText = s.replace,
            onConfirm = { runTranscription(p) }, onDismiss = { pendingProvider = null },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = s.deleteRecording, body = s.deleteRecordingBody, confirmText = s.delete, destructive = true,
            onConfirm = { scope.launch { player.release(); vm.repo.deleteRecording(id); onBack() } },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ProviderButton(
    modifier: Modifier, provider: Provider, subtitle: String, enabled: Boolean, busy: Boolean, online: Boolean, onClick: () -> Unit,
) {
    val icon = when (provider) { Provider.GEMINI -> Lucide.Sparkles; Provider.GROQ -> Lucide.Zap; Provider.ELEVENLABS -> Lucide.AudioLines }
    val tint = if (enabled || busy) AppConfig.Theme.accent else AppConfig.Theme.onSurfaceFaint
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(AppConfig.Theme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(AppConfig.Layout.iconSizeLarge), contentAlignment = Alignment.Center) {
            when {
                busy -> LucideIcon(Lucide.Loader, size = AppConfig.Layout.iconSizeLarge, tint = tint, modifier = Modifier.rotate(angle))
                !online -> LucideIcon(Lucide.WifiOff, size = AppConfig.Layout.iconSizeLarge, tint = tint)
                else -> LucideIcon(icon, size = AppConfig.Layout.iconSizeLarge, tint = tint)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(provider.displayName, style = MaterialTheme.typography.labelMedium, color = if (enabled || busy) AppConfig.Theme.onSurface else AppConfig.Theme.onSurfaceFaint,
            maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint, maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
