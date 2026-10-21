package com.avikalabs.roll.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors

import com.avikalabs.roll.ui.tutorial.tutorialTarget

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    elevated: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    val fill = if (elevated) glassColors.glassFillElevated else glassColors.glassFill

    Box(
        modifier = modifier
            .background(fill, shape)
            .border(1.dp, glassColors.glassBorder, shape)
            .clip(shape)
    ) {
        content()
    }
}

@Composable
fun GlassSegmentedToggle(
    selectedTarget: WallpaperTarget,
    onTargetSelected: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = true
) {
    val glassColors = LocalGlassColors.current

    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(4.dp)
            .tutorialTarget("target_toggle"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val options = listOf(
                WallpaperTarget.HOME to "Home screen",
                WallpaperTarget.LOCK to "Lock screen"
            )

            options.forEach { (target, label) ->
                val isSelected = target == selectedTarget
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) AccentSunset else Color.Transparent,
                    animationSpec = spring(),
                    label = "toggleBg"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else glassColors.textPrimary,
                    label = "toggleText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(animatedBg)
                        .clickable(
                            interactionSource = remember(target) { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            onTargetSelected(target)
                        }
                        .testTag("toggle_${target.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            color = animatedTextColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (target == WallpaperTarget.LOCK && !isPro) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Pro feature",
                                tint = if (isSelected) Color.White else AccentSunset,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentSunset,
            contentColor = Color.White,
            disabledContainerColor = AccentSunset.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.60f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

object DurationFormatter {
    fun formatShort(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds % 60 == 0 -> "${seconds / 60}m"
            else -> "${seconds / 60}m ${seconds % 60}s"
        }
    }

    fun formatPlainSpeech(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds seconds"
            seconds % 60 == 0 -> {
                val mins = seconds / 60
                if (mins == 1) "1 minute" else "$mins minutes"
            }
            else -> {
                val mins = seconds / 60
                val secs = seconds % 60
                val minStr = if (mins == 1) "1 minute" else "$mins minutes"
                "$minStr $secs seconds"
            }
        }
    }
}
