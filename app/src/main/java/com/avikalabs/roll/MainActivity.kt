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
import com.avikalabs.roll.ui.screens.PrivacyPolicyScreen
import com.avikalabs.roll.ui.screens.SettingsScreen
import com.avikalabs.roll.ui.screens.TheRollScreen
import com.avikalabs.roll.ui.theme.AccentSunset
import com.avikalabs.roll.ui.theme.LocalGlassColors
import android.app.Activity
import android.content.Intent
import com.avikalabs.roll.ui.theme.RollTheme
import com.avikalabs.roll.ui.screens.RollProScreen
import com.avikalabs.roll.wallpaper.WallpaperApplier

import androidx.compose.runtime.CompositionLocalProvider
import com.avikalabs.roll.ui.tutorial.LocalTutorialState
import com.avikalabs.roll.ui.tutorial.TutorialState
import com.avikalabs.roll.ui.tutorial.TutorialTargetRegistrar
import com.avikalabs.roll.ui.tutorial.CoachMarkOverlay
import com.avikalabs.roll.ui.tutorial.TutorialStep
import com.avikalabs.roll.ui.tutorial.TutorialTipSet
import com.avikalabs.roll.ui.tutorial.tutorialTarget

class MainActivity : ComponentActivity() {

    private val viewModel: RollViewModel by viewModels()
    private val navigateToRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val targetRoute by navigateToRoute

            val tutorialState = remember { TutorialState() }
            TutorialTargetRegistrar.state = tutorialState

            CompositionLocalProvider(LocalTutorialState provides tutorialState) {
                RollTheme(themeMode = uiState.themeMode) {
                    MainAppContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        initialRoute = targetRoute,
                        onRouteHandled = { navigateToRoute.value = null },
                        tutorialState = tutorialState
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dest = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        if (dest != null) {
            navigateToRoute.value = dest
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLiveWallpaperState()
        viewModel.refreshPurchases()
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    }
}

object NavRoutes {
    const val PICKER = "picker"
    const val DURATION_SETUP = "duration_setup/{target}"
    const val THE_ROLL = "the_roll"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacy_policy"
    const val PRO = "pro"

    fun durationSetup(target: WallpaperTarget) = "duration_setup/${target.name}"
}

@Composable
fun MainAppContent(
    uiState: RollUiState,
    viewModel: RollViewModel,
    initialRoute: String? = null,
    onRouteHandled: () -> Unit = {},
    tutorialState: TutorialState = LocalTutorialState.current
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val glassColors = LocalGlassColors.current
    val context = LocalContext.current
    val isLiveWallpaperActive by viewModel.isLiveWallpaperActive.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showActionSheet by remember { mutableStateOf(false) }

    // Handle navigation intent (e.g. from Widget or Pro shortcuts)
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrEmpty()) {
            navController.navigate(initialRoute)
            onRouteHandled()
        }
    }

    // Show banner message if set
    LaunchedEffect(uiState.bannerMessage) {
        uiState.bannerMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBanner()
        }
    }

    // Show Pro status message if set (e.g. Welcome to Roll Pro!)
    LaunchedEffect(uiState.proStatusMessage) {
        uiState.proStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearProStatusMessage()
        }
    }

    // Coach Mark Tutorial Trigger Logic
    // Never show if tutorials_disabled, or while action sheet, dialog, Pro screen, Privacy Policy screen are open.
    val isBlockedScreen = showActionSheet ||
            tutorialState.isDialogActive ||
            currentRoute == NavRoutes.PRO ||
            currentRoute == NavRoutes.PRIVACY_POLICY

    LaunchedEffect(
        currentRoute,
        isBlockedScreen,
        uiState.tutorialsDisabled,
        uiState.finishedTipSets,
        uiState.activeTarget,
        uiState.totalItemCount,
        uiState.homeItems.size,
        uiState.lockItems.size,
        uiState.isPro,
        isLiveWallpaperActive
    ) {
        if (uiState.tutorialsDisabled || isBlockedScreen) {
            if (tutorialState.currentTipSet != null && (currentRoute == NavRoutes.PRO || currentRoute == NavRoutes.PRIVACY_POLICY)) {
                tutorialState.dismiss()
            }
            return@LaunchedEffect
        }

        val finished = uiState.finishedTipSets

        // 1. PICKER SCREEN
        if (currentRoute == NavRoutes.PICKER) {
            when {
                // "picker_empty" (first launch, no photos yet on home screen)
                uiState.activeTarget == WallpaperTarget.HOME && uiState.totalItemCount == 0 && !finished.contains("picker_empty") -> {
                    tutorialState.currentTipSet = TutorialTipSet(
                        id = "picker_empty",
                        steps = listOf(
                            TutorialStep(
                                id = "step_empty_toggle",
                                targetId = "target_toggle",
                                textResId = R.string.tutorial_picker_empty_target_toggle
                            ),
                            TutorialStep(
                                id = "step_empty_add",
                                targetId = "empty_add_circle",
                                textResId = R.string.tutorial_picker_empty_add_circle
                            ),
                            TutorialStep(
                                id = "step_empty_settings",
                                targetId = "btn_settings",
                                textResId = R.string.tutorial_picker_empty_settings
                            )
                        )
                    )
                    tutorialState.currentStepIndex = 0
                }

                // "picker_photos" (the first time the Picker shows photos on home screen)
                uiState.activeTarget == WallpaperTarget.HOME && uiState.totalItemCount > 0 && !finished.contains("picker_photos") -> {
                    val addTileTextRes = if (uiState.isPro) {
                        R.string.tutorial_picker_photos_add_pro
                    } else {
                        R.string.tutorial_picker_photos_add_free
                    }
                    tutorialState.currentTipSet = TutorialTipSet(
                        id = "picker_photos",
                        steps = listOf(
                            TutorialStep(
                                id = "step_photos_first_card",
                                targetId = "first_photo_card",
                                textResId = R.string.tutorial_picker_photos_first_card
                            ),
                            TutorialStep(
                                id = "step_photos_add_tile",
                                targetId = "add_tile_cell",
                                textResId = addTileTextRes
                            ),
                            TutorialStep(
                                id = "step_photos_next",
                                targetId = "btn_next",
                                textResId = R.string.tutorial_picker_photos_next
                            ),
                            TutorialStep(
                                id = "step_photos_tab_roll",
                                targetId = "tab_roll",
                                textResId = R.string.tutorial_picker_photos_tab_roll
                            )
                        )
                    )
                    tutorialState.currentStepIndex = 0
                }

                // "picker_lock_free" (the first time a free user opens the Lock tab)
                uiState.activeTarget == WallpaperTarget.LOCK && !uiState.isPro && !finished.contains("picker_lock_free") -> {
                    tutorialState.currentTipSet = TutorialTipSet(
                        id = "picker_lock_free",
                        steps = listOf(
                            TutorialStep(
                                id = "step_lock_free_pro_card",
                                targetId = "btn_unlock_lock_screen_pro",
                                textResId = R.string.tutorial_picker_lock_free
                            )
                        )
                    )
                    tutorialState.currentStepIndex = 0
                }

                // "picker_lock_pro" (the first time a Pro user opens the Lock tab, including right after upgrading)
                uiState.activeTarget == WallpaperTarget.LOCK && uiState.isPro && !finished.contains("picker_lock_pro") -> {
                    val targetId = if (isLiveWallpaperActive) "checkbox_unlock_mode" else "link_set_live_wallpaper"
                    val textRes = if (isLiveWallpaperActive) {
                        R.string.tutorial_picker_lock_pro_unlock
                    } else {
                        R.string.tutorial_picker_lock_pro_live_wallpaper
                    }
                    tutorialState.currentTipSet = TutorialTipSet(
                        id = "picker_lock_pro",
                        steps = listOf(
                            TutorialStep(
                                id = "step_lock_pro_unlock",
                                targetId = targetId,
                                textResId = textRes
                            )
                        )
                    )
                    tutorialState.currentStepIndex = 0
                }
            }
        }

        // 2. DURATION SETUP SCREEN — "duration" (first visit)
        if (currentRoute?.startsWith("duration_setup") == true) {
            val durationItems = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeItems else uiState.lockItems
            if (durationItems.isNotEmpty() && !finished.contains("duration")) {
                tutorialState.currentTipSet = TutorialTipSet(
                    id = "duration",
                    steps = listOf(
                        TutorialStep(
                            id = "step_duration_mode",
                            targetId = "mode_per_item",
                            textResId = R.string.tutorial_duration_mode_toggle
                        ),
                        TutorialStep(
                            id = "step_duration_first_card",
                            targetId = "duration_first_card",
                            textResId = R.string.tutorial_duration_first_card
                        ),
                        TutorialStep(
                            id = "step_duration_apply_all",
                            targetId = "btn_apply_to_all",
                            textResId = R.string.tutorial_duration_apply_to_all
                        ),
                        TutorialStep(
                            id = "step_duration_next",
                            targetId = "btn_duration_next",
                            textResId = R.string.tutorial_duration_next
                        )
                    )
                )
                tutorialState.currentStepIndex = 0
            }
        }

        // 3. THE ROLL SCREEN — "roll" (first visit)
        if (currentRoute == NavRoutes.THE_ROLL) {
            val rollItems = if (uiState.activeTarget == WallpaperTarget.HOME) uiState.homeItems else uiState.lockItems
            if (rollItems.isNotEmpty() && !finished.contains("roll") && !tutorialState.isDialogActive) {
                val playbackTextRes = if (uiState.isPro) {
                    R.string.tutorial_roll_playback_pro
                } else {
                    R.string.tutorial_roll_playback_free
                }
                tutorialState.currentTipSet = TutorialTipSet(
                    id = "roll",
                    steps = listOf(
                        TutorialStep(
                            id = "step_roll_playback",
                            targetId = "playback_sequential",
                            textResId = playbackTextRes
                        ),
                        TutorialStep(
                            id = "step_roll_drag_handle",
                            targetId = "drag_handle_0",
                            textResId = R.string.tutorial_roll_drag_handle
                        ),
                        TutorialStep(
                            id = "step_roll_options",
                            targetId = "options_button_0",
                            textResId = R.string.tutorial_roll_options
                        ),
                        TutorialStep(
                            id = "step_roll_add_frame",
                            targetId = "the_roll_add_frame_tile",
                            textResId = R.string.tutorial_roll_add_frame
                        ),
                        TutorialStep(
                            id = "step_roll_execute",
                            targetId = "btn_execute",
                            textResId = R.string.tutorial_roll_execute
                        )
                    )
                )
                tutorialState.currentStepIndex = 0
            }
        }

        // 4. SETTINGS SCREEN — "settings" (first visit)
        if (currentRoute == NavRoutes.SETTINGS) {
            if (!finished.contains("settings")) {
                val proRowTextRes = if (uiState.isPro) {
                    R.string.tutorial_settings_pro_active
                } else {
                    R.string.tutorial_settings_pro_free
                }
                tutorialState.currentTipSet = TutorialTipSet(
                    id = "settings",
                    steps = listOf(
                        TutorialStep(
                            id = "step_settings_pro",
                            targetId = "btn_settings_unlock_pro",
                            textResId = proRowTextRes
                        ),
                        TutorialStep(
                            id = "step_settings_live_wallpaper",
                            targetId = "btn_set_live_wallpaper",
                            textResId = R.string.tutorial_settings_live_wallpaper
                        ),
                        TutorialStep(
                            id = "step_settings_double_tap",
                            targetId = "checkbox_double_tap",
                            textResId = R.string.tutorial_settings_double_tap
                        ),
                        TutorialStep(
                            id = "step_settings_restore",
                            targetId = "row_restore_purchases",
                            textResId = R.string.tutorial_settings_restore
                        )
                    )
                )
                tutorialState.currentStepIndex = 0
            }
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
                if (currentRoute != NavRoutes.PRIVACY_POLICY && currentRoute != NavRoutes.PRO) {
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
                }
            },
            bottomBar = {
                // Persistent navigation exposes Picker & The Roll once at least 1 photo exists (§6)
                val hasAnyPhotos = uiState.totalItemCount > 0
                val showBottomNav = hasAnyPhotos &&
                        currentRoute != NavRoutes.SETTINGS &&
                        currentRoute != NavRoutes.PRIVACY_POLICY &&
                        currentRoute != NavRoutes.PRO

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
                        onRemoveItem = { viewModel.removeItem(it, uiState.activeTarget) },
                        onNextClick = {
                            if (uiState.activeTarget == WallpaperTarget.LOCK && activeConfig.autoChangeOnUnlock && isLiveWallpaperActive) {
                                // Skip Duration setup per PRD §6 & §7.2
                                navController.navigate(NavRoutes.THE_ROLL)
                            } else {
                                navController.navigate(NavRoutes.durationSetup(uiState.activeTarget))
                            }
                        },
                        isPro = uiState.isPro,
                        onNavigatePro = { navController.navigate(NavRoutes.PRO) }
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
                        onExecuteClick = { viewModel.executeRollSetup(uiState.activeTarget) },
                        isPro = uiState.isPro,
                        onNavigatePro = { navController.navigate(NavRoutes.PRO) }
                    )
                }

                // SETTINGS SCREEN
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        currentThemeMode = uiState.themeMode,
                        onThemeModeChanged = { viewModel.setThemeMode(it) },
                        homeDoubleTapEnabled = uiState.homeDoubleTapEnabled,
                        onHomeDoubleTapToggle = { viewModel.setHomeDoubleTapEnabled(it) },
                        onNavigatePrivacyPolicy = { navController.navigate(NavRoutes.PRIVACY_POLICY) },
                        isPro = uiState.isPro,
                        onNavigatePro = { navController.navigate(NavRoutes.PRO) },
                        onRestorePurchases = { viewModel.restorePurchases() },
                        onResetTutorials = { viewModel.resetTutorials() }
                    )
                }

                // PRIVACY POLICY SCREEN
                composable(NavRoutes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // ROLL PRO SCREEN
                composable(NavRoutes.PRO) {
                    RollProScreen(
                        isPro = uiState.isPro,
                        formattedPrice = uiState.proPrice,
                        isBillingAvailable = uiState.isBillingAvailable,
                        statusMessage = uiState.proStatusMessage,
                        onUnlockClick = {
                            (context as? Activity)?.let { activity ->
                                viewModel.launchPurchase(activity)
                            }
                        },
                        onRestorePurchasesClick = {
                            viewModel.restorePurchases()
                        },
                        onCloseClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Add Photo Action Sheet Modal
        if (showActionSheet) {
            AddPhotoActionSheet(
                onDismiss = { showActionSheet = false },
                onPhotosSelected = { uris ->
                    val isPro = uiState.isPro
                    val currentCount = uiState.totalItemCount
                    viewModel.addPhotos(uiState.activeTarget, uris)
                    if (!isPro && currentCount + uris.size > 10) {
                        navController.navigate(NavRoutes.PRO)
                    }
                },
                onFolderSelected = { uri, name ->
                    viewModel.importFolder(uiState.activeTarget, uri, name)
                },
                isPro = uiState.isPro,
                onNavigatePro = {
                    showActionSheet = false
                    navController.navigate(NavRoutes.PRO)
                }
            )
        }

        // First-run Coach Mark Tutorial Overlay
        if (!uiState.tutorialsDisabled && !isBlockedScreen && tutorialState.currentTipSet != null) {
            CoachMarkOverlay(
                tutorialState = tutorialState,
                onStepCompleted = { tipSetId ->
                    viewModel.markTipSetFinished(tipSetId)
                },
                onNeverShowAgain = {
                    tutorialState.currentTipSet?.id?.let { viewModel.markTipSetFinished(it) }
                    viewModel.setTutorialsDisabled(true)
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
                    .tutorialTarget("btn_settings")
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
                            .testTag("nav_${label.lowercase().replace(" ", "_")}")
                            .then(
                                if (route == NavRoutes.THE_ROLL) Modifier.tutorialTarget("tab_roll") else Modifier
                            ),
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
