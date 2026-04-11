package com.expenseanalyst.core.util

/**
 * Single source of truth for all selectable category icon keys.
 * To add a new icon: add the key here AND a matching entry in CategoryIconMapper.
 */
val availableCategoryIcons: List<String> = listOf(
    // Food & Drink
    "restaurant", "local_cafe", "fastfood", "local_pizza", "local_bar",
    "cake", "lunch_dining", "dinner_dining", "bakery_dining", "ice_cream",
    // Transport
    "directions_car", "local_gas_station", "flight_takeoff", "flight_land",
    "directions_bus", "train", "directions_bike", "local_taxi", "two_wheeler",
    "electric_car", "commute", "directions_walk",
    // Shopping
    "shopping_bag", "store", "storefront", "local_mall", "card_giftcard",
    "redeem", "local_grocery_store", "checkroom", "local_offer",
    // Finance
    "payments", "credit_card", "savings", "account_balance_wallet",
    "trending_up", "trending_down", "money_off", "account_balance",
    "receipt", "attach_money",
    // Home & Utilities
    "home", "weekend", "build", "local_laundry_service", "kitchen",
    "power", "water_drop", "wifi", "bathtub", "security",
    "roofing", "cleaning_services", "plumbing",
    // Health & Wellness
    "medical_services", "fitness_center", "spa", "local_pharmacy",
    "self_improvement", "psychology", "monitor_heart", "healing", "medication",
    // Entertainment
    "movie", "sports_esports", "music_note", "sports_football",
    "sports_basketball", "sports_tennis", "headphones", "beach_access",
    "theaters", "casino", "nightlife", "sports_golf",
    // Travel
    "hotel", "luggage", "explore", "travel_explore", "sailing",
    "snowboarding", "hiking", "map", "location_on",
    // Education
    "school", "auto_stories", "library_books", "science", "calculate", "menu_book",
    // Bills & Admin
    "receipt_long", "bolt", "local_fire_department", "vpn_lock",
    // Personal & Lifestyle
    "pets", "child_care", "content_cut", "phone_android", "laptop", "work",
    "volunteer_activism", "groups", "child_friendly", "face",
    // General
    "swap_horiz", "favorite", "more_horiz", "help_outline",
    "star", "flag", "label", "bookmark"
)
