package com.avikalabs.roll.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import com.avikalabs.roll.ui.theme.MonospaceTimerStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object DialMath {
    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 3600 // 60 minutes
    const val ZONE_BOUNDARY_SECONDS = 60
    const val ZONE_BOUNDARY_ANGLE = 90f // 3 o'clock

    fun secondsToAngle(seconds: Int): Float {
        val s = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
        return if (s <= ZONE_BOUNDARY_SECONDS) {
            (s.toFloat() / ZONE_BOUNDARY_SECONDS) * ZONE_BOUNDARY_ANGLE
        } else {
            val fraction = (s - ZONE_BOUNDARY_SECONDS).toFloat() / (MAX_SECONDS - ZONE_BOUNDARY_SECONDS)
            ZONE_BOUNDARY_ANGLE + fraction * (360f - ZONE_BOUNDARY_ANGLE)
        }
    }

    fun angleToSnappedSeconds(angleDeg: Float): Int {
        var normalizedAngle = (angleDeg % 360f + 360f) % 360f
        return if (normalizedAngle <= ZONE_BOUNDARY_ANGLE) {
            val fraction = normalizedAngle / ZONE_BOUNDARY_ANGLE
            val steps = (fraction * 12).roundToInt().coerceIn(1, 12)
            steps * 5
        } else {
            val fraction = (normalizedAngle - ZONE_BOUNDARY_ANGLE) / (360f - ZONE_BOUNDARY_ANGLE)
            val steps = (fraction * 236).roundToInt().coerceIn(0, 236)
            60 + steps * 15
        }
    }

    fun stepIncrement(currentSeconds: Int): Int {
        return if (currentSeconds < 60) {
            (currentSeconds + 5).coerceAtMost(MAX_SECONDS)
        } else {
            (currentSeconds + 15).coerceAtMost(MAX_SECONDS)
        }
    }

    fun stepDecrement(currentSeconds: Int): Int {
        return if (currentSeconds <= 60) {
            (currentSeconds - 5).coerceAtLeast(MIN_SECONDS)
        } else {
            (currentSeconds - 15).coerceAtLeast(MIN_SECONDS)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialControl(
    durationSeconds: Int,
    onDurationChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    val glassColors = LocalGlassColors.current
    val currentAngle = DialMath.secondsToAngle(durationSeconds)

    val plainSpeech = DurationFormatter.formatPlainSpeech(durationSeconds)
    val formattedDisplay = DurationFormatter.formatShort(durationSeconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Circular Dial
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .semantics {
                    contentDescription = "Hold time dial, currently $plainSpeech"
                    stateDescription = plainSpeech
                }
                .testTag("dial_control"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchOffset = change.position - center
                            // Angle clockwise from 12 o'clock (-PI/2)
                            var rad = atan2(touchOffset.y, touchOffset.x) + (PI / 2.0)
                            if (rad < 0) rad += 2 * PI
                            val deg = (rad * 180.0 / PI).toFloat()
                            val snappedSeconds = DialMath.angleToSnappedSeconds(deg)
                            onDurationChanged(snappedSeconds)
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 18.dp.toPx()
                val strokeWidth = 8.dp.toPx()

                // Background track
                drawCircle(
                    color = glassColors.glassBorder.copy(alpha = 0.35f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Stopwatch ticks along bezel
                val numTicks = 60
                for (i in 0 until numTicks) {
                    val tickAngle = (i * 360f / numTicks) - 90f
                    val isZoneBoundary = i == 15 // 3 o'clock (90 degrees clockwise from 12)
                    val isMajor = i % 5 == 0

                    val tickLength = when {
                        isZoneBoundary -> 12.dp.toPx()
                        isMajor -> 8.dp.toPx()
                        else -> 4.dp.toPx()
                    }
                    val tickWidth = when {
                        isZoneBoundary -> 3.dp.toPx()
                        isMajor -> 2.dp.toPx()
                        else -> 1.dp.toPx()
                    }
                    val tickColor = when {
                        isZoneBoundary -> AccentSunset
                        isMajor -> glassColors.textSecondary.copy(alpha = 0.7f)
                        else -> glassColors.textSecondary.copy(alpha = 0.3f)
                    }

                    val rad = Math.toRadians(tickAngle.toDouble())
                    val innerRadius = radius - (strokeWidth / 2f) - 4.dp.toPx()
                    val outerRadius = innerRadius - tickLength

                    val start = Offset(
                        (center.x + innerRadius * cos(rad)).toFloat(),
                        (center.y + innerRadius * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        (center.x + outerRadius * cos(rad)).toFloat(),
                        (center.y + outerRadius * sin(rad)).toFloat()
                    )

                    drawLine(
                        color = tickColor,
                        start = start,
                        end = end,
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }

                // 1m Boundary Label at 3 o'clock
                val boundaryRad = Math.toRadians(0.0) // 3 o'clock in standard math
                val labelDist = radius - 24.dp.toPx()
                val boundaryPaint = android.graphics.Paint().apply {
                    color = AccentSunset.toArgb()
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "1m",
                    (center.x + labelDist * cos(boundaryRad)).toFloat() - 4.dp.toPx(),
                    (center.y + labelDist * sin(boundaryRad)).toFloat() + 4.dp.toPx(),
                    boundaryPaint
                )

                // Colored progress arc starting at 12 o'clock (-90 degrees in drawArc)
                val sweepAngle = currentAngle
                drawArc(
                    color = AccentSunset,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draggable handle at current angle
                val handleRad = Math.toRadians((currentAngle - 90f).toDouble())
                val handlePos = Offset(
                    (center.x + radius * cos(handleRad)).toFloat(),
                    (center.y + radius * sin(handleRad)).toFloat()
                )

                // Outer handle ring
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = handlePos
                )
                // Inner handle accent dot
                drawCircle(
                    color = AccentSunset,
                    radius = 7.dp.toPx(),
                    center = handlePos
                )
            }

            // Center readout display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedDisplay,
                    style = MonospaceTimerStyle.copy(
                        color = glassColors.textPrimary,
                        fontSize = if (formattedDisplay.length > 5) 24.sp else 30.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "HOLD TIME",
                    color = glassColors.textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset chips row for quick selection & TalkBack accessibility (§7.3.3)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 6
        ) {
            val presets = listOf(
                15 to "15s",
                60 to "1m",
                300 to "5m",
                900 to "15m",
                1800 to "30m",
                3600 to "60m"
            )

            presets.forEach { (secs, label) ->
                val isSelected = durationSeconds == secs
                PresetChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onDurationChanged(secs) }
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val fill = if (isSelected) AccentSunset else glassColors.glassFill
    val textColor = if (isSelected) Color.White else glassColors.textPrimary
    val borderColor = if (isSelected) AccentSunset else glassColors.glassBorder

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
