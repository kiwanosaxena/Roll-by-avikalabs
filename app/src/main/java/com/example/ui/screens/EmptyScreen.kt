package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WallpaperTarget
import com.example.ui.theme.AccentSunset
import com.example.ui.theme.LocalGlassColors

/**
 * EmptyStateBody contains ONLY the centered body content:
 * the dashed circle with "+" glyph and explanatory text.
 * It contains NO App Bar, NO toggle, and NO footer buttons.
 */
@Composable
fun EmptyStateBody(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered 74dp Dashed-Outline Circle with + glyph (§7.1)
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) { onAddClick() }
                .semantics { contentDescription = "Add photos to start your roll" }
                .testTag("empty_add_circle"),
            contentAlignment = Alignment.Center
        ) {
            // Dashed Circle Border
            Canvas(modifier = Modifier.size(74.dp)) {
                val strokeWidth = 2.dp.toPx()
                val stroke = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
                drawCircle(
                    color = AccentSunset,
                    radius = (size.minDimension / 2f) - (strokeWidth / 2f),
                    style = stroke
                )
            }

            // Centered + glyph (34sp, weight 300 per PRD §7.1)
            Text(
                text = "+",
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = AccentSunset,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Title: "No frames yet", 14sp medium
        Text(
            text = "No frames yet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = glassColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Body copy: 11.5sp, on-surface-variant / textSecondary
        Text(
            text = "Add a few photos to start your roll. You can set how long each one stays, or use one timer for all of them.",
            fontSize = 11.5.sp,
            color = glassColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * Backward-compatible wrapper for EmptyScreenContent.
 * Renders ONLY the body content without duplicating toggles or app bars.
 */
@Composable
fun EmptyScreenContent(
    activeTarget: WallpaperTarget = WallpaperTarget.HOME,
    onTargetSelected: (WallpaperTarget) -> Unit = {},
    autoChangeOnUnlock: Boolean = false,
    onAutoChangeOnUnlockToggle: (Boolean) -> Unit = {},
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateBody(
        onAddClick = onAddClick,
        modifier = modifier
    )
}
