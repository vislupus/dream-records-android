package com.vislupus.dream_records.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.BuildConfig
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.config.Strings
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.AppCard
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.ScreenTitle
import com.vislupus.dream_records.ui.components.SectionHeader
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import kotlinx.coroutines.delay

/** Language, auto-stop timeout, API keys, export/import. Last tab in the bottom menu. */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dataBusy by vm.dataBusy.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri, onDone = { vm.toast(s.importDone(it.recordings, it.tags, it.audioRestored)) }, onError = { vm.toast(s.importFailed(it)) })
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = AppConfig.Layout.screenPadding).padding(top = AppConfig.Layout.screenPadding, bottom = AppConfig.Layout.sectionSpacing),
    ) {
        ScreenTitle(s.settingsTitle)

        // ---- Language
        SettingRow(icon = Lucide.Languages, title = s.language) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Strings.ALL.forEach { lang ->
                    Choice(lang.languageName, selected = settings.languageCode == lang.languageCode) { vm.launch { vm.settingsStore.setLanguage(lang.languageCode) } }
                }
            }
        }

        // ---- Recording
        SectionHeader(s.sectionRecording)
        SettingRow(icon = Lucide.Clock, title = s.silenceTimeout, subtitle = s.silenceTimeoutHint) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppConfig.Recording.SILENCE_TIMEOUT_OPTIONS_MIN.forEach { m ->
                    Choice("$m", selected = settings.silenceTimeoutMin == m) { vm.launch { vm.settingsStore.setSilenceTimeout(m) } }
                }
            }
            Text(s.minutesShort, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint, modifier = Modifier.padding(top = 4.dp))
        }

        // ---- Transcription
        SectionHeader(s.sectionTranscription)
        SettingRow(icon = Lucide.Sparkles, title = s.geminiKey) {
            KeyField(settings.geminiApiKey) { vm.launch { vm.settingsStore.setGeminiKey(it) } }
        }
        SettingRow(icon = Lucide.Zap, title = s.groqKey) {
            KeyField(settings.groqApiKey) { vm.launch { vm.settingsStore.setGroqKey(it) } }
            Spacer(Modifier.height(8.dp))
            Text(s.groqModel, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppConfig.Transcription.Groq.MODELS.forEach { m ->
                    Choice(m, selected = settings.groqModel == m) { vm.launch { vm.settingsStore.setGroqModel(m) } }
                }
            }
        }
        SettingRow(icon = Lucide.AudioLines, title = s.elevenLabsKey) {
            KeyField(settings.elevenLabsApiKey) { vm.launch { vm.settingsStore.setElevenLabsKey(it) } }
        }
        SettingRow(icon = Lucide.Languages, title = s.transcriptionLanguage, subtitle = s.transcriptionLanguageHint) {
            KeyField(settings.spokenLanguage, secret = false, placeholder = "en") { vm.launch { vm.settingsStore.setSpokenLanguage(it) } }
        }
        Text(s.apiKeyHint, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint, modifier = Modifier.padding(top = 6.dp))

        // ---- Data
        SectionHeader(s.sectionData)
        AppCard {
            Column {
                ActionButton(Lucide.Download, if (dataBusy) s.working else s.exportData, s.exportHint, enabled = !dataBusy) {
                    vm.export(
                        onDone = { file ->
                            vm.toast(s.exportDone(file.name))
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(Intent.createChooser(send, s.share))
                        },
                        onError = { vm.toast("${s.error}: $it") },
                    )
                }
                Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
                ActionButton(Lucide.Upload, if (dataBusy) s.working else s.importData, s.importHint, enabled = !dataBusy) {
                    importLauncher.launch(arrayOf("application/zip", "application/x-yaml", "text/yaml", "text/plain", "application/octet-stream", "*/*"))
                }
            }
        }

        // ---- About
        SectionHeader(s.sectionAbout)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.FolderLock, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
            Spacer(Modifier.width(8.dp))
            Text(s.storageLocation, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text("${s.version} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint)
    }
}

@Composable
private fun SettingRow(icon: Int, title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    AppCard(Modifier.padding(bottom = AppConfig.Layout.itemSpacing)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(icon, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) AppConfig.Theme.onAccent else AppConfig.Theme.onSurfaceMuted,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) AppConfig.Theme.accent else AppConfig.Theme.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/** Text field that commits to settings a moment after you stop typing. */
@Composable
private fun KeyField(value: String, secret: Boolean = true, placeholder: String = "", onCommit: (String) -> Unit) {
    var local by remember(value) { mutableStateOf(value) }
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(local) { if (local != value) { delay(AppConfig.Player.AUTOSAVE_DEBOUNCE_MS); onCommit(local) } }
    OutlinedTextField(
        value = local,
        onValueChange = { local = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = AppConfig.Theme.onSurfaceFaint) },
        visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (secret) {
            { LucideIcon(Lucide.Key, size = AppConfig.Layout.iconSizeSmall, tint = if (reveal) AppConfig.Theme.accent else AppConfig.Theme.onSurfaceFaint, modifier = Modifier.clickable { reveal = !reveal }) }
        } else null,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppConfig.Theme.accent, unfocusedBorderColor = AppConfig.Theme.surfaceBorder,
            focusedContainerColor = AppConfig.Theme.surfaceElevated, unfocusedContainerColor = AppConfig.Theme.surfaceElevated,
            cursorColor = AppConfig.Theme.accent,
        ),
    )
}

@Composable
private fun ActionButton(icon: Int, title: String, hint: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppConfig.Theme.surfaceElevated, contentColor = AppConfig.Theme.accent,
                disabledContainerColor = AppConfig.Theme.surfaceElevated, disabledContentColor = AppConfig.Theme.onSurfaceMuted,
            ),
        ) {
            LucideIcon(if (enabled) icon else Lucide.Loader, size = AppConfig.Layout.iconSizeSmall, tint = if (enabled) AppConfig.Theme.accent else AppConfig.Theme.onSurfaceMuted)
            Spacer(Modifier.width(8.dp))
            Text(title)
        }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint, modifier = Modifier.padding(top = 4.dp))
    }
}
