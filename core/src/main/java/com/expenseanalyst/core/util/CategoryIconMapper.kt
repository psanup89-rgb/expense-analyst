package com.expenseanalyst.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIconVector(iconName: String): ImageVector = when (iconName) {
    "restaurant" -> Icons.Filled.Restaurant
    "directions_car" -> Icons.Filled.DirectionsCar
    "shopping_bag" -> Icons.Filled.ShoppingBag
    "receipt_long" -> Icons.Filled.ReceiptLong
    "movie" -> Icons.Filled.Movie
    "medical_services" -> Icons.Filled.LocalHospital
    "school" -> Icons.Filled.School
    "local_grocery_store" -> Icons.Filled.ShoppingCart
    "home" -> Icons.Filled.Home
    "payments" -> Icons.Filled.Payments
    "swap_horiz" -> Icons.Filled.SwapHoriz
    "more_horiz" -> Icons.Filled.MoreHoriz
    "help_outline" -> Icons.Filled.Help
    "account_balance" -> Icons.Filled.AccountBalance
    "favorite" -> Icons.Filled.Favorite
    else -> Icons.Filled.MoreHoriz
}
