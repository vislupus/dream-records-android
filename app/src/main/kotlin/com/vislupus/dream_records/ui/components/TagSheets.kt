package com.vislupus.dream_records.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.config.TagColors
import com.vislupus.dream_records.data.db.Tag

/** Bottom sheet: pick an existing tag or create a new one and attach it right away. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerSheet(
    tags: List<Tag>,
    onPick: (Tag) -> Unit,
    onCreate: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = AppConfig.Theme.surfaceElevated) {
        Column(Modifier.padding(horizontal = AppConfig.Layout.screenPadding).padding(bottom = AppConfig.Layout.sectionSpacing).navigationBarsPadding().imePadding()) {
            if (creating) {
                TagEditorForm(
                    title = s.newTag, initialName = "", initialColor = AppConfig.Tags.DEFAULT_COLOR_INDEX,
                    onSave = { n, c -> onCreate(n, c); onDismiss() },
                    onCancel = { creating = false },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.addTag, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { creating = true }) {
                        LucideIcon(Lucide.Plus, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
                        Spacer(Modifier.width(6.dp))
                        Text(s.newTag, color = AppConfig.Theme.accent)
                    }
                }
                Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
                if (tags.isEmpty()) {
                    Text(s.noTagsYet, color = AppConfig.Theme.onSurfaceMuted)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { t -> TagChip(t, onClick = { onPick(t); onDismiss() }) }
                    }
                }
            }
        }
    }
}

/** Name + 3x10 colour grid, with live preview. Shared by the picker and the Tags screen. */
@Composable
fun TagEditorForm(
    title: String,
    initialName: String,
    initialColor: Int,
    onSave: (name: String, colorIndex: Int) -> Unit,
    onCancel: () -> Unit,
    errorText: String? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }

    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
        AppTextField(value = name, onValueChange = { if (it.length <= AppConfig.Tags.MAX_NAME_LENGTH) name = it }, label = s.tagName)
        errorText?.let { Text(it, color = AppConfig.Theme.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        SectionHeader(s.tagColor)
        TagColorGrid(selected = color, onSelect = { color = it })
        Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
        // Live preview
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagChip(Tag("preview", name.ifBlank { s.tagName }, color, null, 0L))
        }
        extra?.let { Spacer(Modifier.height(AppConfig.Layout.itemSpacing)); it() }
        Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(s.cancel, color = AppConfig.Theme.onSurfaceMuted) }
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), color) }, enabled = name.isNotBlank()) {
                Text(s.save, color = if (name.isNotBlank()) AppConfig.Theme.accent else AppConfig.Theme.onSurfaceFaint)
            }
        }
    }
}
