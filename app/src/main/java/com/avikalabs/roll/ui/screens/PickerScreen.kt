package com.avikalabs.roll.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.avikalabs.roll.ui.tutorial.tutorialTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.avikalabs.roll.R
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.ui.components.DurationFormatter
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.components.GlassButton
import com.avikalabs.roll.ui.components.GlassSegmentedToggle
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import com.avikalabs.roll.ui.theme.MonospaceBadgeStyle

/**
 * PickerScreen is ONE composable structured with ONE main Column containing in sequential order:
 * 1. The Home/Lock toggle (GlassSegmentedToggle)
 * 2. Body content (EmptyStateBody OR PhotoGrid, conditionally)
 * 3. Auto change on unlock checkbox (for Lock screen)
 * 4. Next button
 */
@Composable
fun PickerScreen(
    activeTarget: WallpaperTarget,
    items: List<WallpaperItem>,
    autoChangeOnUnlock: Boolean,
    isLiveWallpaperActive: Boolean,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onAutoChangeOnUnlockToggle: (Boolean) -> Unit,
    onSetLiveWallpaperClick: () -> Unit,
    onAddTileClick: () -> Unit,
    onRemoveItem: (String) -> Unit = {},
    onNextClick: () -> Unit,
    isPro: Boolean = false,
    onNavigatePro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    val hasPhotos = items.isNotEmpty()

    val handleAddClick = {
        if (!isPro && items.size >= 10) {
            onNavigatePro()
        } else {
            onAddTileClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Segmented Target Toggle (Home screen / Lock screen) - Appears exactly once
        GlassSegmentedToggle(
            selectedTarget = activeTarget,
            onTargetSelected = onTargetSelected,
            isPro = isPro,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 2. Body content: Pro Lock card OR empty state OR photo grid
        if (!isPro && activeTarget == WallpaperTarget.LOCK) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentSunset.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = AccentSunset,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.pro_lock_screen_banner),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = glassColors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "In free mode, your Home roll applies to both screens. Upgrade to Roll Pro to run an independent roll with its own timer on your Lock screen.",
                            fontSize = 13.sp,
                            color = glassColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigatePro,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentSunset),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("btn_unlock_lock_screen_pro")
                                .tutorialTarget("btn_unlock_lock_screen_pro")
                        ) {
                            Text(
                                text = stringResource(R.string.pro_btn_unlock),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else if (!hasPhotos) {
            EmptyStateBody(
                onAddClick = handleAddClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            PhotoGrid(
                items = items,
                activeTarget = activeTarget,
                autoChangeOnUnlock = autoChangeOnUnlock && isLiveWallpaperActive,
                onAddTileClick = handleAddClick,
                onRemoveItem = onRemoveItem,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        // 3. Lock screen auto change on unlock option (§7.1 / §7.2)
        if (activeTarget == WallpaperTarget.LOCK) {
            if (!isPro) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigatePro() }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { onNavigatePro() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentSunset,
                            uncheckedColor = glassColors.textSecondary
                        ),
                        modifier = Modifier.testTag("checkbox_unlock_mode_pro")
                    )
                    Row(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.auto_change_on_unlock),
                            fontSize = 13.sp,
                            color = glassColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Pro feature",
                            tint = AccentSunset,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                val isEnabled = isLiveWallpaperActive
                val isChecked = autoChangeOnUnlock && isLiveWallpaperActive

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isEnabled) {
                                Modifier.clickable { onAutoChangeOnUnlockToggle(!autoChangeOnUnlock) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 4.dp)
                        .tutorialTarget("checkbox_unlock_mode"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = if (isEnabled) {
                            { onAutoChangeOnUnlockToggle(it) }
                        } else null,
                    enabled = isEnabled,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentSunset,
                        uncheckedColor = glassColors.textSecondary
                    )
                )
                Column(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.auto_change_on_unlock),
                        fontSize = 13.sp,
                        color = if (isEnabled) glassColors.textPrimary else glassColors.textSecondary.copy(alpha = 0.6f)
                    )
                    if (!isEnabled) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.unlock_requires_live_wallpaper),
                                fontSize = 11.5.sp,
                                color = glassColors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_set_live_wallpaper),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentSunset,
                                modifier = Modifier
                                    .clickable(role = Role.Button) {
                                        onSetLiveWallpaperClick()
                                    }
                                    .padding(vertical = 2.dp)
                                    .tutorialTarget("link_set_live_wallpaper")
                            )
                        }
                    }
                }
            }
        }
    }

        // 4. Next Button
        if (isPro || activeTarget == WallpaperTarget.HOME) {
            GlassButton(
                text = "Next",
                onClick = onNextClick,
                enabled = hasPhotos,
                testTag = "btn_next",
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .tutorialTarget("btn_next")
            )

            if (!hasPhotos) {
                Text(
                    text = "Add at least one photo first",
                    fontSize = 11.5.sp,
                    color = glassColors.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoGrid(
    items: List<WallpaperItem>,
    activeTarget: WallpaperTarget,
    autoChangeOnUnlock: Boolean,
    onAddTileClick: () -> Unit,
    onRemoveItem: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            val isFirst = items.firstOrNull()?.id == item.id
            ThumbnailItem(
                item = item,
                isUnlockMode = (activeTarget == WallpaperTarget.LOCK && autoChangeOnUnlock),
                isFirst = isFirst,
                onRemove = { onRemoveItem(item.id) }
            )
        }

        // The final cell is always the Add Tile (§7.2)
        item(key = "add_tile") {
            AddTile(onClick = onAddTileClick)
        }
    }
}

@Composable
private fun ThumbnailItem(
    item: WallpaperItem,
    isUnlockMode: Boolean,
    isFirst: Boolean,
    onRemove: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 7f)
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(item.id) {
                    detectTapGestures(
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        }
                    )
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Remove from roll") {
                            onRemove()
                            true
                        }
                    )
                }
                .testTag("photo_card_${item.id}")
                .then(
                    if (isFirst) Modifier.tutorialTarget("first_photo_card") else Modifier
                ),
            shape = RoundedCornerShape(14.dp)
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = "Wallpaper frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Corner duration badge (top-end)
            val durationLabel = if (isUnlockMode) "unlock" else DurationFormatter.formatShort(item.durationSeconds)
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
                    text = durationLabel,
                    style = MonospaceBadgeStyle.copy(color = Color.White, fontSize = 10.5.sp)
                )
            }

            // Folder badge if imported via SAF folder (§7.2)
            if (item.sourceFolderUri != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = "Imported from synced folder",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Long press dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(glassColors.glassFillElevated)
                .border(1.dp, glassColors.glassBorder, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Remove from roll",
                        color = AccentSunset,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onRemove()
                },
                modifier = Modifier.testTag("action_remove_from_roll_${item.id}")
            )
        }
    }
}

@Composable
private fun AddTile(
    onClick: () -> Unit
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 7f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) { onClick() }
            .semantics { contentDescription = "Add another photo to roll" }
            .testTag("add_tile_cell")
            .tutorialTarget("add_tile_cell"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val stroke = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            drawRoundRect(
                color = AccentSunset.copy(alpha = 0.8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = stroke
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = AccentSunset
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add frame",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary
            )
        }
    }
}
