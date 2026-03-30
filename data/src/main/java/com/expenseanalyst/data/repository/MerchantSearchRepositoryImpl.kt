package com.expenseanalyst.data.repository

import android.util.Log
import com.expenseanalyst.data.BuildConfig
import com.expenseanalyst.data.remote.GooglePlacesApiService
import com.expenseanalyst.domain.repository.MerchantSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantSearchRepositoryImpl @Inject constructor(
    private val googlePlacesApiService: GooglePlacesApiService
) : MerchantSearchRepository {

    override suspend fun searchMerchantCategory(merchantName: String): String? {
        if (BuildConfig.GOOGLE_PLACES_API_KEY.isBlank()) {
            Log.w(TAG, "Google Places API key is blank — skipping Tier 3")
            return null
        }

        val types = googlePlacesApiService.findPlaceTypes(merchantName) ?: return null
        val category = mapPlaceTypesToCategory(types)
        Log.d(TAG, "Mapped types $types → category=$category")
        return category
    }

    private companion object {
        const val TAG = "MerchantSearchRepo"
    }

    /**
     * Maps Google Places [types] list to an app category name.
     * Ordered most-specific first so that, for example, a grocery store isn't tagged
     * as a generic food merchant.
     */
    private fun mapPlaceTypesToCategory(types: List<String>): String? {
        val typeSet = types.toSet()
        return when {
            typeSet.any {
                it in setOf(
                    "grocery_or_supermarket", "supermarket", "convenience_store"
                )
            } -> "Groceries"

            typeSet.any {
                it in setOf(
                    "cafe", "restaurant", "bakery", "meal_takeaway",
                    "meal_delivery", "bar", "food", "coffee_shop"
                )
            } -> "Food"

            typeSet.any {
                it in setOf(
                    "pharmacy", "hospital", "doctor", "dentist",
                    "physiotherapist", "health", "medical_lab"
                )
            } -> "Health"

            typeSet.any {
                it in setOf(
                    "gas_station", "parking", "transit_station", "airport",
                    "bus_station", "train_station", "subway_station", "taxi_stand",
                    "car_rental"
                )
            } -> "Transport"

            typeSet.any {
                it in setOf(
                    "school", "university", "primary_school", "secondary_school",
                    "library", "book_store"
                )
            } -> "Education"

            typeSet.any {
                it in setOf(
                    "movie_theater", "amusement_park", "night_club", "casino",
                    "bowling_alley", "stadium", "museum", "art_gallery"
                )
            } -> "Entertainment"

            typeSet.any {
                it in setOf(
                    "clothing_store", "electronics_store", "shopping_mall",
                    "department_store", "jewelry_store", "shoe_store",
                    "furniture_store", "home_goods_store", "hardware_store",
                    "store", "market"
                )
            } -> "Shopping"

            else -> null
        }
    }
}
