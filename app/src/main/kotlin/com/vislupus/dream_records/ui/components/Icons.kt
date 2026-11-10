package com.vislupus.dream_records.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.vislupus.dream_records.R
import com.vislupus.dream_records.config.AppConfig

/**
 * Lucide icons (https://lucide.dev), converted to vector drawables in res/drawable.
 * Use [LucideIcon] everywhere so sizing and tinting stay uniform.
 */
object Lucide {
    val Mic = R.drawable.lucide_mic
    val Square = R.drawable.lucide_square
    val Library = R.drawable.lucide_library
    val Tag = R.drawable.lucide_tag
    val Tags = R.drawable.lucide_tags
    val Calendar = R.drawable.lucide_calendar
    val Settings = R.drawable.lucide_settings
    val Play = R.drawable.lucide_play
    val Pause = R.drawable.lucide_pause
    val Trash = R.drawable.lucide_trash_2
    val Pencil = R.drawable.lucide_pencil
    val Merge = R.drawable.lucide_merge
    val GitMerge = R.drawable.lucide_git_merge
    val Plus = R.drawable.lucide_plus
    val Search = R.drawable.lucide_search
    val X = R.drawable.lucide_x
    val Check = R.drawable.lucide_check
    val CheckCheck = R.drawable.lucide_check_check
    val Download = R.drawable.lucide_download
    val Upload = R.drawable.lucide_upload
    val ChevronLeft = R.drawable.lucide_chevron_left
    val ChevronRight = R.drawable.lucide_chevron_right
    val WifiOff = R.drawable.lucide_wifi_off
    val Sparkles = R.drawable.lucide_sparkles        // Gemini
    val Zap = R.drawable.lucide_zap                  // Groq
    val AudioLines = R.drawable.lucide_audio_lines   // ElevenLabs
    val FileText = R.drawable.lucide_file_text
    val Moon = R.drawable.lucide_moon
    val Clock = R.drawable.lucide_clock
    val Languages = R.drawable.lucide_languages
    val Key = R.drawable.lucide_key_round
    val CircleDot = R.drawable.lucide_circle_dot
    val Combine = R.drawable.lucide_combine
    val More = R.drawable.lucide_ellipsis_vertical
    val Loader = R.drawable.lucide_loader_circle
    val Alert = R.drawable.lucide_circle_alert
    val Info = R.drawable.lucide_info
    val FolderLock = R.drawable.lucide_folder_lock
    val RotateCcw = R.drawable.lucide_rotate_ccw
    val BookOpen = R.drawable.lucide_book_open
    val Share = R.drawable.lucide_share_2
}

@Composable
fun LucideIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = AppConfig.Layout.iconSize,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
