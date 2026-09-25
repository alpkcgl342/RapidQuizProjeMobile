package com.alpkcgl.rapidquizmobile.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.RqShapes

enum class ButtonSize(val minHeight: Dp) { Medium(48.dp), Large(56.dp) }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = size.minHeight),
        shape = RqShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = RqColors.Primary,
            contentColor = RqColors.TextOnColor,
            disabledContainerColor = RqColors.Primary.copy(alpha = 0.5f),
            disabledContentColor = RqColors.TextOnColor,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = RqColors.TextOnColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            ButtonContent(text, icon)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium,
    @DrawableRes icon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = size.minHeight),
        shape = RqShapes.Medium,
        border = BorderStroke(2.dp, RqColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RqColors.Surface,
            contentColor = RqColors.Text,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonSize.Medium.minHeight),
        shape = RqShapes.Medium,
        colors = ButtonDefaults.textButtonColors(contentColor = RqColors.TextMuted),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Şimşek işareti + "Rapid Quiz". */
@Composable
fun AppLogo(modifier: Modifier = Modifier, large: Boolean = false) {
    val markSize = if (large) 48.dp else 36.dp
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(markSize)
                .clip(RqShapes.Medium)
                .background(RqColors.HeroGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_zap),
                contentDescription = null,
                tint = RqColors.TextOnColor,
                modifier = Modifier.size(markSize * 0.55f),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.logo_text),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = if (large) 30.sp else 22.sp,
            ),
        )
    }
}

@Composable
fun LoadingView(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = RqColors.Primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RqColors.TextMuted)
    }
}

/** Hata kutusu: başlık, mesaj, "Tekrar dene" ve isteğe bağlı ek butonlar. */
@Composable
fun ErrorView(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    retryLabel: String = stringResource(R.string.action_retry),
    onRetry: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    StateBox(
        icon = R.drawable.ic_x,
        iconTint = RqColors.Danger,
        iconBackground = RqColors.DangerSoft,
        title = title,
        text = message,
        modifier = modifier,
    ) {
        if (onRetry != null) {
            PrimaryButton(retryLabel, onRetry, size = ButtonSize.Medium, modifier = Modifier.fillMaxWidth())
        }
        extra()
    }
}

@Composable
fun EmptyView(title: String, text: String?, modifier: Modifier = Modifier) {
    StateBox(
        icon = R.drawable.ic_trophy,
        iconTint = RqColors.Primary,
        iconBackground = RqColors.PrimarySoft,
        title = title,
        text = text,
        modifier = modifier,
    )
}

@Composable
private fun StateBox(
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    text: String?,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RqShapes.Pill)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (!text.isNullOrBlank()) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = RqColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { actions() }
    }
}

/** Beyaz kart yüzeyi. */
@Composable
fun RqCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RqShapes.Large)
            .background(RqColors.Surface)
            .padding(20.dp),
        content = content,
    )
}
