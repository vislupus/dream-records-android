package com.vislupus.dream_records.config

import androidx.compose.ui.graphics.Color

/**
 * The 30 preset tag colours, laid out in the editor as 3 rows x 10 columns
 * (index 0-9 = row 1, 10-19 = row 2, 20-29 = row 3).
 *
 * Each entry has a background and a text colour so chips stay readable.
 * Tags store only the index, so you can retune the palette here at any time.
 */
data class TagColor(val background: Color, val text: Color)

object TagColors {

    private val DARK_TEXT = Color(0xFF0B0D12)
    private val LIGHT_TEXT = Color(0xFFF5F6FA)

    val PALETTE: List<TagColor> = listOf(
        // ---- Row 1: saturated
        TagColor(Color(0xFFE5484D), LIGHT_TEXT), // 0  red
        TagColor(Color(0xFFF76B15), LIGHT_TEXT), // 1  orange
        TagColor(Color(0xFFF5B400), DARK_TEXT),  // 2  amber
        TagColor(Color(0xFFBDE029), DARK_TEXT),  // 3  yellow-green
        TagColor(Color(0xFF46A758), LIGHT_TEXT), // 4  green
        TagColor(Color(0xFF12A594), LIGHT_TEXT), // 5  teal
        TagColor(Color(0xFF00A2C7), LIGHT_TEXT), // 6  cyan
        TagColor(Color(0xFF5EB1EF), DARK_TEXT),  // 7  sky blue
        TagColor(Color(0xFF3E63DD), LIGHT_TEXT), // 8  blue
        TagColor(Color(0xFF8E4EC6), LIGHT_TEXT), // 9  purple

        // ---- Row 2: deep / earthy
        TagColor(Color(0xFFD6409F), LIGHT_TEXT), // 10 pink
        TagColor(Color(0xFFE54666), LIGHT_TEXT), // 11 crimson
        TagColor(Color(0xFFAD7F58), LIGHT_TEXT), // 12 brown
        TagColor(Color(0xFF8B6F47), LIGHT_TEXT), // 13 dark brown
        TagColor(Color(0xFF7F9C3A), LIGHT_TEXT), // 14 olive
        TagColor(Color(0xFF2F7F6A), LIGHT_TEXT), // 15 pine
        TagColor(Color(0xFF2B6CB0), LIGHT_TEXT), // 16 navy blue
        TagColor(Color(0xFF6E56CF), LIGHT_TEXT), // 17 violet
        TagColor(Color(0xFF9E8CFC), DARK_TEXT),  // 18 lavender
        TagColor(Color(0xFF8D8D8D), DARK_TEXT),  // 19 grey

        // ---- Row 3: soft pastels (good on dark backgrounds)
        TagColor(Color(0xFFFFB3B3), DARK_TEXT),  // 20 pastel red
        TagColor(Color(0xFFFFCFA3), DARK_TEXT),  // 21 pastel orange
        TagColor(Color(0xFFFFE9A3), DARK_TEXT),  // 22 pastel yellow
        TagColor(Color(0xFFD9F2A3), DARK_TEXT),  // 23 pastel lime
        TagColor(Color(0xFFB7EBC7), DARK_TEXT),  // 24 pastel green
        TagColor(Color(0xFFA8E6DC), DARK_TEXT),  // 25 pastel teal
        TagColor(Color(0xFFB3E5FC), DARK_TEXT),  // 26 pastel sky
        TagColor(Color(0xFFC5CFFF), DARK_TEXT),  // 27 pastel blue
        TagColor(Color(0xFFE2C6FF), DARK_TEXT),  // 28 pastel purple
        TagColor(Color(0xFFFFC8E8), DARK_TEXT),  // 29 pastel pink
    )

    // Named indices used by DefaultTags (so the defaults stay readable).
    const val RED = 0
    const val ORANGE = 1
    const val YELLOW_GREEN = 3
    const val TEAL = 5
    const val SKY_BLUE = 7
    const val PURPLE = 9
    const val BROWN = 12

    fun at(index: Int): TagColor = PALETTE[index.coerceIn(0, PALETTE.lastIndex)]
}
