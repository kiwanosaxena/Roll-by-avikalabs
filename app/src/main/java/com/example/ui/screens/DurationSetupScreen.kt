package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DurationMode
import com.example.data.model.RotationConfig
import com.example.data.model.WallpaperItem
import com.example.data.model.WallpaperTarget
import com.example.ui.components.DialControl
import com.example.ui.components.DurationFormatter
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.theme.AccentSunset
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.MonospaceBadgeStyle

@Composable
fun DurationSetupScreen(
    target: WallpaperTarget,
    items: List<WallpaperItem>,
    config: RotationConfig,
    onDurationModeChanged: (DurationMode) -> Unit,
    onItemDurationChanged: (String, Int) -> Unit,
    onApplyGlobalDuration: (Int) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    var selectedMode by remember(config.durationMode) { mutableStateOf(config.durationMode) }
    var globalDuration by remember(config.globalDurationSeconds) { mutableIntStateOf(config.globalDurationSeconds) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Map of flipped state per item ID for 3D flip card
    val flippedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Mode toggle: "Per image" / "Global timer" (§7.3)
        ModeToggle(
            currentMode = selectedMode,
            onModeSelected = { mode ->
                selectedMode = mode
                onDurationModeChanged(mode)
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (selectedMode == DurationMode.PER_ITEM) {
                // Per-image Mode (§7.3.1): Grid of 5:7 Flip Cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        val isFlipped = flippedStates[item.id] ?: false
                        FlipCardItem(
                            item = item,
                            isFlipped = isFlipped,
                            onFlip = { flippedStates[item.id] = !isFlipped },
                            onDurationChanged = { newDuration ->
                                onItemDurationChanged(item.id, newDuration)
                            }
                        )
                    }
                }
            } else {
                // Global Mode (§7.3.2): Large preview + single dial + Apply to all
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview of first photo
                    if (items.isNotEmpty()) {
                        GlassBox(
                            modifier = Modifier
                                .size(width = 150.dp, height = 210.dp) // 5:7 ratio
                                .padding(4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AsyncImage(
                                model = items.first().uri,
                                contentDescription = "Global preview photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Same dial, one value — every image in this roll switches on this beat.",
                        fontSize = 12.5.sp,
                        color = glassColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Large Dial Control
                    DialControl(
                        durationSeconds = globalDuration,
                        onDurationChanged = { globalDuration = it },
                        sizeDp = 220
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Apply to all" Button (§7.3.2)
                    Button(
                        onClick = {
                            // Check if confirmation needed (§7.3.4)
                            val hasCustomTiming = items.any { it.durationSeconds != globalDuration }
                            if (hasCustomTiming) {
                                showConfirmDialog = true
                            } else {
                                onApplyGlobalDuration(globalDuration)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSunset,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(46.dp)
                            .testTag("btn_apply_to_all")
                    ) {
                        Text(
                            text = "Apply to all",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Pinned Bottom Next Button
        GlassButton(
            text = "Next",
            onClick = onNextClick,
            testTag = "btn_duration_next",
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }

    // Confirmation Dialog for destructive global apply (§7.3.4)
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Apply to all frames?",
                    color = glassColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val formatted = DurationFormatter.formatShort(globalDuration)
                Text(
                    text = "This replaces each photo's own timing with $formatted. Continue?",
                    color = glassColors.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyGlobalDuration(globalDuration)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSunset)
                ) {
                    Text("Apply", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = glassColors.textSecondary)
                }
            },
            containerColor = glassColors.glassFillElevated
        )
    }
}

@Composable
private fun FlipCardItem(
    item: WallpaperItem,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onDurationChanged: (Int) -> Unit
) {
    val glassColors = LocalGlassColors.current

    // 3D Y-Axis Flip Animation (450ms per PRD §7.3.1)
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "cardFlip"
    )

    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 7f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        elevated = isFlipped
    ) {
        if (rotation <= 90f) {
            // FRONT FACE: Photo + Duration Badge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button
                    ) { onFlip() }
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = "Tap to set duration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Corner duration tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = DurationFormatter.formatShort(item.durationSeconds),
                        style = MonospaceBadgeStyle.copy(color = Color.White, fontSize = 10.5.sp)
                    )
                }

                // Tap hint pill at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Tap to set timer",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.5.sp
                    )
                }
            }
        } else {
            // BACK FACE: Dial control + Close ✕ affordance (rotated 180 to be readable)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(glassColors.glassFillElevated)
                    .padding(6.dp)
            ) {
                // Close ✕ button in top corner (§7.3.1)
                IconButton(
                    onClick = onFlip,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(glassColors.glassBorder.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close dial",
                        tint = glassColors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Compact Dial Control
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DialControl(
                        durationSeconds = item.durationSeconds,
                        onDurationChanged = onDurationChanged,
                        sizeDp = 150
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    currentMode: DurationMode,
    onModeSelected: (DurationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modes = listOf(
                DurationMode.PER_ITEM to "Per image",
                DurationMode.GLOBAL to "Global timer"
            )

            modes.forEach { (mode, label) ->
                val isSelected = mode == currentMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (isSelected) AccentSunset else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onModeSelected(mode) }
                        .testTag("mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else glassColors.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
