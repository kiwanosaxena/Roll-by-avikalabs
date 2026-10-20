package com.avikalabs.roll.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avikalabs.roll.R
import com.avikalabs.roll.RollApplication
import com.avikalabs.roll.billing.BillingManager
import com.avikalabs.roll.data.local.ThemeMode
import com.avikalabs.roll.data.model.DurationMode
import com.avikalabs.roll.data.model.PlaybackOrder
import com.avikalabs.roll.data.model.RotationConfig
import com.avikalabs.roll.data.model.WallpaperItem
import com.avikalabs.roll.data.model.WallpaperTarget
import com.avikalabs.roll.data.repository.RollRepository
import com.avikalabs.roll.wallpaper.WallpaperApplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RollUiState(
    val activeTarget: WallpaperTarget = WallpaperTarget.HOME,
    val homeItems: List<WallpaperItem> = emptyList(),
    val lockItems: List<WallpaperItem> = emptyList(),
    val homeConfig: RotationConfig = RotationConfig(WallpaperTarget.HOME),
    val lockConfig: RotationConfig = RotationConfig(WallpaperTarget.LOCK),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homeDoubleTapEnabled: Boolean = false,
    val totalItemCount: Int = 0,
    val bannerMessage: String? = null,
    val isPro: Boolean = false,
    val proPrice: String? = null,
    val isBillingAvailable: Boolean = true,
    val proStatusMessage: String? = null
)

class RollViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RollRepository.getInstance(application)
    val billingManager = (application as? RollApplication)?.billingManager ?: BillingManager.getInstance(application)

    private val _activeTarget = MutableStateFlow(WallpaperTarget.HOME)
    val activeTarget: StateFlow<WallpaperTarget> = _activeTarget.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isLiveWallpaperActive = MutableStateFlow(WallpaperApplier.isLiveWallpaperActive(application))
    val isLiveWallpaperActive: StateFlow<Boolean> = _isLiveWallpaperActive.asStateFlow()

    fun refreshLiveWallpaperState() {
        _isLiveWallpaperActive.value = WallpaperApplier.isLiveWallpaperActive(getApplication())
        viewModelScope.launch {
            repository.disableUnlockModeIfLiveWallpaperInactive()
        }
    }

    val uiState: StateFlow<RollUiState> = combine(
        listOf(
            _activeTarget,
            repository.getItems(WallpaperTarget.HOME),
            repository.getItems(WallpaperTarget.LOCK),
            repository.getRotationConfig(WallpaperTarget.HOME),
            repository.getRotationConfig(WallpaperTarget.LOCK),
            repository.themeModeFlow,
            repository.homeDoubleTapEnabledFlow,
            _bannerMessage,
            billingManager.isPro,
            billingManager.formattedPrice,
            billingManager.isBillingAvailable,
            billingManager.purchaseMessage
        )
    ) { values ->
        val target = values[0] as WallpaperTarget
        @Suppress("UNCHECKED_CAST")
        val homeItems = values[1] as List<WallpaperItem>
        @Suppress("UNCHECKED_CAST")
        val lockItems = values[2] as List<WallpaperItem>
        val homeConfig = (values[3] as? RotationConfig) ?: RotationConfig(WallpaperTarget.HOME)
        val lockConfig = (values[4] as? RotationConfig) ?: RotationConfig(WallpaperTarget.LOCK)
        val themeMode = values[5] as ThemeMode
        val doubleTap = values[6] as Boolean
        val banner = values[7] as? String
        val isPro = values[8] as Boolean
        val proPrice = values[9] as? String
        val isBillingAvailable = values[10] as Boolean
        val proStatusMessage = values[11] as? String

        RollUiState(
            activeTarget = target,
            homeItems = homeItems,
            lockItems = lockItems,
            homeConfig = homeConfig,
            lockConfig = lockConfig,
            themeMode = themeMode,
            homeDoubleTapEnabled = doubleTap,
            totalItemCount = homeItems.size + lockItems.size,
            bannerMessage = banner,
            isPro = isPro,
            proPrice = proPrice,
            isBillingAvailable = isBillingAvailable,
            proStatusMessage = proStatusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RollUiState()
    )

    fun setActiveTarget(target: WallpaperTarget) {
        _activeTarget.value = target
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun clearProStatusMessage() {
        billingManager.clearPurchaseMessage()
    }

    fun launchPurchase(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun restorePurchases(onResult: ((Boolean) -> Unit)? = null) {
        billingManager.restorePurchases { success ->
            onResult?.invoke(success)
        }
    }

    fun refreshPurchases() {
        billingManager.refreshPurchases()
    }

    fun addPhotos(target: WallpaperTarget, uris: List<Uri>) {
        viewModelScope.launch {
            val isPro = uiState.value.isPro
            val existing = if (target == WallpaperTarget.HOME) uiState.value.homeItems else uiState.value.lockItems
            val currentTotal = uiState.value.totalItemCount

            if (!isPro && currentTotal >= 10) {
                _bannerMessage.value = getApplication<Application>().getString(R.string.pro_limit_reached_message)
                return@launch
            }

            var startSortIndex = (existing.maxOfOrNull { it.sortIndex } ?: -1) + 1

            // Persist read permission if available
            val resolver = getApplication<Application>().contentResolver
            uris.forEach { uri ->
                try {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }

            val newItems = uris.map { uri ->
                WallpaperItem(
                    uri = uri.toString(),
                    target = target,
                    durationSeconds = 900, // default 15 minutes per PRD §7.3.5
                    sourceFolderUri = null,
                    sortIndex = startSortIndex++
                )
            }
            repository.addItems(newItems)
        }
    }

    fun importFolder(target: WallpaperTarget, treeUri: Uri, folderName: String) {
        viewModelScope.launch {
            try {
                val count = repository.importFromFolder(target, treeUri, folderName)
                if (count == 0) {
                    _bannerMessage.value = "That folder didn't have any photos in it."
                }
            } catch (_: Exception) {
                _bannerMessage.value = "Unable to import folder. Please make sure the folder contains accessible photos."
            }
        }
    }

    fun updateItemDuration(itemId: String, durationSeconds: Int) {
        viewModelScope.launch {
            repository.updateItemDuration(itemId, durationSeconds)
        }
    }

    fun applyGlobalDuration(target: WallpaperTarget, durationSeconds: Int) {
        viewModelScope.launch {
            repository.applyGlobalDuration(target, durationSeconds)
        }
    }

    fun setDurationMode(target: WallpaperTarget, mode: DurationMode) {
        viewModelScope.launch {
            repository.setDurationMode(target, mode)
        }
    }

    fun setPlaybackOrder(target: WallpaperTarget, order: PlaybackOrder) {
        viewModelScope.launch {
            repository.setPlaybackOrder(target, order)
        }
    }

    fun setAutoChangeOnUnlock(autoChange: Boolean) {
        viewModelScope.launch {
            repository.setAutoChangeOnUnlock(autoChange)
        }
    }

    fun reorderItems(target: WallpaperTarget, reorderedList: List<WallpaperItem>) {
        viewModelScope.launch {
            repository.reorderItems(target, reorderedList)
        }
    }

    fun removeItem(itemId: String, target: WallpaperTarget) {
        viewModelScope.launch {
            repository.removeItem(itemId, target)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setHomeDoubleTapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHomeDoubleTapEnabled(enabled)
        }
    }

    fun executeRollSetup(target: WallpaperTarget, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.executeRollSetup(target)
            _bannerMessage.value = "Roll set."
            onDone()
        }
    }
}
