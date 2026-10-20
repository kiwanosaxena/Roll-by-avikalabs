package com.avikalabs.roll

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.avikalabs.roll.billing.IBillingManager
import com.avikalabs.roll.data.local.RollDao
import com.avikalabs.roll.data.local.RollDatabase
import com.avikalabs.roll.data.local.RollPreferences
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.SyncedFolder
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

class FakeBillingManager(initialPro: Boolean = false) : IBillingManager {
    private val _isPro = MutableStateFlow(initialPro)
    override val isPro: StateFlow<Boolean> = _isPro

    private val _formattedPrice = MutableStateFlow<String?>("$2.99")
    override val formattedPrice: StateFlow<String?> = _formattedPrice

    private val _isBillingAvailable = MutableStateFlow(true)
    override val isBillingAvailable: StateFlow<Boolean> = _isBillingAvailable

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    override val purchaseMessage: StateFlow<String?> = _purchaseMessage

    fun setPro(value: Boolean) {
        _isPro.value = value
    }

    override fun launchPurchase(activity: Activity) {
        _isPro.value = true
        _purchaseMessage.value = "Welcome to Roll Pro!"
    }

    override fun refreshPurchases(onComplete: ((Boolean) -> Unit)?) {
        onComplete?.invoke(_isPro.value)
    }

    override fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }

    override fun isProSync(): Boolean = _isPro.value
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BillingAndProRulesTest {

    private lateinit var context: Context
    private lateinit var preferences: RollPreferences
    private lateinit var repository: RollRepository
    private lateinit var dao: RollDao

    private fun createTestImageUri(filename: String = "test.jpg"): String {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, filename)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file).toString()
    }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        preferences = RollPreferences(context)
        preferences.setIsPro(false)

        dao = RollDatabase.getInstance(context).rollDao()
        dao.clearItemsByTarget(WallpaperTarget.HOME)
        dao.clearItemsByTarget(WallpaperTarget.LOCK)
        dao.getAllSyncedFoldersSync().forEach {
            dao.deleteSyncedFolder(it.treeUri, it.target)
        }

        repository = RollRepository.getInstance(context)
    }

    @Test
    fun `test free mode limits photos to 10 in total`() = runBlocking {
        preferences.setIsPro(false)
        assertFalse(repository.isProSync())

        val items = (1..15).map { index ->
            WallpaperItem(
                id = "item_$index",
                uri = "content://media/photo_$index.jpg",
                target = WallpaperTarget.HOME,
                sortIndex = index
            )
        }

        repository.addItems(items)

        val homeItems = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals(10, homeItems.size)
    }

    @Test
    fun `test pro mode allows more than 10 photos`() = runBlocking {
        preferences.setIsPro(true)
        assertTrue(repository.isProSync())

        val items = (1..20).map { index ->
            WallpaperItem(
                id = "item_$index",
                uri = "content://media/photo_$index.jpg",
                target = WallpaperTarget.HOME,
                sortIndex = index
            )
        }

        repository.addItems(items)

        val homeItems = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals(20, homeItems.size)
    }

    @Test
    fun `test free mode shared roll applies home photo to both screens`() = runBlocking {
        preferences.setIsPro(false)

        val uri1 = createTestImageUri("photo1.jpg")
        val uri2 = createTestImageUri("photo2.jpg")

        val item1 = WallpaperItem(
            id = "photo_1",
            uri = uri1,
            target = WallpaperTarget.HOME,
            sortIndex = 0
        )
        val item2 = WallpaperItem(
            id = "photo_2",
            uri = uri2,
            target = WallpaperTarget.HOME,
            sortIndex = 1
        )
        repository.addItems(listOf(item1, item2))

        val result = repository.executeRollSetup(WallpaperTarget.HOME)
        assertTrue(result)

        val lockConfig = repository.getRotationConfigSync(WallpaperTarget.LOCK)
        assertEquals(null, lockConfig?.nextScheduledAt)
    }

    @Test
    fun `test free mode treats shuffle playback order as in order sequential`() = runBlocking {
        preferences.setIsPro(false)
        repository.setPlaybackOrder(WallpaperTarget.HOME, PlaybackOrder.SHUFFLE)

        val config = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertEquals(PlaybackOrder.SHUFFLE, config?.playbackOrder)

        val testUri = createTestImageUri("photo_shuffle.jpg")
        val items = (1..5).map { index ->
            WallpaperItem(
                id = "photo_$index",
                uri = testUri,
                target = WallpaperTarget.HOME,
                sortIndex = index - 1
            )
        }
        repository.addItems(items)
        dao.updateCurrentIndex(WallpaperTarget.HOME, 0)

        // Advance rotation in free mode with SHUFFLE configured:
        // computeNextIndex will fall back to SEQUENTIAL ((0 + 1) % 5 = 1)
        val advanced = repository.advanceRotation(WallpaperTarget.HOME, isScheduledTrigger = false)
        assertTrue(advanced)
        val updatedConfig = repository.getRotationConfigSync(WallpaperTarget.HOME)
        assertEquals(1, updatedConfig?.currentIndex)
    }

    @Test
    fun `test folder sync is skipped when not pro`() = runBlocking {
        preferences.setIsPro(false)
        assertFalse(repository.isProSync())

        val syncedFolder = SyncedFolder(
            treeUri = "content://com.android.externalstorage.documents/tree/primary%3APictures",
            folderName = "Pictures",
            target = WallpaperTarget.HOME
        )
        dao.insertSyncedFolder(syncedFolder)

        val foldersBefore = repository.getAllSyncedFoldersSync()
        assertEquals(1, foldersBefore.size)

        val itemsBefore = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        assertEquals(0, itemsBefore.size)

        val count = repository.importFromFolder(
            WallpaperTarget.HOME,
            Uri.parse(syncedFolder.treeUri),
            syncedFolder.folderName
        )
        assertEquals(0, count)
    }

    @Test
    fun `test pro unlocks everything and refund keeps all data`() = runBlocking {
        val fakeBilling = FakeBillingManager(initialPro = false)
        assertFalse(fakeBilling.isProSync())

        // 1. Pro purchased
        fakeBilling.setPro(true)
        preferences.setIsPro(true)
        assertTrue(repository.isProSync())

        val validUri = createTestImageUri("photo_pro_refund.jpg")

        // 2. In pro mode, user adds 15 photos and a separate lock screen roll
        val homePhotos = (1..15).map { index ->
            WallpaperItem(
                id = "home_photo_$index",
                uri = validUri,
                target = WallpaperTarget.HOME,
                sortIndex = index
            )
        }
        val lockPhotos = (1..5).map { index ->
            WallpaperItem(
                id = "lock_photo_$index",
                uri = validUri,
                target = WallpaperTarget.LOCK,
                sortIndex = index
            )
        }
        repository.addItems(homePhotos)
        repository.addItems(lockPhotos)

        assertEquals(15, repository.getItemsByTargetSync(WallpaperTarget.HOME).size)
        assertEquals(5, repository.getItemsByTargetSync(WallpaperTarget.LOCK).size)

        // 3. Pro refunded: isPro becomes false
        fakeBilling.setPro(false)
        preferences.setIsPro(false)
        assertFalse(repository.isProSync())

        // Verify: never delete any data
        val homeAfterRefund = repository.getItemsByTargetSync(WallpaperTarget.HOME)
        val lockAfterRefund = repository.getItemsByTargetSync(WallpaperTarget.LOCK)
        assertEquals(15, homeAfterRefund.size)
        assertEquals(5, lockAfterRefund.size)

        // Verify: in free mode, advanceRotation on LOCK does nothing and cancels lock schedule
        val lockAdvanceResult = repository.advanceRotation(WallpaperTarget.LOCK, isScheduledTrigger = false)
        assertFalse(lockAdvanceResult)

        // Verify: in free mode, home rotation rotates only the first 10 photos
        dao.updateCurrentIndex(WallpaperTarget.HOME, 9)
        val homeAdvanceResult = repository.advanceRotation(WallpaperTarget.HOME, isScheduledTrigger = false)
        assertTrue(homeAdvanceResult)
        val homeConfig = repository.getRotationConfigSync(WallpaperTarget.HOME)
        // With items.take(10), after index 9 it wraps back to 0 ((9 + 1) % 10 = 0) instead of index 10
        assertEquals(0, homeConfig?.currentIndex)
    }
}
