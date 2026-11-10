package com.vislupus.dream_records.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.data.TimeUtils
import com.vislupus.dream_records.data.db.RecordingWithTags
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.AppCard
import com.vislupus.dream_records.ui.components.AppTextField
import com.vislupus.dream_records.ui.components.EmptyState
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.ScreenTitle
import com.vislupus.dream_records.ui.components.TagChip

/** All recordings, grouped by night, searchable by text and filterable by tag. */
@Composable
fun LibraryScreen(vm: AppViewModel, onOpenRecording: (String) -> Unit, onOpenNight: (String) -> Unit) {
    val s = LocalStrings.current
    val all by remember { vm.repo.observeRecordings() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val tags by remember { vm.repo.observeTags() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val nightsWithText by remember { vm.repo.observeNightsWithText() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Nights whose combined text matches the query are shown too.
    val matchingNightTexts = remember(query, nightsWithText) {
        if (query.isBlank()) emptySet() else nightsWithText.filter { it.combinedText.contains(query, ignoreCase = true) }.map { it.nightDate }.toSet()
    }
    val filtered = remember(all, query, tagFilter, matchingNightTexts) {
        all.filter { r ->
            (tagFilter == null || r.tags.any { it.id == tagFilter }) &&
                (query.isBlank() || r.recording.transcript.contains(query, ignoreCase = true) || r.recording.nightDate in matchingNightTexts)
        }
    }
    val groups = remember(filtered) { filtered.groupBy { it.recording.nightDate }.toSortedMap(compareByDescending { it }) }

    Column(Modifier.fillMaxSize().padding(horizontal = AppConfig.Layout.screenPadding).padding(top = AppConfig.Layout.screenPadding)) {
        ScreenTitle(s.libraryTitle)
        AppTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = s.searchHint,
            leading = { LucideIcon(Lucide.Search, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceMuted) },
            trailing = if (query.isNotEmpty()) {
                { LucideIcon(Lucide.X, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceMuted, modifier = Modifier.clickable { query = "" }) }
            } else null,
        )
        if (tags.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = AppConfig.Layout.itemSpacing).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { t ->
                    TagChip(t, small = true, selected = tagFilter == t.id, onClick = { tagFilter = if (tagFilter == t.id) null else t.id })
                }
            }
        } else Spacer(Modifier.height(AppConfig.Layout.itemSpacing))

        if (all.isEmpty()) { EmptyState(s.noRecordings); return }
        if (filtered.isEmpty()) { EmptyState(s.noResults); return }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppConfig.Layout.itemSpacing)) {
            groups.forEach { (night, recs) ->
                item(key = "h_$night") {
                    NightHeader(night, recs.size, hasCombined = nightsWithText.any { it.nightDate == night }, onClick = { onOpenNight(night) })
                }
                items(recs, key = { it.recording.id }) { r -> RecordingRow(r, onClick = { onOpenRecording(r.recording.id) }) }
            }
            item { Spacer(Modifier.height(AppConfig.Layout.sectionSpacing)) }
        }
    }
}

@Composable
private fun NightHeader(night: String, count: Int, hasCombined: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.Moon, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
        Spacer(Modifier.width(8.dp))
        Text(
            s.nightOf(TimeUtils.formatDate(TimeUtils.parseNightDate(night))),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (hasCombined) {
            LucideIcon(Lucide.Combine, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceMuted)
            Spacer(Modifier.width(8.dp))
        }
        Text(s.recordingsCount(count), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
        Spacer(Modifier.width(4.dp))
        LucideIcon(Lucide.ChevronRight, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
    }
}

@Composable
fun RecordingRow(r: RecordingWithTags, onClick: () -> Unit) {
    val s = LocalStrings.current
    val rec = r.recording
    AppCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(TimeUtils.formatTime(rec.createdAt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Text(TimeUtils.formatDuration(rec.durationMs), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                Spacer(Modifier.weight(1f))
                if (rec.fileName.isBlank()) LucideIcon(Lucide.Alert, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.warning)
                else if (rec.transcript.isNotBlank()) LucideIcon(Lucide.FileText, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                rec.transcript.ifBlank { s.untranscribed },
                style = MaterialTheme.typography.bodyMedium,
                color = if (rec.transcript.isBlank()) AppConfig.Theme.onSurfaceFaint else AppConfig.Theme.onSurfaceMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (r.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.tags.forEach { TagChip(it, small = true) }
                }
            }
        }
    }
}
