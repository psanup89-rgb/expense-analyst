package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.MerchantRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [CategoryInference.rules] is order-sensitive and first-match-wins, so most of the value here
 * is in the regression and collision cases rather than the happy paths.
 */
class CategoryInferenceTest {

    private fun cat(id: Long, name: String) = Category(
        id = id,
        name = name,
        iconName = "more_horiz",
        colorHex = "#9E9E9E",
        isDefault = true,
        sortOrder = id.toInt()
    )

    private val allCategories = listOf(
        cat(1, "Food"), cat(2, "Transport"), cat(3, "Shopping"), cat(4, "Bills"),
        cat(5, "Entertainment"), cat(6, "Health"), cat(7, "Education"), cat(8, "Groceries"),
        cat(9, "Rent"), cat(10, "Salary"), cat(11, "Transfer"), cat(12, "Other"),
        cat(13, "Misc"), cat(14, "Refund"), cat(15, "Fuel"), cat(16, "Leisure")
    )

    private fun infer(merchant: String, categories: List<Category> = allCategories) =
        CategoryInference.infer(merchant = merchant, bankName = null, categories = categories)?.name

    // ── Fuel ─────────────────────────────────────────────────────────────────

    @Test
    fun `routes Gulf fuel merchants to Fuel`() {
        assertEquals("Fuel", infer("ADNOC"))
        assertEquals("Fuel", infer("Petromin Express"))
        assertEquals("Fuel", infer("SASCO Station"))
        assertEquals("Fuel", infer("Aldrees"))
    }

    @Test
    fun `routes global fuel merchants to Fuel`() {
        assertEquals("Fuel", infer("Shell"))
        assertEquals("Fuel", infer("Caltex"))
        assertEquals("Fuel", infer("TotalEnergies"))
        assertEquals("Fuel", infer("PETROL PUMP 42"))
        assertEquals("Fuel", infer("diesel"))
    }

    // ── Leisure ──────────────────────────────────────────────────────────────

    @Test
    fun `routes real-world outings to Leisure`() {
        assertEquals("Leisure", infer("VOX Cinemas"))
        assertEquals("Leisure", infer("Bowling City"))
        assertEquals("Leisure", infer("KidZania Riyadh"))
        assertEquals("Leisure", infer("webook.com"))
        assertEquals("Leisure", infer("Riyadh Season"))
    }

    @Test
    fun `Disneyland goes to Leisure even though Entertainment keeps disney`() {
        assertEquals("Leisure", infer("Disneyland Paris"))
        assertEquals("Entertainment", infer("Disney Plus"))
    }

    // ── Regressions: the split must not steal anything ───────────────────────

    @Test
    fun `Transport still wins for non-fuel transport merchants`() {
        assertEquals("Transport", infer("Uber"))
        assertEquals("Transport", infer("Careem"))
        assertEquals("Transport", infer("SAPTCO"))
        assertEquals("Transport", infer("Airport Parking"))
        assertEquals("Transport", infer("Salik"))
        assertEquals("Transport", infer("IndiGo"))
    }

    @Test
    fun `Entertainment still wins for digital subscriptions`() {
        assertEquals("Entertainment", infer("Netflix"))
        assertEquals("Entertainment", infer("Spotify"))
        assertEquals("Entertainment", infer("PlayStation Store"))
        assertEquals("Entertainment", infer("Shahid"))
        assertEquals("Entertainment", infer("Steam"))
    }

    // ── Ordering and collision guards ────────────────────────────────────────

    @Test
    fun `Fuel beats Transport when a merchant could match both`() {
        assertEquals("Fuel", infer("ADNOC parking"))
    }

    @Test
    fun `gas bill still routes to Bills — guards against adding a bare gas keyword`() {
        assertEquals("Bills", infer("gas bill"))
    }

    @Test
    fun `hungerstation still routes to Food — guards the station boundary`() {
        assertEquals("Food", infer("HungerStation"))
    }

    @Test
    fun `Shell Beach Cafe routes to Food — this is why Fuel sits after Food`() {
        assertEquals("Food", infer("Shell Beach Cafe"))
    }

    // ── Fallback when the category row is missing ────────────────────────────

    @Test
    fun `falls back to Transport when the Fuel category has been deleted`() {
        val without = allCategories.filterNot { it.name == "Fuel" }
        assertEquals("Transport", infer("ADNOC", without))
    }

    @Test
    fun `falls back to Entertainment when the Leisure category has been deleted`() {
        val without = allCategories.filterNot { it.name == "Leisure" }
        assertEquals("Entertainment", infer("VOX Cinemas", without))
    }

    // ── Name tolerance and precedence ────────────────────────────────────────

    @Test
    fun `matches a renamed category via the startsWith fallback`() {
        val renamed = allCategories.filterNot { it.name == "Fuel" } + cat(99, "Fuel & Petrol")
        assertEquals("Fuel & Petrol", infer("ADNOC", renamed))
    }

    @Test
    fun `a user merchant rule still beats keyword matching`() {
        val rule = MerchantRule(
            id = 1,
            merchantPattern = "adnoc",
            categoryId = 3,
            categoryName = "Shopping",
            createdAt = 0L
        )
        val result = CategoryInference.infer(
            merchant = "ADNOC",
            bankName = null,
            categories = allCategories,
            merchantRules = listOf(rule)
        )
        assertEquals("Shopping", result?.name)
    }
}
