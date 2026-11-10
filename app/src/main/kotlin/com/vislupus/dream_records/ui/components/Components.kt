package com.vislupus.dream_records.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.LocalStrings
import com.vislupus.dream_records.config.TagColors
import com.vislupus.dream_records.data.db.Tag

/** Shared, uniform building blocks. All sizes/colours come from AppConfig. */

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = AppConfig.Layout.itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = AppConfig.Theme.onSurfaceMuted,
        modifier = modifier.padding(top = AppConfig.Layout.sectionSpacing, bottom = 8.dp),
    )
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val base = modifier.fillMaxWidth()
    val m = if (onClick != null) base.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick) else base
    Card(
        modifier = m,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = AppConfig.Theme.surface),
        border = BorderStroke(1.dp, AppConfig.Theme.surfaceBorder),
    ) {
        Box(Modifier.padding(AppConfig.Layout.cardPadding)) { content() }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(AppConfig.Layout.screenPadding), contentAlignment = Alignment.Center) {
        Text(text, color = AppConfig.Theme.onSurfaceMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A coloured tag chip. [small] for dense lists. */
@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = TagColors.at(tag.colorIndex)
    val bg = if (selected) c.background else Color.Transparent
    val fg = if (selected) c.text else c.background
    val shape = RoundedCornerShape(AppConfig.Layout.chipCornerRadius)
    val base = modifier
        .clip(shape)
        .background(bg, shape)
        .border(1.dp, c.background, shape)
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        m.padding(horizontal = if (small) 8.dp else 12.dp, vertical = if (small) 3.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tag.name,
            color = fg,
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        trailing?.let { Spacer(Modifier.width(6.dp)); it() }
    }
}

/** The 3 x 10 colour picker. */
@Composable
fun TagColorGrid(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cols = AppConfig.Layout.TAG_COLOR_COLUMNS
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppConfig.Layout.tagColorCellSpacing)) {
        TagColors.PALETTE.chunked(cols).forEachIndexed { row, colors ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                colors.forEachIndexed { col, color ->
                    val index = row * cols + col
                    val isSel = index == selected
                    Box(
                        Modifier
                            .size(AppConfig.Layout.tagColorCellSize)
                            .clip(CircleShape)
                            .background(color.background)
                            .border(if (isSel) 3.dp else 0.dp, if (isSel) AppConfig.Theme.onBackground else Color.Transparent, CircleShape)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) LucideIcon(Lucide.Check, size = 14.dp, tint = color.text)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppConfig.Theme.surfaceElevated,
        title = { Text(title) },
        text = { Text(body, color = AppConfig.Theme.onSurfaceMuted) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = if (destructive) AppConfig.Theme.danger else AppConfig.Theme.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = AppConfig.Theme.onSurfaceMuted) } },
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    singleLine: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = placeholder?.let { { Text(it, color = AppConfig.Theme.onSurfaceFaint) } },
        label = label?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leading,
        trailingIcon = trailing,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppConfig.Theme.accent,
            unfocusedBorderColor = AppConfig.Theme.surfaceBorder,
            focusedContainerColor = AppConfig.Theme.surface,
            unfocusedContainerColor = AppConfig.Theme.surface,
            cursorColor = AppConfig.Theme.accent,
            focusedLabelColor = AppConfig.Theme.accent,
            unfocusedLabelColor = AppConfig.Theme.onSurfaceMuted,
        ),
    )
}

@Composable
fun VSpace(height: androidx.compose.ui.unit.Dp = AppConfig.Layout.itemSpacing) = Spacer(Modifier.height(height))
