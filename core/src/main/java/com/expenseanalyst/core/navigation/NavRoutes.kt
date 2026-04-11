package com.expenseanalyst.core.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val EXPENSE_LIST = "expense_list"
    const val ADD_EXPENSE = "add_expense"
    // Route with optional args for notification pre-fill (amount, currency, merchant, type)
    const val ADD_EXPENSE_ROUTE = "add_expense?amount={amount}&currency={currency}&merchant={merchant}&type={type}&account={account}&pendingId={pendingId}&paymentMethod={paymentMethod}"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EMI_LIST = "emi_list"
    const val EMI_DETAIL = "emi_detail/{emiGroupId}"
    const val EMI_CREATE = "emi_create/{expenseId}"
    const val SETTINGS = "settings"
    const val PENDING_INBOX = "pending_inbox"
    const val BILLS = "bills"
    const val BILL_DETAIL = "bill_detail/{billId}"
    const val CURRENCY_PICKER = "currency_picker"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ANALYTICS = "analytics"
    const val SMS_IMPORT = "sms_import"
    const val SMS_IMPORT_ROUTE = "sms_import?autoStart={autoStart}"

    fun smsImport(autoStart: String? = null) =
        if (autoStart != null) "sms_import?autoStart=$autoStart" else "sms_import"

    fun billDetail(billId: Long) = "bill_detail/$billId"
    fun editExpense(expenseId: Long) = "edit_expense/$expenseId"
    fun expenseDetail(expenseId: Long) = "expense_detail/$expenseId"
    fun emiDetail(emiGroupId: Long) = "emi_detail/$emiGroupId"
    fun emiCreate(expenseId: Long) = "emi_create/$expenseId"
    fun addExpenseFromNotification(
        amount: Double,
        currency: String,
        merchant: String?,
        type: String,
        account: String? = null,
        pendingId: Long? = null,
        paymentMethod: String? = null
    ): String {
        val merchantParam = merchant?.takeIf { it.isNotBlank() }?.let { "&merchant=$it" } ?: ""
        val accountParam = account?.takeIf { it.isNotBlank() }?.let { "&account=$it" } ?: ""
        val pendingParam = pendingId?.let { "&pendingId=$it" } ?: ""
        val pmParam = paymentMethod?.takeIf { it.isNotBlank() }?.let { "&paymentMethod=$it" } ?: ""
        return "add_expense?amount=$amount&currency=$currency${merchantParam}&type=$type${accountParam}${pendingParam}${pmParam}"
    }
}
