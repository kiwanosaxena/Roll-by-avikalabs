package com.avikalabs.roll.ui.tutorial

import android.graphics.PathMeasure
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.R
import com.avikalabs.roll.ui.theme.AccentSunset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val CaveatFontFamily: FontFamily by lazy {
    try {
        FontFamily(Font(R.font.caveat, FontWeight.Normal))
    } catch (_: Exception) {
        FontFamily.Cursive
    }
}

@Composable
fun CoachMarkOverlay(
    tutorialState: TutorialState,
    onStepCompleted: (tipSetId: String) -> Unit,
    onNeverShowAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tutorialState.isDialogActive) {
        return
    }

    val currentTipSet = tutorialState.currentTipSet ?: return
    val currentStep = tutorialState.currentStep ?: return
    val bounds = tutorialState.currentTargetBounds

    // If target bounds aren't found or empty, skip to next step after a brief layout grace period
    LaunchedEffect(currentStep, bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            kotlinx.coroutines.delay(80)
            val rechecked = tutorialState.currentTargetBounds
            if (rechecked == null || rechecked.width <= 0 || rechecked.height <= 0) {
                tutorialState.nextStep()
            }
        }
    }

    if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
        return
    }

    val isLastStep = tutorialState.currentStepIndex >= currentTipSet.steps.size - 1

    BackHandler {
        onStepCompleted(currentTipSet.id)
        tutorialState.dismiss()
    }

    val stepText = currentStep.customText ?: stringResource(currentStep.textResId)

    // Animation progress for the arrow (400ms)
    val arrowProgress = remember(currentStep.id) { Animatable(0f) }
    LaunchedEffect(currentStep.id) {
        arrowProgress.snapTo(0f)
        arrowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    val density = LocalDensity.current
    val insets = WindowInsets.systemBars.asPaddingValues()
    val topInsetPx = with(density) { insets.calculateTopPadding().toPx() }
    val bottomInsetPx = with(density) { insets.calculateBottomPadding().toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("coach_mark_overlay")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Dim layer consumes clicks to protect background
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 8dp padding around spotlight cut-out
        val paddingPx = with(density) { 8.dp.toPx() }
        val cornerRadiusPx = with(density) { 16.dp.toPx() }

        val spotlightRect = Rect(
            left = (bounds.left - paddingPx).coerceAtLeast(0f),
            top = (bounds.top - paddingPx).coerceAtLeast(0f),
            right = (bounds.right + paddingPx).coerceAtMost(screenWidthPx),
            bottom = (bounds.bottom + paddingPx).coerceAtMost(screenHeightPx)
        )

        // Decide whether to place tip above or below target
        val spaceAbove = spotlightRect.top - topInsetPx
        val spaceBelow = (screenHeightPx - bottomInsetPx) - spotlightRect.bottom
        val placeAbove = spaceAbove >= spaceBelow && spaceAbove > with(density) { 160.dp.toPx() }

        // Full-screen dim layer with spotlight hole
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Dim layer black ~70%
            drawRect(color = Color.Black.copy(alpha = 0.72f))

            // Clear spotlight cut-out
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(spotlightRect.left, spotlightRect.top),
                size = Size(spotlightRect.width, spotlightRect.height),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                blendMode = BlendMode.Clear
            )
        }

        // Draw the curvy arrow from tip card towards the spotlight
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val targetCenterX = spotlightRect.center.x.coerceIn(40f, screenWidthPx - 40f)
            val curveLeft = targetCenterX >= screenWidthPx / 2f

            val startPoint: Offset
            val endPoint: Offset
            val control1: Offset
            val control2: Offset

            if (placeAbove) {
                // Tip is above, arrow points downward into spotlight
                val startY = (spotlightRect.top - with(density) { 56.dp.toPx() }).coerceAtLeast(topInsetPx + 20f)
                val startX = if (curveLeft) targetCenterX + with(density) { 45.dp.toPx() } else targetCenterX - with(density) { 45.dp.toPx() }
                startPoint = Offset(startX, startY)
                endPoint = Offset(targetCenterX, spotlightRect.top - 6f)

                val dx = if (curveLeft) with(density) { 40.dp.toPx() } else -with(density) { 40.dp.toPx() }
                control1 = Offset(startX + dx, startY + (endPoint.y - startY) * 0.4f)
                control2 = Offset(targetCenterX + (dx * 0.5f), startY + (endPoint.y - startY) * 0.8f)
            } else {
                // Tip is below, arrow points upward into spotlight
                val startY = (spotlightRect.bottom + with(density) { 56.dp.toPx() }).coerceAtMost(screenHeightPx - bottomInsetPx - 20f)
                val startX = if (curveLeft) targetCenterX + with(density) { 45.dp.toPx() } else targetCenterX - with(density) { 45.dp.toPx() }
                startPoint = Offset(startX, startY)
                endPoint = Offset(targetCenterX, spotlightRect.bottom + 6f)

                val dx = if (curveLeft) with(density) { 40.dp.toPx() } else -with(density) { 40.dp.toPx() }
                control1 = Offset(startX + dx, startY + (endPoint.y - startY) * 0.4f)
                control2 = Offset(targetCenterX + (dx * 0.5f), startY + (endPoint.y - startY) * 0.8f)
            }

            val fullPath = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                cubicTo(control1.x, control1.y, control2.x, control2.y, endPoint.x, endPoint.y)
            }

            // Path trimming via Android PathMeasure
            val androidPath = fullPath.asAndroidPath()
            val pathMeasure = PathMeasure(androidPath, false)
            val pathLength = pathMeasure.length
            val animatedLength = pathLength * arrowProgress.value

            if (animatedLength > 0f) {
                val animatedAndroidPath = android.graphics.Path()
                pathMeasure.getSegment(0f, animatedLength, animatedAndroidPath, true)
                val animatedComposePath = animatedAndroidPath.asComposePath()

                drawPath(
                    path = animatedComposePath,
                    color = Color.White,
                    style = Stroke(
                        width = with(density) { 3.dp.toPx() },
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Arrow head when path is largely drawn
                if (arrowProgress.value > 0.85f) {
                    val pos = FloatArray(2)
                    val tan = FloatArray(2)
                    pathMeasure.getPosTan(animatedLength, pos, tan)
                    val headAngle = atan2(tan[1].toDouble(), tan[0].toDouble())
                    val arrowHeadLength = with(density) { 14.dp.toPx() }
                    val arrowSpread = Math.PI / 6.0 // 30 degrees

                    val endX = pos[0]
                    val endY = pos[1]

                    val leftWingX = (endX - arrowHeadLength * cos(headAngle - arrowSpread)).toFloat()
                    val leftWingY = (endY - arrowHeadLength * sin(headAngle - arrowSpread)).toFloat()
                    val rightWingX = (endX - arrowHeadLength * cos(headAngle + arrowSpread)).toFloat()
                    val rightWingY = (endY - arrowHeadLength * sin(headAngle + arrowSpread)).toFloat()

                    val headPath = Path().apply {
                        moveTo(leftWingX, leftWingY)
                        lineTo(endX, endY)
                        lineTo(rightWingX, rightWingY)
                    }

                    drawPath(
                        path = headPath,
                        color = Color.White,
                        style = Stroke(
                            width = with(density) { 3.dp.toPx() },
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Tip text and button layout
        val tipBoxModifier = if (placeAbove) {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = insets.calculateTopPadding() + 8.dp)
                .padding(horizontal = 20.dp)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = insets.calculateBottomPadding() + 16.dp)
                .padding(horizontal = 20.dp)
        }

        Box(
            modifier = tipBoxModifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step counter: "2 / 4"
                Text(
                    text = stringResource(
                        R.string.tutorial_step_counter,
                        tutorialState.currentStepIndex + 1,
                        currentTipSet.steps.size
                    ),
                    color = AccentSunset,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Handwritten Tip Text using Caveat
                Text(
                    text = stepText,
                    fontFamily = CaveatFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row
                if (!isLastStep) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onStepCompleted(currentTipSet.id)
                                tutorialState.dismiss()
                            },
                            modifier = Modifier
                                .focusable()
                                .testTag("tutorial_btn_skip")
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_btn_skip),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                tutorialState.nextStep()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSunset,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .focusable()
                                .testTag("tutorial_btn_next")
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_btn_next),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                onStepCompleted(currentTipSet.id)
                                tutorialState.dismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSunset,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(46.dp)
                                .focusable()
                                .testTag("tutorial_btn_got_it")
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_btn_got_it),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                onNeverShowAgain()
                                tutorialState.dismiss()
                            },
                            modifier = Modifier
                                .focusable()
                                .testTag("tutorial_btn_never_show")
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_btn_never_show),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
