package com.expenseanalyst.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIconVector(iconName: String): ImageVector = when (iconName) {
    // Food & Drink
    "restaurant"             -> Icons.Filled.Restaurant
    "local_cafe"             -> Icons.Filled.LocalCafe
    "fastfood"               -> Icons.Filled.Fastfood
    "local_pizza"            -> Icons.Filled.LocalPizza
    "local_bar"              -> Icons.Filled.LocalBar
    "cake"                   -> Icons.Filled.Cake
    "lunch_dining"           -> Icons.Filled.LunchDining
    "dinner_dining"          -> Icons.Filled.DinnerDining
    "bakery_dining"          -> Icons.Filled.BakeryDining
    "ice_cream"              -> Icons.Filled.Icecream
    // Transport
    "directions_car"         -> Icons.Filled.DirectionsCar
    "local_gas_station"      -> Icons.Filled.LocalGasStation
    "flight_takeoff"         -> Icons.Filled.FlightTakeoff
    "flight_land"            -> Icons.Filled.FlightLand
    "directions_bus"         -> Icons.Filled.DirectionsBus
    "train"                  -> Icons.Filled.Train
    "directions_bike"        -> Icons.Filled.DirectionsBike
    "local_taxi"             -> Icons.Filled.LocalTaxi
    "two_wheeler"            -> Icons.Filled.TwoWheeler
    "electric_car"           -> Icons.Filled.ElectricCar
    "commute"                -> Icons.Filled.Commute
    "directions_walk"        -> Icons.Filled.DirectionsWalk
    // Shopping
    "shopping_bag"           -> Icons.Filled.ShoppingBag
    "store"                  -> Icons.Filled.Store
    "storefront"             -> Icons.Filled.Storefront
    "local_mall"             -> Icons.Filled.LocalMall
    "card_giftcard"          -> Icons.Filled.CardGiftcard
    "redeem"                 -> Icons.Filled.Redeem
    "local_grocery_store"    -> Icons.Filled.ShoppingCart
    "checkroom"              -> Icons.Filled.Checkroom
    "local_offer"            -> Icons.Filled.LocalOffer
    // Finance
    "payments"               -> Icons.Filled.Payments
    "credit_card"            -> Icons.Filled.CreditCard
    "savings"                -> Icons.Filled.Savings
    "account_balance_wallet" -> Icons.Filled.AccountBalanceWallet
    "trending_up"            -> Icons.Filled.TrendingUp
    "trending_down"          -> Icons.Filled.TrendingDown
    "money_off"              -> Icons.Filled.MoneyOff
    "account_balance"        -> Icons.Filled.AccountBalance
    "receipt"                -> Icons.Filled.Receipt
    "attach_money"           -> Icons.Filled.AttachMoney
    "currency_exchange"      -> Icons.Filled.CurrencyExchange
    // Home & Utilities
    "home"                   -> Icons.Filled.Home
    "weekend"                -> Icons.Filled.Weekend
    "build"                  -> Icons.Filled.Build
    "local_laundry_service"  -> Icons.Filled.LocalLaundryService
    "kitchen"                -> Icons.Filled.Kitchen
    "power"                  -> Icons.Filled.Power
    "water_drop"             -> Icons.Filled.WaterDrop
    "wifi"                   -> Icons.Filled.Wifi
    "bathtub"                -> Icons.Filled.Bathtub
    "security"               -> Icons.Filled.Security
    "roofing"                -> Icons.Filled.Roofing
    "cleaning_services"      -> Icons.Filled.CleaningServices
    "plumbing"               -> Icons.Filled.Plumbing
    // Health & Wellness
    "medical_services"       -> Icons.Filled.LocalHospital
    "fitness_center"         -> Icons.Filled.FitnessCenter
    "spa"                    -> Icons.Filled.Spa
    "local_pharmacy"         -> Icons.Filled.LocalPharmacy
    "self_improvement"       -> Icons.Filled.SelfImprovement
    "psychology"             -> Icons.Filled.Psychology
    "monitor_heart"          -> Icons.Filled.MonitorHeart
    "healing"                -> Icons.Filled.Healing
    "medication"             -> Icons.Filled.Medication
    // Entertainment
    "movie"                  -> Icons.Filled.Movie
    "sports_esports"         -> Icons.Filled.SportsEsports
    "music_note"             -> Icons.Filled.MusicNote
    "sports_football"        -> Icons.Filled.SportsFootball
    "sports_basketball"      -> Icons.Filled.SportsBasketball
    "sports_tennis"          -> Icons.Filled.SportsTennis
    "headphones"             -> Icons.Filled.Headphones
    "beach_access"           -> Icons.Filled.BeachAccess
    "theaters"               -> Icons.Filled.Theaters
    "casino"                 -> Icons.Filled.Casino
    "nightlife"              -> Icons.Filled.Nightlife
    "sports_golf"            -> Icons.Filled.SportsGolf
    // Travel
    "hotel"                  -> Icons.Filled.Hotel
    "luggage"                -> Icons.Filled.Luggage
    "explore"                -> Icons.Filled.Explore
    "travel_explore"         -> Icons.Filled.TravelExplore
    "sailing"                -> Icons.Filled.Sailing
    "snowboarding"           -> Icons.Filled.Snowboarding
    "hiking"                 -> Icons.Filled.Hiking
    "map"                    -> Icons.Filled.Map
    "location_on"            -> Icons.Filled.LocationOn
    // Education
    "school"                 -> Icons.Filled.School
    "auto_stories"           -> Icons.Filled.AutoStories
    "library_books"          -> Icons.Filled.LibraryBooks
    "science"                -> Icons.Filled.Science
    "calculate"              -> Icons.Filled.Calculate
    "menu_book"              -> Icons.Filled.MenuBook
    // Bills & Admin
    "receipt_long"           -> Icons.Filled.ReceiptLong
    "bolt"                   -> Icons.Filled.Bolt
    "local_fire_department"  -> Icons.Filled.LocalFireDepartment
    "vpn_lock"               -> Icons.Filled.VpnLock
    // Personal & Lifestyle
    "pets"                   -> Icons.Filled.Pets
    "child_care"             -> Icons.Filled.ChildCare
    "content_cut"            -> Icons.Filled.ContentCut
    "phone_android"          -> Icons.Filled.PhoneAndroid
    "laptop"                 -> Icons.Filled.Laptop
    "work"                   -> Icons.Filled.Work
    "volunteer_activism"     -> Icons.Filled.VolunteerActivism
    "groups"                 -> Icons.Filled.Groups
    "child_friendly"         -> Icons.Filled.ChildFriendly
    "face"                   -> Icons.Filled.Face
    // General
    "swap_horiz"             -> Icons.Filled.SwapHoriz
    "favorite"               -> Icons.Filled.Favorite
    "more_horiz"             -> Icons.Filled.MoreHoriz
    "help_outline"           -> Icons.Filled.Help
    "star"                   -> Icons.Filled.Star
    "flag"                   -> Icons.Filled.Flag
    "label"                  -> Icons.Filled.Label
    "bookmark"               -> Icons.Filled.Bookmark
    else                     -> Icons.Filled.MoreHoriz
}
