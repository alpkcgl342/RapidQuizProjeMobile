package com.alpkcgl.rapidquizmobile.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.data.model.Outcome
import com.alpkcgl.rapidquizmobile.ui.components.htmlString
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.RqShapes
import com.alpkcgl.rapidquizmobile.ui.theme.SpaceGrotesk
import com.alpkcgl.rapidquizmobile.ui.theme.TABULAR_NUMS
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Kalan süre (ms). [running] iken her karede duvar saatine göre güncellenir; durunca
 * son değerde donar (cevap verildiğinde halka o anda kalır).
 */
@Composable
fun rememberRemainingMs(deadlineAtLocalMs: Long?, totalMs: Long, running: Boolean): Long {
    val remaining = remember { mutableLongStateOf(totalMs) }
    LaunchedEffect(deadlineAtLocalMs, running) {
        if (deadlineAtLocalMs == null) {
            remaining.longValue = totalMs
            return@LaunchedEffect
        }
        while (running) {
            val left = (deadlineAtLocalMs - System.currentTimeMillis()).coerceIn(0, totalMs)
            remaining.longValue = left
            if (left == 0L) break
            withFrameMillis { }
        }
    }
    return remaining.longValue
}

/** 5 sn'den 0'a inen halka. > 3 sn yeşil, 3–1.5 sn amber, < 1.5 sn kırmızı; son 2 sn nabız. */
@Composable
fun CountdownRing(remainingMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val color = when {
        remainingMs > 3000 -> RqColors.TimerSafe
        remainingMs > 1500 -> RqColors.TimerWarn
        else -> RqColors.TimerCritical
    }
    val animatedColor by animateColorAsState(color, tween(240), label = "ringColor")
    val seconds = ceil(remainingMs / 1000.0).toInt()
    val pulsing = remainingMs in 1..2000
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val fraction = if (totalMs > 0) remainingMs.toFloat() / totalMs else 0f
    // Ekran okuyucuya yalnızca 3 ve 1 saniyede duyuru
    val announcement = if (seconds == 3 || seconds == 1) stringResource(R.string.quiz_seconds_left, seconds) else ""

    Box(
        modifier
            .size(88.dp)
            .scale(if (pulsing) pulseScale else 1f)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = RqColors.Border,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke),
            )
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$seconds",
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = TABULAR_NUMS),
            color = animatedColor,
        )
    }
}

/** "Soru 7 / 20" + ilerleme çubuğu. */
@Composable
fun QuestionProgress(index: Int, total: Int, modifier: Modifier = Modifier) {
    val a11y = stringResource(R.string.quiz_progress_a11y, index + 1, total)
    val pct by animateFloatAsState(
        if (total > 0) (index + 1f) / total else 0f,
        tween(240),
        label = "progress",
    )
    Column(
        modifier.clearAndSetSemantics { contentDescription = a11y },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            htmlString(R.string.quiz_progress, index + 1, total),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMS),
            color = RqColors.TextMuted,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RqShapes.Pill)
                .background(RqColors.Border),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct)
                    .height(8.dp)
                    .clip(RqShapes.Pill)
                    .background(RqColors.Primary),
            )
        }
    }
}

enum class OptionState { IDLE, SELECTED, CORRECT, WRONG, DISABLED }

/** Şık: A/B/C/D rozeti + metin, min. 56 dp. Yalnızca IDLE durumda tıklanır. */
@Composable
fun OptionButton(
    label: String,
    text: String,
    state: OptionState,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = when (state) {
        OptionState.SELECTED -> accent
        OptionState.CORRECT -> RqColors.Success
        OptionState.WRONG -> RqColors.Danger
        else -> RqColors.Border
    }
    val background = when (state) {
        OptionState.SELECTED -> RqColors.PrimarySoft
        OptionState.CORRECT -> RqColors.SuccessSoft
        OptionState.WRONG -> RqColors.DangerSoft
        else -> RqColors.Surface
    }
    val badgeBackground = when (state) {
        OptionState.CORRECT -> RqColors.Success
        OptionState.WRONG -> RqColors.Danger
        else -> RqColors.SurfaceAlt
    }
    val badgeContent = if (state == OptionState.CORRECT || state == OptionState.WRONG) RqColors.TextOnColor else RqColors.TextMuted

    // Yanlışta kısa sallanma
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == OptionState.WRONG) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 300
                    -6f at 50
                    6f at 100
                    -4f at 150
                    4f at 200
                    0f at 300
                },
            )
        }
    }
    val stateText = when (state) {
        OptionState.CORRECT -> stringResource(R.string.option_correct_a11y)
        OptionState.WRONG -> stringResource(R.string.option_wrong_a11y)
        else -> null
    }

    Row(
        modifier
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (state == OptionState.DISABLED) 0.55f else 1f)
            .clip(RqShapes.Medium)
            .background(background)
            .border(2.dp, border, RqShapes.Medium)
            .clickable(enabled = state == OptionState.IDLE, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = listOfNotNull("$label. $text", stateText).joinToString(", ")
                if (state != OptionState.IDLE) disabled()
            }
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RqShapes.Small)
                .background(badgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                OptionState.CORRECT -> Icon(painterResource(R.drawable.ic_check), null, tint = badgeContent, modifier = Modifier.size(18.dp))
                OptionState.WRONG -> Icon(painterResource(R.drawable.ic_x), null, tint = badgeContent, modifier = Modifier.size(18.dp))
                else -> Text(
                    label,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = badgeContent,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 21.sp),
        )
    }
}

/** "Doğru!" / "Yanlış" / "Süre doldu!" rozeti, yukarı kayan "+135" ve doğruda konfeti. */
@Composable
fun FeedbackOverlay(outcome: Outcome, points: Int, modifier: Modifier = Modifier) {
    val (text, icon) = when (outcome) {
        Outcome.CORRECT -> stringResource(R.string.feedback_correct) to R.drawable.ic_check
        Outcome.WRONG -> stringResource(R.string.feedback_wrong) to R.drawable.ic_x
        Outcome.TIMEOUT -> stringResource(R.string.feedback_timeout) to R.drawable.ic_clock
    }
    val pillColor = if (outcome == Outcome.CORRECT) RqColors.SuccessStrong else RqColors.Danger

    val pop = remember { Animatable(0.6f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(900, easing = EaseOut))
    }

    Box(
        modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        contentAlignment = Alignment.Center,
    ) {
        if (outcome == Outcome.CORRECT) Confetti(progress.value)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (points > 0) {
                val pointsA11y = stringResource(R.string.feedback_points_a11y, points)
                Text(
                    stringResource(R.string.feedback_points, points),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                    color = RqColors.SuccessStrong,
                    modifier = Modifier
                        .offset { IntOffset(0, (-24 * progress.value).dp.roundToPx()) }
                        .alpha(1f - (progress.value - 0.6f).coerceAtLeast(0f) / 0.4f)
                        .semantics { contentDescription = pointsA11y },
                )
            }
            Row(
                Modifier
                    .scale(pop.value)
                    .clip(RqShapes.Pill)
                    .background(pillColor)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(icon), null, tint = RqColors.TextOnColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.titleMedium, color = RqColors.TextOnColor)
            }
        }
    }
}

/** 14 parçalık sabit dağılımlı konfeti (render'da rastgelelik yok). */
@Composable
private fun Confetti(progress: Float) {
    val alpha = if (progress < 0.7f) 1f else ((1f - progress) / 0.3f).coerceIn(0f, 1f)
    Canvas(Modifier.size(1.dp)) {
        repeat(14) { i ->
            val angle = i / 14.0 * PI * 2
            val dist = (70 + (i % 3) * 22).dp.toPx() * progress
            val dx = (cos(angle) * dist).toFloat()
            val dy = (sin(angle) * dist).toFloat() - 20.dp.toPx() * progress
            val rot = (if (i % 2 == 1) 1 else -1) * (180 + i * 20) * progress
            val piece = Size(8.dp.toPx(), 12.dp.toPx())
            rotate(rot, pivot = Offset(dx, dy)) {
                drawRect(
                    color = RqColors.Confetti[i % RqColors.Confetti.size].copy(alpha = alpha),
                    topLeft = Offset(dx - piece.width / 2, dy - piece.height / 2),
                    size = piece,
                )
            }
        }
    }
}
