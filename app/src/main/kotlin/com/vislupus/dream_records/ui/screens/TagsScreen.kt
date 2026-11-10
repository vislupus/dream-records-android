package com.vislupus.dream_records.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.data.db.Tag
import com.vislupus.dream_records.data.db.TagWithCount
import com.vislupus.dream_records.ui.AppViewModel
import com.vislupus.dream_records.ui.components.AppCard
import com.vislupus.dream_records.ui.components.ConfirmDialog
import com.vislupus.dream_records.ui.components.Lucide
import com.vislupus.dream_records.ui.components.LucideIcon
import com.vislupus.dream_records.ui.components.ScreenTitle
import com.vislupus.dream_records.ui.components.TagChip
import com.vislupus.dream_records.ui.components.TagEditorForm
import kotlinx.coroutines.launch

/** Manage tags: create, rename/recolour, merge, delete. Built-in tags cannot be deleted. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagsScreen(vm: AppViewModel) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val tags by remember { vm.repo.observeTagsWithCount() }.collectAsStateWithLifecycle(initialValue = emptyList())

    var editing by remember { mutableStateOf<TagWithCount?>(null) }
    var creating by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf<TagWithCount?>(null) }
    var mergeTarget by remember { mutableStateOf<Tag?>(null) }
    var deleting by remember { mutableStateOf<TagWithCount?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = AppConfig.Layout.screenPadding).padding(top = AppConfig.Layout.screenPadding)) {
        ScreenTitle(s.tagsTitle) {
            IconButton(onClick = { nameError = null; creating = true }) { LucideIcon(Lucide.Plus, tint = AppConfig.Theme.accent, size = AppConfig.Layout.iconSizeLarge) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppConfig.Layout.itemSpacing)) {
            items(tags, key = { it.id }) { t ->
                AppCard(onClick = { nameError = null; editing = t }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TagChip(t.toTag())
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.usedIn(t.count), style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                            if (t.protectedKey != null) {
                                Text(s.protectedTag, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceFaint)
                            }
                        }
                        if (t.protectedKey != null) LucideIcon(Lucide.CircleDot, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
                        else LucideIcon(Lucide.Pencil, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.onSurfaceFaint)
                    }
                }
            }
            item { Spacer(Modifier.height(AppConfig.Layout.sectionSpacing)) }
        }
    }

    // ---- Create
    if (creating) {
        EditorSheet(onDismiss = { creating = false }) {
            TagEditorForm(
                title = s.createTag, initialName = "", initialColor = AppConfig.Tags.DEFAULT_COLOR_INDEX, errorText = nameError,
                onSave = { n, c ->
                    scope.launch {
                        if (vm.repo.tagNameExists(n, null)) nameError = s.tagExists
                        else { vm.repo.createTag(n, c); creating = false }
                    }
                },
                onCancel = { creating = false },
            )
        }
    }

    // ---- Edit / merge / delete
    editing?.let { t ->
        EditorSheet(onDismiss = { editing = null }) {
            TagEditorForm(
                title = s.editTag, initialName = t.name, initialColor = t.colorIndex, errorText = nameError,
                onSave = { n, c ->
                    scope.launch {
                        if (vm.repo.tagNameExists(n, t.id)) nameError = s.tagExists
                        else { vm.repo.updateTag(t.toTag().copy(name = n, colorIndex = c)); editing = null }
                    }
                },
                onCancel = { editing = null },
                extra = {
                    Column {
                        if (t.protectedKey != null) {
                            Text(s.protectedTagHint, style = MaterialTheme.typography.bodySmall, color = AppConfig.Theme.onSurfaceMuted)
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { merging = t; editing = null }) {
                                    LucideIcon(Lucide.GitMerge, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.accent)
                                    Spacer(Modifier.width(6.dp)); Text(s.mergeTag, color = AppConfig.Theme.accent)
                                }
                                TextButton(onClick = { deleting = t; editing = null }) {
                                    LucideIcon(Lucide.Trash, size = AppConfig.Layout.iconSizeSmall, tint = AppConfig.Theme.danger)
                                    Spacer(Modifier.width(6.dp)); Text(s.deleteTag, color = AppConfig.Theme.danger)
                                }
                            }
                        }
                    }
                },
            )
        }
    }

    merging?.let { from ->
        EditorSheet(onDismiss = { merging = null }) {
            Text("${s.mergeInto}: ${from.name} →", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(AppConfig.Layout.itemSpacing))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.filter { it.id != from.id }.forEach { t -> TagChip(t.toTag(), onClick = { mergeTarget = t.toTag() }) }
            }
            Spacer(Modifier.height(AppConfig.Layout.sectionSpacing))
        }
    }
    mergeTarget?.let { into ->
        val from = merging ?: return@let
        ConfirmDialog(
            title = s.merge, body = s.mergeConfirm(from.name, into.name), confirmText = s.merge,
            onConfirm = { scope.launch { vm.repo.mergeTags(from.id, into.id); merging = null } },
            onDismiss = { mergeTarget = null },
        )
    }
    deleting?.let { t ->
        ConfirmDialog(
            title = s.deleteTag, body = s.deleteTagBody(t.name), confirmText = s.delete, destructive = true,
            onConfirm = { scope.launch { vm.repo.deleteTag(t.id) } },
            onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = AppConfig.Theme.surfaceElevated) {
        Column(Modifier.padding(horizontal = AppConfig.Layout.screenPadding).padding(bottom = AppConfig.Layout.sectionSpacing).navigationBarsPadding().imePadding()) {
            content()
        }
    }
}
