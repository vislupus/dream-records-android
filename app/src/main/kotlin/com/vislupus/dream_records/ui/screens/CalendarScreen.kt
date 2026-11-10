package com.vislupus.dream_records.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.config.TagColors
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.ScreenTitle
import com.vislupus.dream_records.ui.components.TagChip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Month calendar. Week starts on Monday, Saturday/Sunday are tinted, nights with
 * recordings are highlighted, and built-in tags show as coloured dots under the day.
 */
@Composable
fun CalendarScreen(vm: AppViewModel, onOpenRecording: (String) -> Unit, onOpenNight: (String) -> Unit) {
    val s = LocalStrings.current
    val nightDates by remember { vm.repo.observeNightDates() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val dots by remember { vm.repo.observeCalendarDots() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val tags by remember { vm.repo.observeTags() }.collectAsStateWithLifecycle(initialValue = emptyList())

    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val dotsByDate = remember(dots) { dots.groupBy { it.nightDate } }
    val recordedSet = remember(nightDates) { nightDates.toSet() }
    val locale = remember(s.languageCode) { Locale(s.languageCode) }

    val selectedFlow = remember(selected) {
        selected?.let { vm.repo.observeNight(TimeUtils.nightKey(it)) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val selectedRecs by selectedFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = AppConfig.Layout.screenPadding).padding(top = AppConfig.Layout.screenPadding)) {
        ScreenTitle(s.calendarTitle) {
            TextButton(onClick = { month = YearMonth.from(today); selected = today }) { Text(s.today, color = AppConfig.Theme.accent) }
        }

        // ---- Month switcher
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { LucideIcon(Lucide.ChevronLeft) }
            Text(
                TimeUtils.formatMonth(month.atDay(1), locale),
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }) { LucideIcon(Lucide.ChevronRight) }
        }

        // ---- Day-of-week header (Monday first)
        Row(Modifier.fillMaxWidth()) {
            s.dayNames.forEachIndexed { i, name ->
                val weekend = (i + 1) in AppConfig.Calendar.WEEKEND_DAYS
                Text(
                    name, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (weekend) AppConfig.Theme.weekendText else AppConfig.Theme.onSurfaceMuted,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // ---- Grid
        val first = month.atDay(1)
        val offset = (first.dayOfWeek.value - AppConfig.Calendar.FIRST_DAY_OF_WEEK + 7) % 7
        val days = month.lengthOfMonth()
        val cells = (0 until offset).map { null } + (1..days).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date -> Box(Modifier.weight(1f)) { if (date != null) DayCell(date, today, selected, recordedSet, dotsByDate[TimeUtils.nightKey(date)].orEmpty().map { it.colorIndex }) { selected = if (selected == date) null else date } } }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // ---- Legend (built-in tags)
        Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            tags.filter { it.protectedKey != null }.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(AppConfig.Layout.calendarDotSize + 2.dp).clip(CircleShape).background(TagColors.at(t.colorIndex).background))
                    Spacer(Modifier.width(4.dp))
                    Text(t.name, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                }
            }
        }

        // ---- Selected day
        selected?.let { d ->
            Spacer(Modifier.height(AppConfig.Layout.sectionSpacing))
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(enabled = selectedRecs.isNotEmpty()) { onOpenNight(TimeUtils.nightKey(d)) }.padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(Lucide.Moon, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
                Spacer(Modifier.width(8.dp))
                Text(s.nightOf(TimeUtils.formatDate(d)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (selectedRecs.isNotEmpty()) LucideIcon(Lucide.ChevronRight, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
            }
            Spacer(Modifier.height(8.dp))
            if (selectedRecs.isEmpty()) {
                Text(s.noRecordingsThisDay, color = AppConfig.Theme.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppConfig.Layout.itemSpacing)) {
                    selectedRecs.forEach { r -> RecordingRow(r, onClick = { onOpenRecording(r.recording.id) }) }
                }
            }
        }
        Spacer(Modifier.height(AppConfig.Layout.sectionSpacing))
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, selected: LocalDate?, recorded: Set<String>, dotColors: List<Int>, onClick: () -> Unit) {
    val key = TimeUtils.nightKey(date)
    val hasRec = key in recorded
    val isToday = date == today
    val isSel = date == selected
    val weekend = date.dayOfWeek.value in AppConfig.Calendar.WEEKEND_DAYS
    Column(
        Modifier.fillMaxWidth().height(AppConfig.Layout.calendarDaySize + 10.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(AppConfig.Layout.calendarDaySize - 10.dp)
                .clip(CircleShape)
                .background(if (isSel) AppConfig.Theme.accent else if (hasRec) AppConfig.Theme.surfaceElevated else androidx.compose.ui.graphics.Color.Transparent)
                .border(if (isToday) 1.5.dp else 0.dp, if (isToday) AppConfig.Theme.todayRing else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasRec || isSel) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSel -> AppConfig.Theme.onAccent
                    hasRec -> AppConfig.Theme.onSurface
                    weekend -> AppConfig.Theme.weekendText.copy(alpha = 0.8f)
                    else -> AppConfig.Theme.onSurfaceMuted
                },
            )
        }
        Row(Modifier.height(AppConfig.Layout.calendarDotSize + 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            dotColors.distinct().take(AppConfig.Layout.CALENDAR_MAX_DOTS_PER_DAY).forEach { c ->
                Box(Modifier.size(AppConfig.Layout.calendarDotSize).clip(CircleShape).background(TagColors.at(c).background))
            }
        }
    }
}

@Suppress("unused")
private fun DayOfWeek.isWeekend() = value in AppConfig.Calendar.WEEKEND_DAYS
