package com.avikalabs.roll

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.avikalabs.roll.data.local.RollDatabase
import com.avikalabs.roll.data.local.RollPreferences
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.MainAppContent
import com.avikalabs.roll.ui.RollViewModel
import com.avikalabs.roll.ui.theme.RollTheme
import com.avikalabs.roll.ui.tutorial.LocalTutorialState
import com.avikalabs.roll.ui.tutorial.TutorialState
import com.avikalabs.roll.ui.tutorial.TutorialTargetRegistrar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TutorialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var app: Application
    private lateinit var repository: RollRepository
    private lateinit var preferences: RollPreferences

    @Before
    fun setup() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            app = ApplicationProvider.getApplicationContext()
            val dao = RollDatabase.getInstance(context).rollDao()
            dao.clearItemsByTarget(WallpaperTarget.HOME)
            dao.clearItemsByTarget(WallpaperTarget.LOCK)
            repository = RollRepository.getInstance(context)
            preferences = RollPreferences(context)
            repository.resetTutorials()
        }
    }

    @Test
    fun `The first launch shows the picker_empty tips`() {
        runBlocking {
            repository.resetTutorials()
            val fakeBilling = FakeBillingManager(initialPro = false)
            val viewModel = RollViewModel(app, fakeBilling)
            val tutorialState = TutorialState()
            TutorialTargetRegistrar.state = tutorialState

            composeTestRule.setContent {
                val uiState by viewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides tutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            tutorialState = tutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            assertEquals("picker_empty", tutorialState.currentTipSet?.id)
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertExists()
            composeTestRule.onNodeWithText(context.getString(R.string.tutorial_picker_empty_target_toggle)).assertExists()
        }
    }

    @Test
    fun `After Got it that tip set does not show again even after the activity is recreated`() {
        runBlocking {
            repository.resetTutorials()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity {
                assertTrue(preferences.getFinishedTipSetsSync().isEmpty())
            }

            // User finishes tip set (e.g. clicking Got it)
            repository.markTipSetFinished("picker_empty")

            // Recreate activity
            scenario.recreate()
            scenario.onActivity {
                val finished = preferences.getFinishedTipSetsSync()
                assertTrue(finished.contains("picker_empty"))
            }

            val fakeBilling = FakeBillingManager(initialPro = false)
            val viewModel = RollViewModel(app, fakeBilling)
            val tutorialState = TutorialState()
            TutorialTargetRegistrar.state = tutorialState

            composeTestRule.setContent {
                val uiState by viewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides tutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            tutorialState = tutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            assertNotEquals("picker_empty", tutorialState.currentTipSet?.id)
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertDoesNotExist()
            scenario.close()
        }
    }

    @Test
    fun `Never show tips again stops tips on every screen`() {
        runBlocking {
            repository.setTutorialsDisabled(true)
            val fakeBilling = FakeBillingManager(initialPro = false)
            val viewModel = RollViewModel(app, fakeBilling)
            val tutorialState = TutorialState()
            TutorialTargetRegistrar.state = tutorialState

            composeTestRule.setContent {
                val uiState by viewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides tutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            tutorialState = tutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            assertNull(tutorialState.currentTipSet)
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertDoesNotExist()
        }
    }

    @Test
    fun `Show tips again brings them back`() {
        runBlocking {
            repository.markTipSetFinished("picker_empty")
            repository.setTutorialsDisabled(true)
            assertTrue(preferences.isTutorialDisabledSync())
            assertTrue(preferences.getFinishedTipSetsSync().contains("picker_empty"))

            val fakeBilling = FakeBillingManager(initialPro = false)
            val viewModel = RollViewModel(app, fakeBilling)
            viewModel.resetTutorials()

            composeTestRule.waitForIdle()
            assertFalse(preferences.isTutorialDisabledSync())
            assertTrue(preferences.getFinishedTipSetsSync().isEmpty())
            assertEquals(context.getString(R.string.settings_tips_will_show_again), viewModel.bannerMessage.value)
        }
    }

    @Test
    fun `The overlay does not appear while the add-photo sheet or the Roll set dialog is open`() {
        runBlocking {
            repository.resetTutorials()
            val fakeBilling = FakeBillingManager(initialPro = false)
            val viewModel = RollViewModel(app, fakeBilling)
            val tutorialState = TutorialState()
            TutorialTargetRegistrar.state = tutorialState

            // Dialog is open
            tutorialState.isDialogActive = true

            composeTestRule.setContent {
                val uiState by viewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides tutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            tutorialState = tutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertDoesNotExist()
        }
    }

    @Test
    fun `A free user sees picker_lock_free on the Lock tab`() {
        runBlocking {
            repository.resetTutorials()
            val freeBilling = FakeBillingManager(initialPro = false)
            val freeViewModel = RollViewModel(app, freeBilling)
            freeViewModel.setActiveTarget(WallpaperTarget.LOCK)
            val freeTutorialState = TutorialState()
            TutorialTargetRegistrar.state = freeTutorialState

            composeTestRule.setContent {
                val uiState by freeViewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides freeTutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = freeViewModel,
                            tutorialState = freeTutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            assertEquals("picker_lock_free", freeTutorialState.currentTipSet?.id)
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertExists()
            composeTestRule.onNodeWithText(context.getString(R.string.tutorial_picker_lock_free)).assertExists()
        }
    }

    @Test
    fun `A Pro user sees picker_lock_pro on the Lock tab`() {
        runBlocking {
            repository.resetTutorials()
            val proBilling = FakeBillingManager(initialPro = true)
            val proViewModel = RollViewModel(app, proBilling)
            proViewModel.setActiveTarget(WallpaperTarget.LOCK)
            val proTutorialState = TutorialState()
            TutorialTargetRegistrar.state = proTutorialState

            composeTestRule.setContent {
                val uiState by proViewModel.uiState.collectAsState()
                CompositionLocalProvider(LocalTutorialState provides proTutorialState) {
                    RollTheme {
                        MainAppContent(
                            uiState = uiState,
                            viewModel = proViewModel,
                            tutorialState = proTutorialState
                        )
                    }
                }
            }

            composeTestRule.waitForIdle()
            assertEquals("picker_lock_pro", proTutorialState.currentTipSet?.id)
            composeTestRule.onNodeWithTag("coach_mark_overlay").assertExists()
        }
    }
}
