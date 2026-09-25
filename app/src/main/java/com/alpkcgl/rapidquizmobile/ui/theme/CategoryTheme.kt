package com.alpkcgl.rapidquizmobile.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt
import com.alpkcgl.rapidquizmobile.R

/**
 * Kategori renkleri (web `lib/categoryTheme.ts`). Canlı ton zemin/gradyan/ikon içindir;
 * üstünde beyaz metin olacak dolgular [strong] tonunu kullanır.
 */
data class CategoryPalette(val base: Color, val strong: Color)

private val PALETTES = mapOf(
    "yazilim" to CategoryPalette(Color(0xFF6C4DFF), Color(0xFF5438E0)),
    "yapay-zeka" to CategoryPalette(Color(0xFF00C2A8), Color(0xFF00806F)),
    "bilgisayar-muhendisligi" to CategoryPalette(Color(0xFF2B8CFF), Color(0xFF1A6FD4)),
    "ulkeler" to CategoryPalette(Color(0xFFFF8A3D), Color(0xFFC25510)),
    "fizik" to CategoryPalette(Color(0xFFFF5C8A), Color(0xFFD62E5E)),
)

fun categoryPalette(slug: String, colorHex: String): CategoryPalette =
    PALETTES[slug] ?: run {
        // Bilinmeyen kategoride koyu ton türetilir (%72 renk + siyah).
        val base = runCatching { Color(colorHex.toColorInt()) }.getOrDefault(RqColors.Primary)
        CategoryPalette(base, lerp(base, Color.Black, 0.28f))
    }

/** Backend'deki ikon anahtarları → Lucide vektörleri. */
@DrawableRes
fun categoryIcon(icon: String): Int = when (icon) {
    "code" -> R.drawable.ic_code
    "brain" -> R.drawable.ic_brain
    "chip" -> R.drawable.ic_cpu
    "globe" -> R.drawable.ic_globe
    "atom" -> R.drawable.ic_atom
    else -> R.drawable.ic_sparkles
}
