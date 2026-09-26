package com.expenseanalyst.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FlightLand
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Roofing
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Snowboarding
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsFootball
import androidx.compose.material.icons.outlined.SportsGolf
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The glyph for a category's stored icon name. The Ledger set ([LedgerCategoryIcons]) covers every
 * icon name the seeded and in-use categories carry and wins when present; every other name the
 * picker offers falls back to Material's *Outlined* style, which sits close enough to the Ledger
 * line icons that a custom category doesn't look out of family.
 *
 * Used by the app AND by the notification badge (rasterised by ImageVectorRasterizer), so the two
 * can never disagree about a category's icon.
 */
fun categoryIconVector(iconName: String): ImageVector =
    LedgerCategoryIcons.forName(iconName) ?: materialCategoryIcon(iconName)

private fun materialCategoryIcon(iconName: String): ImageVector = when (iconName) {
    // Food & Drink
    "restaurant"             -> Icons.Outlined.Restaurant
    "local_cafe"             -> Icons.Outlined.LocalCafe
    "fastfood"               -> Icons.Outlined.Fastfood
    "local_pizza"            -> Icons.Outlined.LocalPizza
    "local_bar"              -> Icons.Outlined.LocalBar
    "cake"                   -> Icons.Outlined.Cake
    "lunch_dining"           -> Icons.Outlined.LunchDining
    "dinner_dining"          -> Icons.Outlined.DinnerDining
    "bakery_dining"          -> Icons.Outlined.BakeryDining
    "ice_cream"              -> Icons.Outlined.Icecream
    // Transport
    "directions_car"         -> Icons.Outlined.DirectionsCar
    "local_gas_station"      -> Icons.Outlined.LocalGasStation
    "flight_takeoff"         -> Icons.Outlined.FlightTakeoff
    "flight_land"            -> Icons.Outlined.FlightLand
    "directions_bus"         -> Icons.Outlined.DirectionsBus
    "train"                  -> Icons.Outlined.Train
    "directions_bike"        -> Icons.Outlined.DirectionsBike
    "local_taxi"             -> Icons.Outlined.LocalTaxi
    "two_wheeler"            -> Icons.Outlined.TwoWheeler
    "electric_car"           -> Icons.Outlined.ElectricCar
    "commute"                -> Icons.Outlined.Commute
    "directions_walk"        -> Icons.Outlined.DirectionsWalk
    // Shopping
    "shopping_bag"           -> Icons.Outlined.ShoppingBag
    "store"                  -> Icons.Outlined.Store
    "storefront"             -> Icons.Outlined.Storefront
    "local_mall"             -> Icons.Outlined.LocalMall
    "card_giftcard"          -> Icons.Outlined.CardGiftcard
    "redeem"                 -> Icons.Outlined.Redeem
    "local_grocery_store"    -> Icons.Outlined.ShoppingCart
    "checkroom"              -> Icons.Outlined.Checkroom
    "local_offer"            -> Icons.Outlined.LocalOffer
    // Finance
    "payments"               -> Icons.Outlined.Payments
    "credit_card"            -> Icons.Outlined.CreditCard
    "savings"                -> Icons.Outlined.Savings
    "account_balance_wallet" -> Icons.Outlined.AccountBalanceWallet
    "trending_up"            -> Icons.Outlined.TrendingUp
    "trending_down"          -> Icons.Outlined.TrendingDown
    "money_off"              -> Icons.Outlined.MoneyOff
    "account_balance"        -> Icons.Outlined.AccountBalance
    "receipt"                -> Icons.Outlined.Receipt
    "attach_money"           -> Icons.Outlined.AttachMoney
    "currency_exchange"      -> Icons.Outlined.CurrencyExchange
    // Home & Utilities
    "home"                   -> Icons.Outlined.Home
    "weekend"                -> Icons.Outlined.Weekend
    "build"                  -> Icons.Outlined.Build
    "local_laundry_service"  -> Icons.Outlined.LocalLaundryService
    "kitchen"                -> Icons.Outlined.Kitchen
    "power"                  -> Icons.Outlined.Power
    "water_drop"             -> Icons.Outlined.WaterDrop
    "wifi"                   -> Icons.Outlined.Wifi
    "bathtub"                -> Icons.Outlined.Bathtub
    "security"               -> Icons.Outlined.Security
    "roofing"                -> Icons.Outlined.Roofing
    "cleaning_services"      -> Icons.Outlined.CleaningServices
    "plumbing"               -> Icons.Outlined.Plumbing
    // Health & Wellness
    "medical_services"       -> Icons.Outlined.LocalHospital
    "fitness_center"         -> Icons.Outlined.FitnessCenter
    "spa"                    -> Icons.Outlined.Spa
    "local_pharmacy"         -> Icons.Outlined.LocalPharmacy
    "self_improvement"       -> Icons.Outlined.SelfImprovement
    "psychology"             -> Icons.Outlined.Psychology
    "monitor_heart"          -> Icons.Outlined.MonitorHeart
    "healing"                -> Icons.Outlined.Healing
    "medication"             -> Icons.Outlined.Medication
    // Entertainment
    "movie"                  -> Icons.Outlined.Movie
    "sports_esports"         -> Icons.Outlined.SportsEsports
    "music_note"             -> Icons.Outlined.MusicNote
    "sports_football"        -> Icons.Outlined.SportsFootball
    "sports_basketball"      -> Icons.Outlined.SportsBasketball
    "sports_tennis"          -> Icons.Outlined.SportsTennis
    "headphones"             -> Icons.Outlined.Headphones
    "beach_access"           -> Icons.Outlined.BeachAccess
    "theaters"               -> Icons.Outlined.Theaters
    "casino"                 -> Icons.Outlined.Casino
    "nightlife"              -> Icons.Outlined.Nightlife
    "sports_golf"            -> Icons.Outlined.SportsGolf
    // Travel
    "hotel"                  -> Icons.Outlined.Hotel
    "luggage"                -> Icons.Outlined.Luggage
    "explore"                -> Icons.Outlined.Explore
    "travel_explore"         -> Icons.Outlined.TravelExplore
    "sailing"                -> Icons.Outlined.Sailing
    "snowboarding"           -> Icons.Outlined.Snowboarding
    "hiking"                 -> Icons.Outlined.Hiking
    "map"                    -> Icons.Outlined.Map
    "location_on"            -> Icons.Outlined.LocationOn
    // Education
    "school"                 -> Icons.Outlined.School
    "auto_stories"           -> Icons.Outlined.AutoStories
    "library_books"          -> Icons.Outlined.LibraryBooks
    "science"                -> Icons.Outlined.Science
    "calculate"              -> Icons.Outlined.Calculate
    "menu_book"              -> Icons.Outlined.MenuBook
    // Bills & Admin
    "receipt_long"           -> Icons.Outlined.ReceiptLong
    "bolt"                   -> Icons.Outlined.Bolt
    "local_fire_department"  -> Icons.Outlined.LocalFireDepartment
    "vpn_lock"               -> Icons.Outlined.VpnLock
    // Personal & Lifestyle
    "pets"                   -> Icons.Outlined.Pets
    "child_care"             -> Icons.Outlined.ChildCare
    "content_cut"            -> Icons.Outlined.ContentCut
    "phone_android"          -> Icons.Outlined.PhoneAndroid
    "laptop"                 -> Icons.Outlined.Laptop
    "work"                   -> Icons.Outlined.Work
    "volunteer_activism"     -> Icons.Outlined.VolunteerActivism
    "groups"                 -> Icons.Outlined.Groups
    "child_friendly"         -> Icons.Outlined.ChildFriendly
    "face"                   -> Icons.Outlined.Face
    // General
    "swap_horiz"             -> Icons.Outlined.SwapHoriz
    "favorite"               -> Icons.Outlined.Favorite
    "more_horiz"             -> Icons.Outlined.MoreHoriz
    "help_outline"           -> Icons.Outlined.Help
    "star"                   -> Icons.Outlined.Star
    "flag"                   -> Icons.Outlined.Flag
    "label"                  -> Icons.Outlined.Label
    "bookmark"               -> Icons.Outlined.Bookmark
    else                     -> Icons.Outlined.MoreHoriz
}
