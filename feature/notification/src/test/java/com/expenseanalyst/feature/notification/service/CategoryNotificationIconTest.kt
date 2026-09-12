package com.expenseanalyst.feature.notification.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Guards the icon-name → drawable map against drift. A seeded category whose icon is missing
 * here degrades silently to a coloured letter badge rather than failing, so nothing else would
 * catch it.
 */
class CategoryNotificationIconTest {

    @Test
    fun `every seeded category icon resolves to a drawable`() {
        listOf(
            "restaurant", "directions_car", "shopping_bag", "receipt_long", "movie",
            "medical_services", "school", "local_grocery_store", "home", "payments",
            "swap_horiz", "more_horiz", "help_outline", "currency_exchange",
            "local_gas_station", "beach_access"
        ).forEach { iconName ->
            assertNotNull(
                CategoryNotificationIcon.drawableFor(iconName),
                "no notification drawable mapped for seeded icon '$iconName'"
            )
        }
    }

    @Test
    fun `an unmapped icon falls through to the letter badge`() {
        assertNull(CategoryNotificationIcon.drawableFor("sports_esports"))
    }
}
