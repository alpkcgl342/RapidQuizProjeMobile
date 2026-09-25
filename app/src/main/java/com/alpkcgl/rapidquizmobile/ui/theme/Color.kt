package com.alpkcgl.rapidquizmobile.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Web `tokens.css` renkleri (proje dokümanı §13). Yalnızca açık tema. */
object RqColors {
    // Marka
    val Primary = Color(0xFF6C4DFF)
    val PrimaryStrong = Color(0xFF5438E0)
    val PrimarySoft = Color(0xFFEFEBFF)
    val Accent = Color(0xFFFF5C8A)
    val AccentSoft = Color(0xFFFFE9F0)

    // Zemin ve yüzeyler
    val Background = Color(0xFFF7F5FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF1EEFC)
    val Border = Color(0xFFE4DFF7)

    // Metin
    val Text = Color(0xFF1B1233)
    val TextMuted = Color(0xFF5A5273)
    val TextOnColor = Color(0xFFFFFFFF)

    // Durum
    val Success = Color(0xFF0FA968)
    val SuccessStrong = Color(0xFF0A7D4D)
    val SuccessSoft = Color(0xFFE3F8EE)
    val Danger = Color(0xFFE11D48)
    val DangerSoft = Color(0xFFFFE7EC)
    val Warning = Color(0xFFF59E0B)
    val WarningSoft = Color(0xFFFFF4E0)
    /** Açık uyarı zemininde okunur metin (web StatTile). */
    val WarningText = Color(0xFF9A5B00)

    // Geri sayım
    val TimerSafe = Success
    val TimerWarn = Warning
    val TimerCritical = Danger

    val HeroGradient = Brush.linearGradient(listOf(Primary, Accent))

    // Skor tablosu madalyaları (açık → koyu, metin)
    val Gold = listOf(Color(0xFFFFE27A), Color(0xFFF5B800), Color(0xFF5C4300))
    val Silver = listOf(Color(0xFFEEF1F6), Color(0xFFB8C0CC), Color(0xFF3D4654))
    val Bronze = listOf(Color(0xFFFFD1A8), Color(0xFFD9823B), Color(0xFF5A2E08))

    val Confetti = listOf(Primary, Accent, Color(0xFF00C2A8), Warning, Color(0xFF2B8CFF))
}
