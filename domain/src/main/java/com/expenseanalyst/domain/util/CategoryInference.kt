package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.MerchantRule

/**
 * Infers an expense category from merchant name and/or bank name using keyword matching.
 * Returns null if no match — callers should fall back to "Misc".
 *
 * [rules] is ORDERED and matching is first-wins, so position is behaviour, not style.
 * Read the comments above "Fuel" and "Leisure" before reordering anything.
 */
object CategoryInference {

    private val rules: List<Pair<String, List<String>>> = listOf(
        "Food" to listOf(
            // India
            "swiggy", "zomato", "domino", "mcdonald", "burger", "pizza", "restaurant",
            "cafe", "kfc", "subway", "haldirams", "barbeque", "biryani", "dunkin",
            "starbucks", "chaayos", "ccd", "coffee day", "dine", "eatery", "food",
            // Saudi / Gulf
            "talabat", "hungerstation", "hunger station", "jahez", "mrsool", "keeta",
            "careem food", "deliveroo", "foodpanda",
            // Global chains
            "hardees", "popeyes", "shake shack", "krispy kreme", "nandos", "fuddruckers",
            "texas roadhouse", "chilis", "outback", "fridays", "applebees", "tim hortons",
            "din tai fung", "sushi", "shawarma", "bakery", "kitchen", "grill", "grills",
            "dining", "meals", "eatout", "takeaway", "takeout"
        ),
        // Fuel MUST precede Transport, which owned these keywords until now.
        // It sits AFTER Food deliberately: Food's "cafe" intercepts "Shell Beach Cafe"
        // before "shell" below can claim it. Moving Fuel first would break that.
        // Deliberately absent: "aramco" (also a major employer — would steal salary
        // credits, since Salary is matched far later), bare "gas" (Bills owns "gas bill"
        // and now runs after this), bare "oil" ("boil"), bare "station" ("hungerstation").
        "Fuel" to listOf(
            // Saudi / Gulf
            "adnoc", "petromin", "sasco", "aldrees", "naft", "enoc", "emarat", "woqod",
            // Global
            "petrol", "fuel", "diesel", "gas station", "shell", "bp ", "caltex",
            "total energies", "totalenergies", "atlas oil", "oilibya", "jax"
        ),
        "Transport" to listOf(
            // India
            "uber", "ola", "rapido", "metro",
            "irctc", "indigo", "spicejet", "air india", "vistara", "goair", "akasa",
            "bus", "taxi", "cab", "ixigo", "redbus", "makemytrip", "cleartrip",
            "blumart", "yulu", "bounce",
            // Saudi / Gulf
            "careem", "jeeny", "saptco", "hafilat", "salik", "nol card",
            // Global
            "airline", "airways", "airport", "flight", "train fare", "bus fare",
            "parking", "toll"
        ),
        "Shopping" to listOf(
            // India
            "amazon", "flipkart", "myntra", "nykaa", "ajio", "meesho", "snapdeal",
            "tatacliq", "reliance digital", "croma", "vijay sales", "lenskart",
            "firstcry", "pepperfry", "urban ladder", "ikea",
            // Saudi / Gulf
            "noon", "namshi", "ounass", "extra stores", "jarir", "home centre",
            "pan emirates", "pottery barn", "bath body", "brands for less",
            "centrepoint", "max fashion", "brand outlet", "souq",
            // Global
            "h&m", "zara", "forever 21", "shein", "temu", "aliexpress",
            "mango", "pull and bear", "massimo dutti", "swarovski", "aldo",
            "charles keith", "virgin megastore", "lulu fashion", "lifestyle"
        ),
        "Bills" to listOf(
            // India
            "electricity", "broadband", "wifi", "jio", "airtel",
            "vodafone", "bsnl", "vi ", " vi", "recharge", "tatapower", "bescom",
            "tneb", "adani electric", "torrent power", "msedcl", "cesc",
            // Saudi / Gulf
            "stc", "mobily", "zain", "we telecom", "etisalat", "du telecom",
            "ooredoo", "oredoo", "dewa", "sewa", "aadc", "addc", "kahramaa",
            "sadad", "fatoorah", "salik recharge", "istimara", "baladiya",
            // General
            "electricity bill", "water bill", "gas bill", "utility", "postpaid",
            "telephone", "internet bill", "insurance", "takaful", "tawuniya",
            "medgulf", "municipality", "bill payment"
        ),
        "Health" to listOf(
            // India
            "pharmacy", "medplus", "apollo", "hospital", "clinic", "doctor",
            "practo", "1mg", "netmeds", "pharmeasy", "lybrate", "healthifyme",
            "cure", "diagnostic", "pathlab", "medicover",
            // Saudi / Gulf
            "aster", "care medical", "sulaiman alhabib", "dr suliman", "al moosa",
            "nmc health", "burjeel", "mediclinic", "polyclinic", "life pharmacy",
            "bin sina", "nahdi", "al dawaa", "saudi german", "ibn sina",
            // General
            "dentist", "optic", "optical", "lab test", "xray", "scan", "medical"
        ),
        "Groceries" to listOf(
            // India
            "bigbasket", "blinkit", "zepto", "dmart", "grofers", "dunzo",
            "jiomart", "reliance fresh", "more supermarket", "spar", "nature's basket",
            "hyperpure", "milkbasket",
            // Saudi / Gulf
            "panda", "hyper panda", "tamimi", "danube", "al raya", "lulu hypermarket",
            "carrefour", "geant", "union coop", "spinneys", "waitrose",
            "al meera", "west zone", "nesto", "safari market", "abc market",
            "choithrams", "organic market",
            // Global
            "supermarket", "hypermarket", "grocery"
        ),
        // Leisure MUST precede Entertainment. It takes the real-world outings that
        // Entertainment used to own; Entertainment keeps the digital subscriptions.
        // "disneyland" is listed here explicitly because Entertainment still holds
        // "disney" — without it a Disneyland ticket would fall to Entertainment.
        "Leisure" to listOf(
            // India
            "bookmyshow", "pvr", "inox",
            // Saudi / Gulf
            "cineco", "vox cinema", "reel cinema", "novo cinema", "muvi", "webook",
            "platinumlist", "riyadh season", "global village", "img world",
            "yas waterworld", "aquaventure", "motiongate", "funzone", "kidzania",
            "legoland", "ferrari world",
            // Global
            "cinema", "theme park", "water park", "adventure", "concert", "event ticket",
            "bowling", "escape room", "laser tag", "soft play", "funland", "disneyland",
            "aquarium", "museum", "paintball", "trampoline", "karting"
        ),
        "Entertainment" to listOf(
            // India
            "netflix", "spotify", "hotstar", "disney", "prime video", "youtube",
            "gaana", "wynk", "zee5", "sonyliv",
            "jiosaavn", "mxplayer", "lionsgate", "apple tv", "steam",
            // Saudi / Gulf
            "shahid", "viu", "starzplay", "jawwy tv",
            // Global
            "twitch", "ea games", "playstation", "xbox"
        ),
        "Education" to listOf(
            "udemy", "coursera", "byju", "unacademy", "vedantu", "simplilearn",
            "upgrad", "school", "college", "university", "tuition", "books",
            "edx", "linkedin learning", "testbook", "gradeup", "british council",
            "ielts", "toefl", "training", "workshop", "certification"
        ),
        "Refund" to listOf("refund", "reversal", "cashback", "cash back", "reimburs"),
        "Salary" to listOf("salary", "payroll", "stipend"),
        "Transfer" to listOf(
            "neft", "rtgs", "imps", "sarie", "loan repayment", "emi payment",
            "federal one", "cibil", "repayment", "fund transfer", "bank transfer",
            "wire transfer", "atm withdrawal", "cash withdrawal"
        )
    )

    /**
     * Where a rule's category may be absent from the user's database, the category that
     * owned those keywords before the split.
     *
     * Fuel and Leisure are recent additions and — unlike the seeded categories — remain
     * user-deletable, so this is reachable. Without it, a matched keyword whose category
     * can't be resolved returns null and the caller drops the expense into Misc, which is
     * strictly worse than the pre-split behaviour.
     */
    private val fallbacks = mapOf(
        "Fuel" to "Transport",
        "Leisure" to "Entertainment"
    )

    /**
     * Finds a category by name, using exact match first, then startsWith fallback.
     * Handles user renames like "Food" → "Food & Drinks".
     */
    private fun findCategory(categories: List<Category>, name: String): Category? =
        categories.find { it.name.equals(name, ignoreCase = true) }
            ?: categories.find { it.name.startsWith(name, ignoreCase = true) }

    /**
     * Returns the matching Category from [categories] list, or null if no keyword matches.
     * Checks user-defined [merchantRules] first, then [merchant]+[bankName] keywords, then [smsBody].
     */
    fun infer(
        merchant: String?,
        bankName: String?,
        categories: List<Category>,
        smsBody: String? = null,
        merchantRules: List<MerchantRule> = emptyList()
    ): Category? {
        // Step 1: User-defined rules (highest priority)
        MerchantRuleMatcher.findMatch(merchant, merchantRules)
            ?.let { rule -> categories.find { it.id == rule.categoryId } }
            ?.let { return it }

        // Step 2: a refund/reversal/cashback signal in the SMS body wins ahead of merchant
        // keyword matching below. Without this, a refund FROM a merchant whose own name is
        // also a spending keyword (e.g. KeetaParser hardcodes merchant="Keeta", and "keeta" is
        // a Food & Drinks keyword for the food-delivery app) lands in that spending category
        // instead of Refund — silently breaking the refund-nets-out-of-spend logic in
        // ExpenseListViewModel, which only nets INCOME rows whose category is "Refund".
        smsBody?.lowercase()?.let { body ->
            if (body.contains("refund") || body.contains("reversal") ||
                body.contains("cashback") || body.contains("cash back") ||
                body.contains("reimburs")
            ) {
                findCategory(categories, "Refund")?.let { return it }
            }
        }

        val searchText = listOfNotNull(merchant, bankName)
            .joinToString(" ")
            .lowercase()

        // Step 3: keyword matching on merchant + bank name
        // Category name lookup uses exact match first, then startsWith to handle user renames
        // e.g. "Food" rule matches "Food & Drinks" if user appended to the default name.
        // If neither the rule's category nor its fallback resolves, continue scanning rather
        // than bailing out — a deleted category shouldn't sink the whole lookup.
        if (searchText.isNotBlank()) {
            for ((categoryName, keywords) in rules) {
                if (keywords.any { searchText.contains(it) }) {
                    findCategory(categories, categoryName)?.let { return it }
                    fallbacks[categoryName]
                        ?.let { fallbackName -> findCategory(categories, fallbackName) }
                        ?.let { return it }
                }
            }
        }

        // Step 4: fallback — check SMS body for other payment-type signals (refund already
        // handled in Step 2)
        val bodyLower = smsBody?.lowercase() ?: return null
        return when {
            bodyLower.contains("salary") || bodyLower.contains("payroll") ||
                bodyLower.contains("stipend") ->
                findCategory(categories, "Salary")
            bodyLower.contains("neft") || bodyLower.contains("rtgs") ||
                bodyLower.contains("imps") || bodyLower.contains("sarie") ||
                bodyLower.contains("fund transfer") || bodyLower.contains("wire transfer") ||
                bodyLower.contains("internal transfer") ->
                findCategory(categories, "Transfer")
            bodyLower.contains("atm") || bodyLower.contains("cash withdrawal") ->
                findCategory(categories, "Transfer")
            else -> null
        }
    }
}
