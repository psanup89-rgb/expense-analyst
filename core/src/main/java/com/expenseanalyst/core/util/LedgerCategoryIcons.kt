package com.expenseanalyst.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Ledger category glyphs: one drawn family on a 24dp grid with 2dp padding, a 1.5dp stroke
 * and round caps and joins. Artwork source: the "Ledger — final" design canvas, board 3.
 *
 * Keyed by the icon names stored in `categories.icon_name`, so no data changes are needed to
 * adopt them — except Transport, whose seeded name moves from `directions_car` to
 * `directions_bus` (MIGRATION_28_29) so it no longer shares a car with a user's Vehicle category.
 *
 * Drawn in black: `Icon(tint = …)` and the notification rasteriser both recolour the stroke.
 */
object LedgerCategoryIcons {

    private val PATHS: Map<String, String> = mapOf(
        "restaurant" to "M7 3v7M5 3v4a2 2 0 0 0 4 0V3M7 10v11M17 3c-2 1.2-3 3.4-3 6.5 0 2.3 1 3.5 3 3.5v8",
        "local_grocery_store" to "M4 9h16l-1.6 9.2a2 2 0 0 1-2 1.8H7.6a2 2 0 0 1-2-1.8L4 9zM8 9l3-5M16 9l-3-5M9.5 13v3.5M14.5 13v3.5",
        "shopping_bag" to "M6 8h12l-1.1 12.1a1 1 0 0 1-1 .9H8.1a1 1 0 0 1-1-.9L6 8zM9 8V6.5a3 3 0 0 1 6 0V8",
        "receipt_long" to "M6 3h12v18l-2.4-1.6L13.2 21l-2.4-1.6L8.4 21 6 19.4V3zM9 8h6M9 11.5h6M9 15h3.5",
        "home" to "M4 11l8-7 8 7M6 9.5V20h12V9.5M10 20v-5.5h4V20",
        "directions_bus" to "M6 4h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM4 11h16M8 18v2M16 18v2M8 14.5h.01M16 14.5h.01",
        "directions_car" to "M5 15l1.6-5.2A2 2 0 0 1 8.5 8.4h7a2 2 0 0 1 1.9 1.4L19 15M4 15h16v4H4zM7 19v2M17 19v2M7.5 17h.01M16.5 17h.01",
        "local_gas_station" to "M5 21V5a2 2 0 0 1 2-2h5a2 2 0 0 1 2 2v16M4 21h11M8 8h3M14 10h2a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 0 3 0V8.5L18 6",
        "hotel" to "M3 19V6M3 14h18v5M21 19v-3.5a2.5 2.5 0 0 0-2.5-2.5H11v1M7 12.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4z",
        "movie" to "M4 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2.5a2.5 2.5 0 0 0 0 5V17a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2.5a2.5 2.5 0 0 0 0-5zM14 5v2M14 11v2M14 17v2",
        "medical_services" to "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10zM8.5 11.5h2l1-2 2 4 1-2h1",
        "school" to "M2.5 9L12 5l9.5 4L12 13 2.5 9zM6.5 10.8V15c0 1.4 2.5 3 5.5 3s5.5-1.6 5.5-3v-4.2M21.5 9v5",
        "pets" to "M12 20c-3 0-5-1.7-5-3.8 0-2.3 2.4-4.7 5-4.7s5 2.4 5 4.7c0 2.1-2 3.8-5 3.8zM5.5 12.5a1.7 2.1 0 1 0 0-4.2 1.7 2.1 0 0 0 0 4.2zM18.5 12.5a1.7 2.1 0 1 0 0-4.2 1.7 2.1 0 0 0 0 4.2zM9.3 8.5a1.7 2.1 0 1 0 0-4.2 1.7 2.1 0 0 0 0 4.2zM14.7 8.5a1.7 2.1 0 1 0 0-4.2 1.7 2.1 0 0 0 0 4.2z",
        "payments" to "M3 7h18v10H3zM12 14.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM6.5 10v4M17.5 10v4",
        "currency_exchange" to "M9 5L4 10l5 5M4 10h10a5 5 0 0 1 0 10h-3",
        "swap_horiz" to "M4 8h15l-3.5-3.5M20 16H5l3.5 3.5",
        "credit_card" to "M3 6h18v12H3zM3 10h18M7 14.5h3M14 14.5h3",
        "more_horiz" to "M6 13.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6zM12 13.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6zM18 13.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6z",
        "help_outline" to "M4 8l8-4 8 4v8l-8 4-8-4V8zM4 8l8 4 8-4M12 12v8"
    )

    private val cache = HashMap<String, ImageVector>()

    /** The Ledger glyph for [iconName], or null when the set has no drawing for it. */
    fun forName(iconName: String): ImageVector? {
        val path = PATHS[iconName] ?: return null
        return synchronized(cache) { cache.getOrPut(iconName) { build(iconName, path) } }
    }

    private fun build(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = "Ledger.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()
}
