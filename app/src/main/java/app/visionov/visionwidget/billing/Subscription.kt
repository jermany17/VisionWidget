package app.visionov.visionwidget.billing

/**
 * The one thing sold, and the two ways to pay for it.
 *
 * These are names agreed with Play rather than chosen here: the subscription and its
 * two base plans are declared in the Console, and a letter out of place means the
 * query comes back empty with nothing said about why. Nothing else about the offer —
 * the prices, the trial, the currency — belongs in the app. Those are the Console's to
 * change without a release, and the app reads whatever it is handed.
 */
object Plus {
    const val PRODUCT_ID = "vision_plus"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"

    /**
     * Where Play keeps the subscription once it is bought — the sheet that cancels it,
     * changes the card, or switches the plan. The app never offers those itself: a
     * subscription is managed where it is billed.
     */
    fun manageUrl(packageName: String) =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=$packageName"
}

/** One of the two base plans, as the paywall offers it. */
data class PlanOffer(
    val basePlanId: String,
    /** "$3.99" — formatted by Play in the user's own currency. */
    val price: String,
    /** The token the purchase is launched with; meaningless to anything else. */
    val offerToken: String
)

/** Both plans, once Play has answered. Null until then, and if the query fails. */
data class PlusOffers(
    val monthly: PlanOffer,
    val yearly: PlanOffer
)

/**
 * Whether the user has Vision+, as far as the app can tell.
 *
 * [Unknown] is its own state rather than a false: at launch, before Play has answered,
 * a subscriber would otherwise be shown the paywall for the second it takes to find
 * out. Locked features treat it as not-subscribed; the settings row waits.
 */
sealed interface PlusStatus {

    data object Unknown : PlusStatus

    data object None : PlusStatus

    /**
     * Held, whether or not it renews. A cancelled subscription is still a subscription
     * until the period it was paid for runs out — [autoRenewing] is false then, and the
     * features stay on.
     */
    data class Active(
        val basePlanId: String?,
        val autoRenewing: Boolean
    ) : PlusStatus {
        /** "Vision+ · Annual", as the settings row names it. */
        val label: String
            get() = when (basePlanId) {
                Plus.YEARLY -> "Vision+ · Annual"
                Plus.MONTHLY -> "Vision+ · Monthly"
                else -> "Vision+"
            }
    }

    /** What every locked feature actually asks. Unknown is treated as not held. */
    val isActive: Boolean get() = this is Active
}
