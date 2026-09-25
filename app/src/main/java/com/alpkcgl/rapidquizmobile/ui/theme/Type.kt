package com.alpkcgl.rapidquizmobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alpkcgl.rapidquizmobile.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Başlıklar ve sayılar. */
val SpaceGrotesk = FontFamily(
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700),
)

/** Gövde metni. */
val Inter = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
)

/** Sayılarda sabit genişlikli rakamlar (sayaçlar zıplamasın). */
const val TABULAR_NUMS = "tnum"

// Renk verilmez: metin rengi LocalContentColor'dan gelir (buton, çip vb. kendi rengini verebilsin).
private val display = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
private val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal)

val Typography = Typography(
    displayLarge = display.copy(fontSize = 64.sp, lineHeight = 68.sp),
    displayMedium = display.copy(fontSize = 44.sp, lineHeight = 48.sp),
    headlineLarge = display.copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = display.copy(fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = display.copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = display.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = display.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = body.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = body.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = body.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
