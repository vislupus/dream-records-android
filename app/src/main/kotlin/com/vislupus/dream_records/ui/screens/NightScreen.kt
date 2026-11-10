package com.vislupus.dream_records.ui.screens

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.AppTextField
import com.vislupus.dream_records.ui.components.ConfirmDialog
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import com.vislupus.dream_records.data.db.Night
import kotlinx.coroutines.launch

/** Marker for "no Night row in the DB yet". */
private val NO_NIGHT = Night("", "", 0L)

/** One night: the combined text (editable, auto-saved) and the separate recordings. */
@Composable
fun NightScreen(vm: AppViewModel, nightDate: String, onBack: () -> Unit, onOpenRecording: (String) -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val recs by remember(nightDate) { vm.repo.observeNight(nightDate) }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Sentinel: the Night row may legitimately not exist yet, so we wait for the first real DB emission.
    val nightFlow = remember(nightDate) { vm.repo.observeNightEntry(nightDate).map { it ?: NO_NIGHT } }
    val nightState by nightFlow.collectAsStateWithLifecycle(initialValue = null)
    var text by remember { mutableStateOf<String?>(null) }
    var savedText by remember { mutableStateOf("") }
    LaunchedEffect(nightState) {
        val n = nightState ?: return@LaunchedEffect
        if (text == null) { text = if (n === NO_NIGHT) "" else n.combinedText; savedText = text!! }
    }
    LaunchedEffect(text) {
        val t = text ?: return@LaunchedEffect
        if (t == savedText) return@LaunchedEffect
        delay(AppConfig.Player.AUTOSAVE_DEBOUNCE_MS)
        vm.repo.saveNightText(nightDate, t); savedText = t
    }
    var confirmCombine by remember { mutableStateOf(false) }
    fun combine() = scope.launch { val t = vm.repo.combineNight(nightDate); text = t; savedText = t }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = AppConfig.Layout.screenPadding).padding(top = 8.dp, bottom = AppConfig.Layout.sectionSpacing),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { LucideIcon(Lucide.ChevronLeft, size = AppConfig.Layout.iconSizeLarge) }
            Column(Modifier.weight(1f)) {
                Text(s.nightOf(TimeUtils.formatDate(TimeUtils.parseNightDate(nightDate))), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(s.recordingsCount(recs.size), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
            }
        }

        SectionHeader(s.combinedText)
        Button(
            onClick = { if (text.isNullOrBlank()) combine() else confirmCombine = true },
            colors = ButtonDefaults.buttonColors(containerColor = AppConfig.Theme.surfaceElevated, contentColor = AppConfig.Theme.accent),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LucideIcon(Lucide.Combine, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
            Spacer(Modifier.width(8.dp))
            Text(s.combineNow)
        }
        Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
        AppTextField(value = text.orEmpty(), onValueChange = { text = it }, placeholder = s.combinedTextHint, singleLine = false, minLines = 6)

        SectionHeader(s.partsOfNight)
        Column(verticalArrangement = Arrangement.spacedBy(AppConfig.Layout.itemSpacing)) {
            recs.forEach { r -> RecordingRow(r, onClick = { onOpenRecording(r.recording.id) }) }
        }
    }

    if (confirmCombine) {
        ConfirmDialog(
            title = s.combineConfirm, body = s.combineConfirmBody, confirmText = s.combineNight,
            onConfirm = { combine() }, onDismiss = { confirmCombine = false },
        )
    }
}
