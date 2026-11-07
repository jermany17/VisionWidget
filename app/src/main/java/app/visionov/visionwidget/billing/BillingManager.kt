package app.visionov.visionwidget.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import app.visionov.visionwidget.data.AppPreferences
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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "VisionBilling"

/**
 * The app's one line to Play's billing service.
 *
 * A single instance for the process, because the connection is a process-wide thing:
 * Play pushes a purchase back through whichever listener is registered, and two clients
 * would mean a purchase landing on one of them at random. Held against the application
 * context for the same reason — an Activity's would outlive its own screen.
 *
 * What it answers are two flows. [offers] is what the paywall prices itself from, and
 * [status] is what every locked feature asks. Neither is cached to disk: Play is the
 * record of what was bought, and a copy of it here is a copy that can be edited. The
 * cost is a second of [PlusStatus.Unknown] at launch, which is why that is a state of
 * its own rather than a false.
 */
class BillingManager private constructor(context: Context) {

    private val app = context.applicationContext
    private val preferences = AppPreferences(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow<PlusStatus>(PlusStatus.Unknown)
    val status: StateFlow<PlusStatus> = _status.asStateFlow()

    private val _offers = MutableStateFlow<PlusOffers?>(null)
    val offers: StateFlow<PlusOffers?> = _offers.asStateFlow()

    /** Held so the purchase flow doesn't have to ask Play a second time to launch. */
    private var productDetails: ProductDetails? = null

    /** One reconnect at a time, however many callers notice the line is down at once. */
    private val connecting = Mutex()

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                scope.launch { settle(purchases.orEmpty()) }

            // Backing out of the sheet. Not a failure, and nothing to say about it.
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit

            else -> Log.w(TAG, "Purchase update failed: ${result.debugMessage}")
        }
    }

    private val client = BillingClient.newBuilder(app)
        .setListener(purchasesUpdated)
        .enablePendingPurchases(
            // Slow payments — a bank transfer, a cash top-up — land later as a separate
            // update rather than at the end of the sheet. Declared so Play will deliver
            // them at all; [settle] ignores anything not yet paid for.
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /**
     * Brings the line up if it is down, and leaves it up.
     *
     * Safe to call as often as anything likes — on every return to the foreground, for
     * instance, which is the point: Play drops the connection when it is updated or the
     * process is backgrounded long enough, and nothing announces that it has.
     */
    fun refresh() {
        scope.launch {
            if (connect()) {
                loadOffers()
                reconcile()
            }
        }
    }

    private suspend fun connect(): Boolean = connecting.withLock {
        if (client.isReady) return@withLock true

        return@withLock runCatching {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    private var answered = false

                    override fun onBillingSetupFinished(result: BillingResult) {
                        // Play can call this twice on a retry; the second resume would
                        // crash the coroutine machinery rather than be ignored.
                        if (answered) return
                        answered = true

                        val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                        if (!ok) Log.w(TAG, "Billing setup: ${result.debugMessage}")
                        continuation.resumeWith(Result.success(ok))
                    }

                    override fun onBillingServiceDisconnected() {
                        if (answered) return
                        answered = true
                        continuation.resumeWith(Result.success(false))
                    }
                })
            }
        }.getOrElse {
            Log.w(TAG, "Billing connect failed", it)
            false
        }
    }

    /** Asks Play what the two plans cost, in the user's own currency. */
    private suspend fun loadOffers() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Plus.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val result = runCatching { client.queryProductDetails(params) }.getOrNull() ?: return
        val details = result.productDetailsList?.firstOrNull { it.productId == Plus.PRODUCT_ID }
        if (details == null) {
            // Almost always the Console rather than the code: a base plan left inactive,
            // or a build the tester's account can't see on any track.
            Log.w(TAG, "No product details for ${Plus.PRODUCT_ID}")
            return
        }

        productDetails = details
        val monthly = details.offerFor(Plus.MONTHLY)
        val yearly = details.offerFor(Plus.YEARLY)
        _offers.value = if (monthly != null && yearly != null) {
            PlusOffers(monthly, yearly)
        } else {
            Log.w(TAG, "Missing a base plan: monthly=$monthly yearly=$yearly")
            null
        }
    }

    /**
     * What Play says is owned right now, which is the only thing worth believing.
     *
     * Asked on every refresh rather than only after a purchase: a subscription can end,
     * be cancelled, or be refunded somewhere the app was never running to hear about it.
     */
    private suspend fun reconcile() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = runCatching { client.queryPurchasesAsync(params) }.getOrNull()
        if (result == null || result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Left as it was rather than cleared. A failed query is not evidence that a
            // subscription ended, and dropping someone to the paywall on a dropped
            // connection is the worse of the two mistakes.
            return
        }

        settle(result.purchasesList)
    }

    /**
     * Takes Play's word for what is owned, and finishes anything half-done.
     *
     * A purchase that is never acknowledged is refunded automatically within three days,
     * so this is not housekeeping — it is the last step of buying.
     */
    private suspend fun settle(purchases: List<Purchase>) {
        val held = purchases.firstOrNull {
            it.products.contains(Plus.PRODUCT_ID) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (held == null) {
            _status.value = PlusStatus.None
            return
        }

        if (!held.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(held.purchaseToken)
                .build()
            runCatching { client.acknowledgePurchase(params) }
                .onFailure { Log.w(TAG, "Acknowledge failed", it) }
        }

        _status.value = PlusStatus.Active(
            // Remembered from the purchase rather than read back off it: Play tells the
            // client what was bought but not which base plan it was bought on. Only the
            // settings row's wording turns on this, so a blank after a reinstall costs
            // the word "Annual" and nothing else.
            basePlanId = preferences.purchasedBasePlanId,
            autoRenewing = held.isAutoRenewing
        )
    }

    /**
     * Opens Play's own purchase sheet on [basePlanId].
     *
     * Needs the Activity rather than the application context — the sheet is put up over
     * whatever is on screen, and Play will not be handed a context with no window.
     */
    fun purchase(activity: Activity, basePlanId: String) {
        val details = productDetails
        val token = details?.offerFor(basePlanId)?.offerToken
        if (details == null || token == null) {
            Log.w(TAG, "Nothing to purchase: details=$details plan=$basePlanId")
            // Likely a sheet opened before Play answered. Ask again so the next tap works.
            refresh()
            return
        }

        // Noted before the sheet opens, because Play won't say afterwards which plan
        // was taken. Harmless if the purchase is abandoned: nothing reads it until a
        // purchase is actually held.
        preferences.purchasedBasePlanId = basePlanId

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(token)
                        .build()
                )
            )
            .build()

        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Could not open the purchase sheet: ${result.debugMessage}")
        }
    }

    companion object {
        @Volatile
        private var instance: BillingManager? = null

        fun get(context: Context): BillingManager =
            instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
    }
}

/** The offer sitting under a base plan, or null where the Console hasn't got one. */
private fun ProductDetails.offerFor(basePlanId: String): PlanOffer? {
    val offer = subscriptionOfferDetails
        ?.firstOrNull { it.basePlanId == basePlanId }
        ?: return null

    // The last phase is the one that goes on forever; the ones before it are the trial
    // and the introductory price, which the paywall doesn't quote.
    val price = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
        ?: return null

    return PlanOffer(basePlanId, price, offer.offerToken)
}

