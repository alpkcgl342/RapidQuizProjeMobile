package com.alpkcgl.rapidquizmobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", TR)

/** 1840 → "1.840" */
fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance(TR).format(value)

/** 1830 ms → "1,8" */
fun formatSeconds(ms: Long): String = NumberFormat.getNumberInstance(TR).apply {
    maximumFractionDigits = 1
}.format(ms / 1000.0)

/** ISO zaman damgası → "26 Eyl" (cihaz saat diliminde). */
fun formatShortDate(iso: String): String = runCatching {
    SHORT_DATE.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
}.getOrDefault("")

/** `&lt;b>` etiketli string kaynağını kalın bölümleriyle döner. */
@Composable
fun htmlString(id: Int, vararg args: Any): AnnotatedString =
    AnnotatedString.fromHtml(stringResource(id, *args))

/** Kullanıcıya gösterilecek mesaj: sunucu mesajı Türkçedir; istemci hataları kaynaktan gelir. */
@Composable
fun ApiException.userMessage(): String = when (code) {
    ApiException.NETWORK_ERROR -> stringResource(R.string.error_network)
    ApiException.HTTP_ERROR -> stringResource(R.string.error_unexpected)
    else -> message
}
