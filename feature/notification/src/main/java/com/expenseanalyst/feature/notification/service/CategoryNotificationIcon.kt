package com.expenseanalyst.feature.notification.service

import com.expenseanalyst.feature.notification.R

/**
 * Maps a [com.expenseanalyst.domain.model.Category.iconName] to a static vector drawable used
 * as the notification's large icon.
 *
 * Deliberately a small subset of `CategoryIconMapper` (core/util/CategoryIconMapper.kt), which
 * maps the same icon-name vocabulary to Compose `ImageVector`s for in-app UI — those can't be
 * used here since notifications are built outside any Composition. This covers the 16 seeded
 * categories; anything else (a custom category) falls back to a plain colored-initial badge,
 * drawn at the call site in TransactionAlertNotification.
 *
 * Keyed by icon name, not category name — so any category using one of these icons gets the
 * glyph regardless of what it is called.
 */
internal object CategoryNotificationIcon {

    private val ICONS: Map<String, Int> = mapOf(
        "restaurant" to R.drawable.ic_cat_food,
        "directions_car" to R.drawable.ic_cat_transport,
        "shopping_bag" to R.drawable.ic_cat_shopping,
        "receipt_long" to R.drawable.ic_cat_bills,
        "movie" to R.drawable.ic_cat_entertainment,
        "medical_services" to R.drawable.ic_cat_health,
        "school" to R.drawable.ic_cat_education,
        "local_grocery_store" to R.drawable.ic_cat_groceries,
        "home" to R.drawable.ic_cat_rent,
        "payments" to R.drawable.ic_cat_salary,
        "swap_horiz" to R.drawable.ic_cat_transfer,
        "more_horiz" to R.drawable.ic_cat_other,
        "help_outline" to R.drawable.ic_cat_misc,
        "currency_exchange" to R.drawable.ic_cat_refund,
        "local_gas_station" to R.drawable.ic_cat_fuel,
        "beach_access" to R.drawable.ic_cat_leisure
    )

    fun drawableFor(iconName: String): Int? = ICONS[iconName]
}
