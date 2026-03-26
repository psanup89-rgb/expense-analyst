package com.expenseanalyst.core.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val EXPENSE_LIST = "expense_list"
    const val ADD_EXPENSE = "add_expense"
    // Route with optional args for notification pre-fill (amount, currency, merchant, type)
    const val ADD_EXPENSE_ROUTE = "add_expense?amount={amount}&currency={currency}&merchant={merchant}&type={type}&account={account}"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EMI_LIST = "emi_list"
    const val EMI_DETAIL = "emi_detail/{emiGroupId}"
    const val EMI_CREATE = "emi_create/{expenseId}"
    const val SETTINGS = "settings"
    const val CURRENCY_PICKER = "currency_picker"
    const val SMS_IMPORT = "sms_import"
    const val SMS_IMPORT_ROUTE = "sms_import?autoStart={autoStart}"

    fun smsImport(autoStart: String? = null) =
        if (autoStart != null) "sms_import?autoStart=$autoStart" else "sms_import"

    fun editExpense(expenseId: Long) = "edit_expense/$expenseId"
    fun expenseDetail(expenseId: Long) = "expense_detail/$expenseId"
    fun emiDetail(emiGroupId: Long) = "emi_detail/$emiGroupId"
    fun emiCreate(expenseId: Long) = "emi_create/$expenseId"
    fun addExpenseFromNotification(
        amount: Double,
        currency: String,
        merchant: String?,
        type: String,
        account: String? = null
    ): String {
        val merchantParam = merchant?.takeIf { it.isNotBlank() }?.let { "&merchant=$it" } ?: ""
        val accountParam = account?.takeIf { it.isNotBlank() }?.let { "&account=$it" } ?: ""
        return "add_expense?amount=$amount&currency=$currency${merchantParam}&type=$type${accountParam}"
    }
}
