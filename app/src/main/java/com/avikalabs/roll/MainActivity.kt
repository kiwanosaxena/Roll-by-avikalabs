package com.avikalabs.roll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.ui.RollUiState
import com.avikalabs.roll.ui.RollViewModel
import com.avikalabs.roll.ui.components.AddPhotoActionSheet
import com.avikalabs.roll.ui.components.GlassBox
import com.avikalabs.roll.ui.screens.DurationSetupScreen
import com.avikalabs.roll.ui.screens.PickerScreen
import com.avikalabs.roll.ui.screens.SettingsScreen
import com.avikalabs.roll.ui.screens.TheRollScreen
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import com.avikalabs.roll.ui.theme.RollTheme
import com.avikalabs.roll.wallpaper.WallpaperApplier

class MainActivity : ComponentActivity() {

    private val viewModel: RollViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            RollTheme(themeMode = uiState.themeMode) {
                MainAppContent(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLiveWallpaperState()
    }
}

object NavRoutes {
    const val PICKER = "picker"
    const val DURATION_SETUP = "duration_setup/{target}"
    const val THE_ROLL = "the_roll"
    const val SETTINGS = "settings"

    fun durationSetup(target: WallpaperTarget) = "duration_setup/${target.name}"
}

@Composable
fun MainAppContent(
    uiState: RollUiState,
    viewModel: RollViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val glassColors = LocalGlassColors.current
    val context = LocalContext.current
    val isLiveWallpaperActive by viewModel.isLiveWallpaperActive.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showActionSheet by remember { mutableStateOf(false) }

    // Show banner message if set
    LaunchedEffect(uiState.bannerMessage) {
        uiState.bannerMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBanner()
        }
    }

    // Root Box with fixed glass background gradient (§8.1)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glassColors.backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = glassColors.textPrimary,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                RollTopAppBar(
                    currentRoute = currentRoute,
                    activeTarget = uiState.activeTarget,
                    selectedCount = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeItems.size else uiState.lockItems.size,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateSettings = {
                        if (currentRoute != NavRoutes.SETTINGS) {
                            navController.navigate(NavRoutes.SETTINGS)
                        }
                    }
                )
            },
            bottomBar = {
                // Persistent navigation exposes Picker & The Roll once at least 1 photo exists (§6)
                val hasAnyPhotos = uiState.totalItemCount > 0
                val showBottomNav = hasAnyPhotos && currentRoute != NavRoutes.SETTINGS

                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RollBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route == NavRoutes.PICKER) {
                                val popped = navController.popBackStack(NavRoutes.PICKER, inclusive = false)
                                if (!popped && currentRoute != NavRoutes.PICKER) {
                                    navController.navigate(NavRoutes.PICKER) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else if (route == NavRoutes.THE_ROLL) {
                                if (currentRoute != NavRoutes.THE_ROLL) {
                                    navController.navigate(NavRoutes.THE_ROLL) {
                                        popUpTo(NavRoutes.PICKER) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else {
                                navController.navigate(route)
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.PICKER,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // PICKER SCREEN
                composable(NavRoutes.PICKER) {
                    val activeItems = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeItems else uiState.lockItems
                    val activeConfig = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeConfig else uiState.lockConfig

                    PickerScreen(
                        activeTarget = uiState.activeTarget,
                        items = activeItems,
                        autoChangeOnUnlock = activeConfig.autoChangeOnUnlock,
                        isLiveWallpaperActive = isLiveWallpaperActive,
                        onTargetSelected = { viewModel.setActiveTarget(it) },
                        onAutoChangeOnUnlockToggle = { viewModel.setAutoChangeOnUnlock(it) },
                        onSetLiveWallpaperClick = {
                            WallpaperApplier.launchLiveWallpaperPicker(context)
                        },
                        onAddTileClick = { showActionSheet = true },
                        onNextClick = {
                            if (uiState.activeTarget == WallpaperTarget.LOCK && activeConfig.autoChangeOnUnlock && isLiveWallpaperActive) {
                                // Skip Duration setup per PRD §6 & §7.2
                                navController.navigate(NavRoutes.THE_ROLL)
                            } else {
                                navController.navigate(NavRoutes.durationSetup(uiState.activeTarget))
                            }
                        }
                    )
                }

                // DURATION SETUP SCREEN
                composable(
                    route = NavRoutes.DURATION_SETUP,
                    arguments = listOf(navArgument("target") { type = NavType.StringType })
                ) { backStackEntry ->
                    val targetName = backStackEntry.arguments?.getString("target") ?: WallpaperTarget.HOME.name
                    val target = try { WallpaperTarget.valueOf(targetName) } catch (_: Exception) { WallpaperTarget.HOME }

                    val items = if (target == WallpaperTarget.HOME) uiState.homeItems else uiState.lockItems
                    val config = if (target == WallpaperTarget.HOME) uiState.homeConfig else uiState.lockConfig

                    DurationSetupScreen(
                        target = target,
                        items = items,
                        config = config,
                        onDurationModeChanged = { viewModel.setDurationMode(target, it) },
                        onItemDurationChanged = { id, dur -> viewModel.updateItemDuration(id, dur) },
                        onApplyGlobalDuration = { dur -> viewModel.applyGlobalDuration(target, dur) },
                        onNextClick = { navController.navigate(NavRoutes.THE_ROLL) }
                    )
                }

                // THE ROLL SCREEN
                composable(NavRoutes.THE_ROLL) {
                    val items = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeItems else uiState.lockItems
                    val config = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeConfig else uiState.lockConfig

                    TheRollScreen(
                        activeTarget = uiState.activeTarget,
                        items = items,
                        config = config,
                        onTargetSelected = { viewModel.setActiveTarget(it) },
                        onPlaybackOrderChanged = { viewModel.setPlaybackOrder(uiState.activeTarget, it) },
                        onReorderItems = { viewModel.reorderItems(uiState.activeTarget, it) },
                        onRemoveItem = { viewModel.removeItem(it, uiState.activeTarget) },
                        onAddFrameClick = { showActionSheet = true },
                        onExecuteClick = { viewModel.executeRollSetup(uiState.activeTarget) }
                    )
                }

                // SETTINGS SCREEN
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        currentThemeMode = uiState.themeMode,
                        onThemeModeChanged = { viewModel.setThemeMode(it) },
                        homeDoubleTapEnabled = uiState.homeDoubleTapEnabled,
                        onHomeDoubleTapToggle = { viewModel.setHomeDoubleTapEnabled(it) }
                    )
                }
            }
        }

        // Add Photo Action Sheet Modal
        if (showActionSheet) {
            AddPhotoActionSheet(
                onDismiss = { showActionSheet = false },
                onPhotosSelected = { uris ->
                    viewModel.addPhotos(uiState.activeTarget, uris)
                },
                onFolderSelected = { uri, name ->
                    viewModel.importFolder(uiState.activeTarget, uri, name)
                }
            )
        }
    }
}

@Composable
fun RollTopAppBar(
    currentRoute: String?,
    activeTarget: WallpaperTarget,
    selectedCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val isRootScreen = currentRoute == NavRoutes.PICKER || currentRoute == NavRoutes.THE_ROLL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRootScreen) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = glassColors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = when (currentRoute) {
                NavRoutes.SETTINGS -> "Settings"
                NavRoutes.THE_ROLL -> "The Roll"
                else -> if (currentRoute?.startsWith("duration_setup") == true) "Duration setup" else "Roll"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = glassColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        // Live count on Picker screen (§7.2)
        if (currentRoute == NavRoutes.PICKER && selectedCount > 0) {
            Text(
                text = "$selectedCount selected",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textSecondary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Settings gear icon (§7.6)
        if (currentRoute != NavRoutes.SETTINGS) {
            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .testTag("btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = glassColors.textPrimary
                )
            }
        }
    }
}

@Composable
fun RollBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassBox(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(28.dp),
            elevated = true
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    NavRoutes.PICKER to Pair("Picker", Icons.Outlined.PhotoLibrary),
                    NavRoutes.THE_ROLL to Pair("The Roll", Icons.Outlined.ViewCarousel)
                )

                navItems.forEach { (route, pair) ->
                    val (label, icon) = pair
                    val isSelected = (route == NavRoutes.PICKER && (currentRoute == NavRoutes.PICKER || currentRoute?.startsWith("duration_setup") == true)) ||
                            (route == NavRoutes.THE_ROLL && currentRoute == NavRoutes.THE_ROLL)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) AccentSunset else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) { onNavigate(route) }
                            .testTag("nav_${label.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else glassColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else glassColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
