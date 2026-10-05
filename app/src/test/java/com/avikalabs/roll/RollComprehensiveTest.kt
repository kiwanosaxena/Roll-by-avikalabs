package com.avikalabs.roll

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.avikalabs.roll.data.local.RollDatabase
import com.avikalabs.roll.data.local.ThemeMode
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.receiver.UnlockReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RollComprehensiveTest {

    private lateinit var context: Context
    private lateinit var repository: RollRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val dao = RollDatabase.getInstance(context).rollDao()
        dao.clearItemsByTarget(WallpaperTarget.HOME)
        dao.clearItemsByTarget(WallpaperTarget.LOCK)
        repository = RollRepository.getInstance(context)
    }

    @Test
    fun `test app application name resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Roll", appName)
    }

    @Test
    fun `test repository item insertion and target separation`() = runBlocking {
        val homeItem = WallpaperItem(
            id = "test_home_1",
            uri = "content://media/external/images/media/101",
            target = WallpaperTarget.HOME,
            durationSeconds = 60,
            sortIndex = 0
        )
        val lockItem = WallpaperItem(
            id = "test_lock_1",
            uri = "content://media/external/images/media/102",
            target = WallpaperTarget.LOCK,
            durationSeconds = 120,
            sortIndex = 0
        )

        repository.addItems(listOf(homeItem))
        repository.addItems(listOf(lockItem))

        val homeItems = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        val lockItems = repository.getItemsByTargetSync(WallpaperTarget.LOCK)

        assertTrue(homeItems.any { it.id == "test_home_1" })
        assertTrue(lockItems.any { it.id == "test_lock_1" })
        assertEquals(WallpaperTarget.HOME, homeItems.first { it.id == "test_home_1" }.target)
        assertEquals(WallpaperTarget.LOCK, lockItems.first { it.id == "test_lock_1" }.target)
    }

    @Test
    fun `test auto change on unlock configuration update`() = runBlocking {
        repository.setAutoChangeOnUnlock(true)
        val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
        assertNotNull(lockConfig)
        assertTrue(lockConfig!!.autoChangeOnUnlock)

        repository.setAutoChangeOnUnlock(false)
        val updatedConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
        assertNotNull(updatedConfig)
        assertFalse(updatedConfig!!.autoChangeOnUnlock)
    }

    @Test
    fun `test playback order configuration`() = runBlocking {
        repository.setPlaybackOrder(WallpaperTarget.HOME, PlaybackOrder.SHUFFLE)
        var config = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertNotNull(config)
        assertEquals(PlaybackOrder.SHUFFLE, config!!.playbackOrder)

        repository.setPlaybackOrder(WallpaperTarget.HOME, PlaybackOrder.SEQUENTIAL)
        config = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertNotNull(config)
        assertEquals(PlaybackOrder.SEQUENTIAL, config!!.playbackOrder)
    }

    @Test
    fun `test preferences theme mode and double tap`() = runBlocking {
        repository.setThemeMode(ThemeMode.DAY)
        repository.setHomeDoubleTapEnabled(true)
        assertTrue(repository.isHomeDoubleTapEnabledSync())

        repository.setHomeDoubleTapEnabled(false)
        assertFalse(repository.isHomeDoubleTapEnabledSync())
    }

    @Test
    fun `test unlock receiver does not crash on user present broadcast`() {
        val receiver = UnlockReceiver()
        val intent = Intent(Intent.ACTION_USER_PRESENT)
        // Should handle broadcast gracefully and safely without throwing exceptions
        receiver.onReceive(context, intent)
    }

    @Test
    fun `test disableUnlockModeIfLiveWallpaperInactive switches unlock off when not active`() = runBlocking {
        repository.setAutoChangeOnUnlock(true)
        val before = repository.getRotationConfigSync(WallpaperTarget.LOCK)
        assertTrue(before!!.autoChangeOnUnlock)

        repository.disableUnlockModeIfLiveWallpaperInactive()
        val after = repository.getRotationConfigSync(WallpaperTarget.LOCK)
        assertFalse(after!!.autoChangeOnUnlock)
    }

    @Test
    fun `test reordering and removing items in repository`() = runBlocking {
        val item1 = WallpaperItem(id = "item_a", uri = "uri_a", target = WallpaperTarget.HOME, sortIndex = 0)
        val item2 = WallpaperItem(id = "item_b", uri = "uri_b", target = WallpaperTarget.HOME, sortIndex = 1)
        repository.addItems(listOf(item1, item2))

        val reordered = listOf(item2.copy(sortIndex = 0), item1.copy(sortIndex = 1))
        repository.reorderItems(WallpaperTarget.HOME, reordered)

        val itemsAfterReorder = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals("item_b", itemsAfterReorder[0].id)
        assertEquals("item_a", itemsAfterReorder[1].id)

        // Remove item
        repository.removeItem("item_a", WallpaperTarget.HOME)
        val itemsAfterRemove = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertFalse(itemsAfterRemove.any { it.id == "item_a" })
    }

    @Test
    fun `test removing photo before current shifts position`() = runBlocking {
        val item0 = WallpaperItem(id = "p0", uri = "uri0", target = WallpaperTarget.HOME, sortIndex = 0)
        val item1 = WallpaperItem(id = "p1", uri = "uri1", target = WallpaperTarget.HOME, sortIndex = 1)
        val item2 = WallpaperItem(id = "p2", uri = "uri2", target = WallpaperTarget.HOME, sortIndex = 2)
        repository.addItems(listOf(item0, item1, item2))

        val dao = RollDatabase.getInstance(context).rollDao()
        dao.updateCurrentIndex(WallpaperTarget.HOME, 2)

        repository.removeItem("p0", WallpaperTarget.HOME)

        val config = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertNotNull(config)
        assertEquals(1, config!!.currentIndex)
        val remaining = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals("p2", remaining[config.currentIndex].id)
    }

    @Test
    fun `test removing photo currently showing updates current position`() = runBlocking {
        val item0 = WallpaperItem(id = "x0", uri = "uri0", target = WallpaperTarget.HOME, sortIndex = 0)
        val item1 = WallpaperItem(id = "x1", uri = "uri1", target = WallpaperTarget.HOME, sortIndex = 1)
        repository.addItems(listOf(item0, item1))

        val dao = RollDatabase.getInstance(context).rollDao()
        dao.updateCurrentIndex(WallpaperTarget.HOME, 0)

        repository.removeItem("x0", WallpaperTarget.HOME)

        val config = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertNotNull(config)
        assertEquals(0, config!!.currentIndex)
        val remaining = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals(1, remaining.size)
        assertEquals("x1", remaining[0].id)
    }

    @get:org.junit.Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun `test widget action receiver action string`() {
        assertEquals("com.avikalabs.roll.ACTION_WIDGET_ADVANCE", com.avikalabs.roll.widget.RollWidgetActionReceiver.ACTION_WIDGET_ADVANCE)
    }

    @Test
    fun `test main activity launch`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @Test
    fun `test duration setup screen rendering and interaction`() {
        val items = listOf(
            WallpaperItem(id = "item_1", uri = "content://media/1", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 0),
            WallpaperItem(id = "item_2", uri = "content://media/2", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 1)
        )
        val config = com.avikalabs.roll.data.model.RotationConfig(WallpaperTarget.HOME)
        var nextClicked = false
        composeTestRule.setContent {
            com.avikalabs.roll.ui.screens.DurationSetupScreen(
                target = WallpaperTarget.HOME,
                items = items,
                config = config,
                onDurationModeChanged = {},
                onItemDurationChanged = { _, _ -> },
                onApplyGlobalDuration = {},
                onNextClick = { nextClicked = true }
            )
        }
        composeTestRule.onNodeWithTag("btn_duration_next").performClick()
        assertTrue(nextClicked)
    }

    @Test
    fun `test duration setup screen flip card on tap`() {
        val items = listOf(
            WallpaperItem(id = "item_1", uri = "content://media/1", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 0)
        )
        val config = com.avikalabs.roll.data.model.RotationConfig(WallpaperTarget.HOME)
        composeTestRule.setContent {
            com.avikalabs.roll.ui.screens.DurationSetupScreen(
                target = WallpaperTarget.HOME,
                items = items,
                config = config,
                onDurationModeChanged = {},
                onItemDurationChanged = { _, _ -> },
                onApplyGlobalDuration = {},
                onNextClick = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Tap to set duration").performClick()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test duration setup screen global mode rendering`() {
        val items = listOf(
            WallpaperItem(id = "item_1", uri = "content://media/1", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 0)
        )
        val config = com.avikalabs.roll.data.model.RotationConfig(
            target = WallpaperTarget.HOME,
            durationMode = com.avikalabs.roll.data.model.DurationMode.GLOBAL
        )
        composeTestRule.setContent {
            com.avikalabs.roll.ui.screens.DurationSetupScreen(
                target = WallpaperTarget.HOME,
                items = items,
                config = config,
                onDurationModeChanged = {},
                onItemDurationChanged = { _, _ -> },
                onApplyGlobalDuration = {},
                onNextClick = {}
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test the roll screen rendering and execute button`() {
        val items = listOf(
            WallpaperItem(id = "item_1", uri = "content://media/1", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 0),
            WallpaperItem(id = "item_2", uri = "content://media/2", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 1)
        )
        val config = com.avikalabs.roll.data.model.RotationConfig(WallpaperTarget.HOME)
        var executed = false
        composeTestRule.setContent {
            com.avikalabs.roll.ui.screens.TheRollScreen(
                activeTarget = WallpaperTarget.HOME,
                items = items,
                config = config,
                onTargetSelected = {},
                onPlaybackOrderChanged = {},
                onReorderItems = {},
                onRemoveItem = {},
                onAddFrameClick = {},
                onExecuteClick = { executed = true }
            )
        }
        composeTestRule.onNodeWithTag("btn_execute").performClick()
        assertTrue(executed)
        composeTestRule.onNodeWithTag("dialog_roll_set").assertExists()
    }

    @Test
    fun `test picker screen long press photo card shows remove menu and removes item`() {
        val items = listOf(
            WallpaperItem(id = "item_1", uri = "content://media/1", target = WallpaperTarget.HOME, durationSeconds = 900, sortIndex = 0)
        )
        var removedId: String? = null
        composeTestRule.setContent {
            com.avikalabs.roll.ui.screens.PickerScreen(
                activeTarget = WallpaperTarget.HOME,
                items = items,
                autoChangeOnUnlock = false,
                isLiveWallpaperActive = false,
                onTargetSelected = {},
                onAutoChangeOnUnlockToggle = {},
                onSetLiveWallpaperClick = {},
                onAddTileClick = {},
                onRemoveItem = { removedId = it },
                onNextClick = {}
            )
        }
        composeTestRule.onNodeWithTag("photo_card_item_1").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_remove_from_roll_item_1").performClick()
        assertEquals("item_1", removedId)
    }

    @Test
    fun `test removing photo with sourceFolderUri records in removed_uris`() = runBlocking {
        val item = WallpaperItem(
            id = "f1",
            uri = "content://media/folder/photo1.jpg",
            target = WallpaperTarget.HOME,
            sourceFolderUri = "content://media/tree/folder1"
        )
        repository.addItems(listOf(item))
        repository.removeItem("f1", WallpaperTarget.HOME)

        val removed = repository.getAllRemovedUris()
        assertTrue(removed.contains("content://media/folder/photo1.jpg"))
    }

    @Test
    fun `test removing photo without sourceFolderUri does not record in removed_uris`() = runBlocking {
        val item = WallpaperItem(
            id = "m1",
            uri = "content://media/manual/photo1.jpg",
            target = WallpaperTarget.HOME,
            sourceFolderUri = null
        )
        repository.addItems(listOf(item))
        repository.removeItem("m1", WallpaperTarget.HOME)

        val removed = repository.getAllRemovedUris()
        assertFalse(removed.contains("content://media/manual/photo1.jpg"))
    }

    @Test
    fun `test migration 1 to 2 creates removed_uris table`() {
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }).build())
        val db = helper.writableDatabase
        RollDatabase.MIGRATION_1_2.migrate(db)
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='removed_uris'")
        assertTrue(cursor.moveToFirst())
        assertEquals("removed_uris", cursor.getString(0))
        cursor.close()
        db.close()
    }
}
