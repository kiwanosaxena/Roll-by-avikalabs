package com.avikalabs.roll.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.RotationConfig
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.ui.components.DurationFormatter
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.components.GlassButton
import com.avikalabs.roll.ui.components.GlassSegmentedToggle
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import com.avikalabs.roll.ui.theme.MonospaceBadgeStyle
import java.util.Collections

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TheRollScreen(
    activeTarget: WallpaperTarget,
    items: List<WallpaperItem>,
    config: RotationConfig,
    onTargetSelected: (WallpaperTarget) -> Unit,
    onPlaybackOrderChanged: (PlaybackOrder) -> Unit,
    onReorderItems: (List<WallpaperItem>) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddFrameClick: () -> Unit,
    onExecuteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    // Local mutable list for real-time smooth drag and drop
    var currentItems by remember(items, activeTarget) { mutableStateOf(items) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    val rowSpacingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val step = (if (rowHeightPx > 0f) rowHeightPx else 180f) + rowSpacingPx
    var showRollSetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(items, activeTarget) {
        currentItems = items
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Target Toggle (Home / Lock)
        GlassSegmentedToggle(
            selectedTarget = activeTarget,
            onTargetSelected = onTargetSelected,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Playback Order Toggle (In order / Shuffle) (§7.4)
        PlaybackOrderToggle(
            currentOrder = config.playbackOrder,
            onOrderSelected = onPlaybackOrderChanged,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Vertical List of Roll Items
        Box(modifier = Modifier.weight(1f)) {
            if (currentItems.isEmpty()) {
                EmptyScreenContent(
                    activeTarget = activeTarget,
                    onTargetSelected = onTargetSelected,
                    autoChangeOnUnlock = config.autoChangeOnUnlock,
                    onAutoChangeOnUnlockToggle = {},
                    onAddClick = onAddFrameClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = currentItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isShowingNow = (index == config.currentIndex.coerceIn(0, currentItems.size - 1))
                        val isBeingDragged = item.id == draggingId

                        RollRowItem(
                            index = index,
                            totalCount = currentItems.size,
                            item = item,
                            isShowingNow = isShowingNow,
                            isUnlockMode = (activeTarget == WallpaperTarget.LOCK && config.autoChangeOnUnlock),
                            isBeingDragged = isBeingDragged,
                            dragOffsetY = if (isBeingDragged) dragOffsetY else 0f,
                            onDragStart = {
                                draggingId = item.id
                                dragOffsetY = 0f
                            },
                            onDrag = { deltaY ->
                                dragOffsetY += deltaY
                                var from = currentItems.indexOfFirst { it.id == draggingId }
                                if (from != -1) {
                                    val list = currentItems.toMutableList()
                                    var moved = false
                                    while (dragOffsetY > step / 2 && from < list.size - 1) {
                                        list.add(from + 1, list.removeAt(from))
                                        from++
                                        dragOffsetY -= step
                                        moved = true
                                    }
                                    while (dragOffsetY < -step / 2 && from > 0) {
                                        list.add(from - 1, list.removeAt(from))
                                        from--
                                        dragOffsetY += step
                                        moved = true
                                    }
                                    if (moved) {
                                        currentItems = list
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingId != null) {
                                    draggingId = null
                                    dragOffsetY = 0f
                                    onReorderItems(currentItems)
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val mutable = currentItems.toMutableList()
                                    Collections.swap(mutable, index, index - 1)
                                    currentItems = mutable
                                    onReorderItems(mutable)
                                }
                            },
                            onMoveDown = {
                                if (index < currentItems.size - 1) {
                                    val mutable = currentItems.toMutableList()
                                    Collections.swap(mutable, index, index + 1)
                                    currentItems = mutable
                                    onReorderItems(mutable)
                                }
                            },
                            onDelete = {
                                onRemoveItem(item.id)
                            },
                            modifier = Modifier
                                .then(if (isBeingDragged) Modifier else Modifier.animateItem())
                                .onSizeChanged { if (it.height > 0) rowHeightPx = it.height.toFloat() }
                                .zIndex(if (isBeingDragged) 10f else 1f)
                        )
                    }

                    // Dashed Add Frame to Roll Affordance at Bottom (§7.4)
                    item(key = "add_frame_bottom") {
                        AddFrameBottomTile(onClick = onAddFrameClick)
                    }
                }
            }
        }

        // Execute Button to complete setup and start roll
        Spacer(modifier = Modifier.height(10.dp))
        GlassButton(
            text = "Execute",
            icon = Icons.Outlined.Check,
            onClick = {
                onExecuteClick()
                showRollSetDialog = true
            },
            enabled = currentItems.isNotEmpty(),
            testTag = "btn_execute",
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showRollSetDialog) {
        RollSetDialog(
            target = activeTarget,
            onDismiss = { showRollSetDialog = false }
        )
    }
}

@Composable
private fun RollRowItem(
    index: Int,
    totalCount: Int,
    item: WallpaperItem,
    isShowingNow: Boolean,
    isUnlockMode: Boolean,
    isBeingDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    val glassColors = LocalGlassColors.current
    var showMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isBeingDragged) 1.03f else 1f,
        label = "dragScale"
    )

    val durationText = if (isUnlockMode) "on unlock" else DurationFormatter.formatShort(item.durationSeconds)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isBeingDragged) Modifier.shadow(8.dp, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (isBeingDragged) glassColors.glassFillElevated else glassColors.glassFill)
            .border(
                width = if (isShowingNow) 2.dp else 1.dp,
                color = if (isShowingNow) AccentSunset else glassColors.glassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                customActions = listOfNotNull(
                    if (index > 0) CustomAccessibilityAction("Move up") { onMoveUp(); true } else null,
                    if (index < totalCount - 1) CustomAccessibilityAction("Move down") { onMoveDown(); true } else null,
                    CustomAccessibilityAction("Remove from roll") { onDelete(); true }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle (§7.4.1 - Scoped to handle with detectDragGestures)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { latestOnDragStart() },
                            onDragEnd = { latestOnDragEnd() },
                            onDragCancel = { latestOnDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                latestOnDrag(dragAmount.y)
                            }
                        )
                    }
                    .semantics { contentDescription = "Drag handle for frame ${index + 1}" }
                    .testTag("drag_handle_$index"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    tint = if (isBeingDragged) AccentSunset else glassColors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Thumbnail (Small 48dp rounded square)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Frame Name & Duration
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Frame ${index + 1}",
                        fontSize = 14.sp,
                        fontWeight = if (isShowingNow) FontWeight.Bold else FontWeight.Medium,
                        color = glassColors.textPrimary
                    )
                    if (isShowingNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentSunset.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NOW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentSunset
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = durationText,
                    style = MonospaceBadgeStyle.copy(
                        color = if (isShowingNow) AccentSunset else glassColors.textSecondary,
                        fontSize = 11.5.sp
                    )
                )
            }

            // Context Menu button (3 dots) for accessible reorder and delete
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options for frame ${index + 1}",
                        tint = glassColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(glassColors.glassFillElevated)
                ) {
                    if (index > 0) {
                        DropdownMenuItem(
                            text = { Text("Move up", color = glassColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.ArrowUpward, null, tint = glassColors.textSecondary) },
                            onClick = {
                                showMenu = false
                                onMoveUp()
                            }
                        )
                    }
                    if (index < totalCount - 1) {
                        DropdownMenuItem(
                            text = { Text("Move down", color = glassColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.ArrowDownward, null, tint = glassColors.textSecondary) },
                            onClick = {
                                showMenu = false
                                onMoveDown()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove from roll", color = Color(0xFFE53935)) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFE53935)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFrameBottomTile(onClick: () -> Unit) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) { onClick() }
            .testTag("the_roll_add_frame_tile"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val stroke = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            drawRoundRect(
                color = AccentSunset.copy(alpha = 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = stroke
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = AccentSunset
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add frame to roll",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary
            )
        }
    }
}

@Composable
private fun PlaybackOrderToggle(
    currentOrder: PlaybackOrder,
    onOrderSelected: (PlaybackOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    GlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(3.dp),
        shape = RoundedCornerShape(21.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val orders = listOf(
                PlaybackOrder.SEQUENTIAL to "In order",
                PlaybackOrder.SHUFFLE to "Shuffle"
            )

            orders.forEach { (order, label) ->
                val isSelected = order == currentOrder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) AccentSunset else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onOrderSelected(order) }
                        .testTag("playback_${order.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (order == PlaybackOrder.SHUFFLE) {
                            Icon(
                                imageVector = Icons.Outlined.Shuffle,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else glassColors.textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else glassColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RollSetDialog(
    target: WallpaperTarget,
    onDismiss: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val targetName = if (target == WallpaperTarget.HOME) "Home screen" else "Lock screen"

    Dialog(onDismissRequest = onDismiss) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, AccentSunset.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("dialog_roll_set"),
            shape = RoundedCornerShape(24.dp),
            elevated = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success glowing checkmark badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentSunset.copy(alpha = 0.15f))
                        .border(1.5.dp, AccentSunset, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = AccentSunset,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Roll set.",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your wallpaper roll for $targetName is now active and rotating.",
                    fontSize = 13.5.sp,
                    color = glassColors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassButton(
                    text = "Done",
                    onClick = onDismiss,
                    testTag = "btn_dialog_done",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
