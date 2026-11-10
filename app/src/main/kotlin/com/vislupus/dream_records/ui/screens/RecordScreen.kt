package com.vislupus.dream_records.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.MainActivity
import com.vislupus.dream_records.audio.RecorderBus
import com.vislupus.dream_records.audio.RecorderService
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon

/**
 * The home screen: one big microphone. Tap = start, tap again = stop.
 * While recording the button turns red, shows a stop square and a ring that
 * pulses with the microphone level. Everything else stays dark and quiet.
 */
@Composable
fun RecordScreen(vm: AppViewModel, onOpenRecording: (String) -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val rec by RecorderBus.state.collectAsStateWithLifecycle()
    val latest by remember { vm.repo.observeLatest() }.collectAsStateWithLifecycle(initialValue = null)

    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Ask for the notification permission once (Android 13+), needed for the foreground-service notification.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Optional keep-screen-on while recording (off by default, see AppConfig).
    DisposableEffect(rec.isRecording) {
        val act = context as? MainActivity
        if (AppConfig.Recording.KEEP_SCREEN_ON_WHILE_RECORDING) act?.setKeepScreenOn(rec.isRecording)
        onDispose { act?.setKeepScreenOn(false) }
    }

    // Show the one-shot result message then clear it.
    LaunchedEffect(rec.lastEvent) {
        when (rec.lastEvent) {
            "saved" -> vm.toast(s.saved)
            "auto" -> vm.toast(s.stoppedSilence)
            "short" -> vm.toast(s.discardedTooShort)
        }
        if (rec.lastEvent != null) RecorderBus.clearEvent()
    }

    fun toggle() {
        if (rec.isRecording) RecorderService.stop(context)
        else if (!hasMic) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        else RecorderService.start(context, settings.silenceTimeoutMin, settings.languageCode)
    }

    Column(
        Modifier.fillMaxSize().padding(AppConfig.Layout.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ---- Timer (only while recording)
        // Fixed-height slot so the mic button doesn't jump when the timer appears.
        Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
            if (rec.isRecording) {
                Text(
                    TimeUtils.formatDuration(rec.elapsedMs),
                    fontSize = AppConfig.Typography.timerSize,
                    fontWeight = FontWeight.Light,
                    color = AppConfig.Theme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(AppConfig.Layout.sectionSpacing))

        MicButton(isRecording = rec.isRecording, level = rec.level, onClick = ::toggle)

        Spacer(Modifier.height(AppConfig.Layout.sectionSpacing))
        Text(
            if (rec.isRecording) s.tapToStop else if (hasMic) s.tapToRecord else s.micPermissionNeeded,
            color = AppConfig.Theme.onSurfaceMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        if (!hasMic && !rec.isRecording) {
            TextButton(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text(s.grantPermission) }
        } else {
            Text(
                s.autoStopAfter.format(settings.silenceTimeoutMin),
                color = AppConfig.Theme.onSurfaceFaint,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // ---- Last recording shortcut (hidden while recording)
        Spacer(Modifier.height(AppConfig.Layout.sectionSpacing * 2))
        val last = latest
        AnimatedVisibility(!rec.isRecording && last != null) {
            if (last != null) {
                Row(
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onOpenRecording(last.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(Lucide.FileText, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceMuted)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${s.lastRecording}: ${TimeUtils.formatDateTime(last.createdAt)} · ${TimeUtils.formatDuration(last.durationMs)}",
                        color = AppConfig.Theme.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MicButton(isRecording: Boolean, level: Float, onClick: () -> Unit) {
    val size = AppConfig.Layout.micButtonSize
    val color = if (isRecording) AppConfig.Theme.recording else AppConfig.Theme.accent
    val smoothLevel by animateFloatAsState(level, animationSpec = tween(180), label = "level")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val breath by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "breath",
    )

    Box(Modifier.size(size + 48.dp), contentAlignment = Alignment.Center) {
        // Outer ring reacts to the mic level while recording, breathes softly when idle.
        Canvas(Modifier.fillMaxSize()) {
            val base = size.toPx() / 2f
            val extra = if (isRecording) 24.dp.toPx() * smoothLevel + 4.dp.toPx() else 6.dp.toPx() * breath
            drawCircle(
                color = color.copy(alpha = if (isRecording) 0.35f else 0.18f),
                radius = base + extra,
                style = Stroke(width = AppConfig.Layout.micRingWidth.toPx()),
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isRecording) 0.18f else 0.12f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(
                icon = if (isRecording) Lucide.Square else Lucide.Mic,
                size = AppConfig.Layout.micIconSize,
                tint = color,
            )
        }
    }
}
