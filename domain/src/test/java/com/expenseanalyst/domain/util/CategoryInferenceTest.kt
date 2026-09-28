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

    // ── Refund signal beats merchant-keyword matching ────────────────────────

    @Test
    fun `a Keeta refund routes to Refund, not Food, even though Keeta is a Food keyword`() {
        val result = CategoryInference.infer(
            merchant = "Keeta",
            bankName = "Keeta",
            categories = allCategories,
            smsBody = "SAR 31.83 refunded to your payment method on 17 Apr 2026 at 12:34."
        )
        assertEquals("Refund", result?.name)
    }

    @Test
    fun `a normal Keeta order still routes to Food when the body has no refund wording`() {
        val result = CategoryInference.infer(
            merchant = "Keeta",
            bankName = "Keeta",
            categories = allCategories,
            smsBody = "SAR 31.83 charged for your Keeta order on 17 Apr 2026."
        )
        assertEquals("Food", result?.name)
    }

    @Test
    fun `refund signal falls back to merchant keyword matching when Refund category is missing`() {
        val without = allCategories.filterNot { it.name == "Refund" }
        val result = CategoryInference.infer(
            merchant = "Keeta",
            bankName = "Keeta",
            categories = without,
            smsBody = "SAR 31.83 refunded to your payment method on 17 Apr 2026 at 12:34."
        )
        assertEquals("Food", result?.name)
    }

    @Test
    fun `an internal transfer body routes to Transfer`() {
        val result = CategoryInference.infer(
            merchant = "SAMUEL RAJASEKAR",
            bankName = "Al Rajhi Bank",
            categories = allCategories,
            smsBody = "Debit Internal Transfer From:6805 Amount:SR 4000 To:SAMUEL RAJASEKAR"
        )
        assertEquals("Transfer", result?.name)
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

    // ── Sep 2026 Misc audit ─────────────────────────────────────────────────

    private val withNew = allCategories + listOf(cat(17, "Investments"), cat(18, "EMI"), cat(19, "People"))

    @Test
    fun `merchants from the Misc audit find their category`() {
        assertEquals("Rent", infer("Ejar", withNew))
        assertEquals("Groceries", infer("Keemart", withNew))
        assertEquals("Groceries", infer("Lulu Express Sahara Mall", withNew))
        assertEquals("Food", infer("Yammak", withNew))
        assertEquals("Food", infer("ETERNAL LIMITED", withNew))
        assertEquals("Transport", infer("UBR* PEND", withNew))
        assertEquals("Leisure", infer("CINEPOLIS I", withNew))
        assertEquals("Bills", infer("OPENAI *C", withNew))
        assertEquals("Investments", infer("Groww", withNew))
        assertEquals("Investments", infer("INDIANESIGN", withNew))
        assertEquals("Investments", infer("MONTHLYSMALLCAS", withNew))
        assertEquals("EMI", infer("HDFC HOME LOAN", withNew))
        // Tamara confirmations name the shop, sometimes in Arabic
        assertEquals("Shopping", infer("بان هوم - السعودية", withNew))
        assertEquals("Shopping", infer("اكسترا: متاجر", withNew))
        assertEquals("Shopping", infer("Landmark Online", withNew))
        assertEquals("Shopping", infer("Samsung", withNew))
    }

    @Test
    fun `a descriptor the bank cut short still matches its keyword`() {
        assertEquals("Food", infer("HUNGERSTA", withNew))
        assertEquals("Bills", infer("SAUDI ELE", withNew))
        assertEquals("Shopping", infer("LIFE STYL", withNew))
        assertEquals("Shopping", infer("Pan Emira", withNew))
        // Too short to be sure: a bare "Google" must not become "google cloud"
        assertEquals(null, infer("Google", withNew))
    }

    @Test
    fun `traffic fines go to Vehicle, or Transport when there is no Vehicle category`() {
        val body = "MOI Payments-Traffic Violations From:1234 Amount:SR 300"
        assertEquals("Transport", infer("MOI Payments-Traffic Violations", withNew))
        // AlRajhiParser's merchant is just "MOI Payment" — the body carries the fine
        assertEquals(
            "Vehicle",
            CategoryInference.infer("MOI Payment", null, withNew + cat(20, "Vehicle"), smsBody = body)?.name
        )
        assertEquals(
            "Vehicle",
            CategoryInference.infer("Traffic Violation fine", null, withNew + cat(20, "Vehicle"), smsBody = body)?.name
        )
    }

    @Test
    fun `a UPI payment to a person goes to People, a card purchase never does`() {
        fun upi(m: String) = CategoryInference.infer(m, null, withNew, upiDebit = true)?.name
        assertEquals("People", upi("P K SASEEDHARAN"))
        assertEquals("People", upi("Mrs NIRMALAMARY  D"))
        assertEquals("People", upi("MANOJ POOVANNIYIL"))
        // brand keyword wins over the person check
        assertEquals("Food", upi("CREAM STORY"))
        // same name on a card is a truncated business descriptor, not a person
        assertEquals(null, infer("RAYMOND L".replace("RAYMOND", "ROHAN"), withNew))
    }

    @Test
    fun `bank interest goes to Interest`() {
        val cats = allCategories + cat(21, "Interest")
        assertEquals("Interest", infer("Interest", cats))
        assertEquals(
            "Interest",
            CategoryInference.infer("DBS Bank", null, cats, smsBody = "Monthly interest of INR 58 credited")?.name
        )
    }
}
