package com.avikalabs.roll.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.avikalabs.roll.data.local.RollPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface IBillingManager {
    val isPro: StateFlow<Boolean>
    val formattedPrice: StateFlow<String?>
    val isBillingAvailable: StateFlow<Boolean>
    val purchaseMessage: StateFlow<String?>
    fun launchPurchase(activity: Activity)
    fun refreshPurchases(onComplete: ((Boolean) -> Unit)? = null)
    fun clearPurchaseMessage()
    fun isProSync(): Boolean
}

class BillingManager(
    private val context: Context,
    private val preferences: RollPreferences,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : IBillingManager, PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(preferences.isProSync())
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice: StateFlow<String?> = _formattedPrice.asStateFlow()

    private val _isBillingAvailable = MutableStateFlow(true)
    override val isBillingAvailable: StateFlow<Boolean> = _isBillingAvailable.asStateFlow()

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    override val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        // Start connection on app startup
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingAvailable.value = true
                    queryProductDetails()
                    refreshPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage} (code ${billingResult.responseCode})")
                    _isBillingAvailable.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _isBillingAvailable.value = false
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PRO)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isBillingAvailable.value = true
                val details = queryResult.productDetailsList?.firstOrNull { it.productId == PRODUCT_ID_PRO }
                productDetails = details
                val price = details?.oneTimePurchaseOfferDetails?.formattedPrice
                _formattedPrice.value = price
            } else {
                Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
            }
        }
    }

    override fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            queryProductDetails()
            _purchaseMessage.value = "Google Play isn't available right now"
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow error: ${result.debugMessage}")
            if (result.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                _isBillingAvailable.value = false
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchases(purchases)
                } else {
                    refreshPurchases()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
            }
            else -> {
                Log.w(TAG, "Purchases updated error: ${billingResult.debugMessage}")
            }
        }
    }

    override fun refreshPurchases(onComplete: ((Boolean) -> Unit)?) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isBillingAvailable.value = true
                val proPurchase = purchases.firstOrNull { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO)
                }

                if (proPurchase != null) {
                    when (proPurchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            if (!proPurchase.isAcknowledged) {
                                acknowledge(proPurchase)
                            }
                            updateProStatus(true)
                            _purchaseMessage.value = "Welcome to Roll Pro!"
                            onComplete?.invoke(true)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            updateProStatus(false)
                            _purchaseMessage.value = "Payment pending – Pro unlocks automatically when it completes."
                            onComplete?.invoke(false)
                        }
                        else -> {
                            updateProStatus(false)
                            onComplete?.invoke(false)
                        }
                    }
                } else {
                    // roll_pro not found -> isPro = false (handles refunds)
                    updateProStatus(false)
                    onComplete?.invoke(false)
                }
            } else {
                Log.w(TAG, "queryPurchasesAsync failed: ${billingResult.debugMessage}")
                // Keep the last saved value, don't crash
                _isBillingAvailable.value = false
                onComplete?.invoke(_isPro.value)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val proPurchase = purchases.firstOrNull { it.products.contains(PRODUCT_ID_PRO) }
        if (proPurchase != null) {
            when (proPurchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!proPurchase.isAcknowledged) {
                        acknowledge(proPurchase)
                    }
                    updateProStatus(true)
                    _purchaseMessage.value = "Welcome to Roll Pro!"
                }
                Purchase.PurchaseState.PENDING -> {
                    updateProStatus(false)
                    _purchaseMessage.value = "Payment pending – Pro unlocks automatically when it completes."
                }
                else -> {
                    updateProStatus(false)
                }
            }
        } else {
            updateProStatus(false)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
            }
        }
    }

    private fun updateProStatus(isPro: Boolean) {
        _isPro.value = isPro
        externalScope.launch {
            preferences.setIsPro(isPro)
        }
    }

    override fun isProSync(): Boolean {
        return preferences.isProSync()
    }

    override fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }

    companion object {
        const val PRODUCT_ID_PRO = "roll_pro"
        private const val TAG = "BillingManager"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                val preferences = RollPreferences(context)
                val instance = BillingManager(context.applicationContext, preferences)
                INSTANCE = instance
                instance
            }
        }
    }
}
