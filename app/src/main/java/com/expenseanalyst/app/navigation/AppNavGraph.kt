package com.expenseanalyst.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.expenseanalyst.app.MainViewModel
import com.expenseanalyst.core.navigation.NavRoutes
import com.expenseanalyst.feature.emi.ui.EmiCreateScreen
import com.expenseanalyst.feature.emi.ui.EmiDetailScreen
import com.expenseanalyst.feature.emi.ui.EmiListScreen
import com.expenseanalyst.feature.expenses.ui.AddExpenseScreen
import com.expenseanalyst.feature.expenses.ui.BillDetailScreen
import com.expenseanalyst.feature.expenses.ui.BillsScreen
import com.expenseanalyst.feature.expenses.ui.EditBillScreen
import com.expenseanalyst.feature.expenses.ui.EditExpenseScreen
import com.expenseanalyst.feature.expenses.ui.ExpenseDetailScreen
import com.expenseanalyst.feature.expenses.ui.ExpenseListScreen
import com.expenseanalyst.feature.notification.ui.NotificationBanner
import com.expenseanalyst.feature.notification.ui.PendingInboxScreen
import com.expenseanalyst.feature.notification.ui.SmsImportScreen
import com.expenseanalyst.feature.onboarding.ui.OnboardingScreen
import com.expenseanalyst.feature.analytics.ui.AnalyticsScreen
import com.expenseanalyst.feature.budget.ui.BudgetScreen
import com.expenseanalyst.feature.settings.ui.AccountManagementScreen
import com.expenseanalyst.feature.settings.ui.CategoryManagementScreen
import com.expenseanalyst.feature.settings.ui.SettingsScreen
import com.expenseanalyst.feature.loans.ui.LoanListScreen
import com.expenseanalyst.feature.loans.ui.AddLoanScreen
import com.expenseanalyst.feature.loans.ui.LoanDetailScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.EXPENSE_LIST,
    mainViewModel: MainViewModel? = null,
    modifier: Modifier = Modifier
) {
    // Handle navigation triggered by system notification taps
    val pendingRoute by (mainViewModel?.pendingRoute?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) })
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route)
            mainViewModel?.consumePendingRoute()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = { smsAutoStart ->
                    if (smsAutoStart != null) {
                        navController.navigate(NavRoutes.smsImport(smsAutoStart)) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.EXPENSE_LIST) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.EXPENSE_LIST) {
            Box(Modifier.fillMaxSize()) {
                ExpenseListScreen(
                    onAddExpense = { navController.navigate(NavRoutes.ADD_EXPENSE) },
                    onImportFromSms = { navController.navigate(NavRoutes.SMS_IMPORT) },
                    onExpenseClick = { id -> navController.navigate(NavRoutes.expenseDetail(id)) },
                    onViewAnalytics = { navController.navigate(NavRoutes.ANALYTICS) }
                )
                NotificationBanner(
                    onSave = { parsed, pendingId ->
                        val accountStr = parsed.accountLast4?.let { last4 ->
                            val bank = parsed.bankName.takeIf { it != "Unknown Bank" } ?: ""
                            if (bank.isNotBlank()) "$bank *$last4" else "*$last4"
                        }
                        navController.navigate(
                            NavRoutes.addExpenseFromNotification(
                                amount = parsed.amount,
                                currency = parsed.currencyCode,
                                merchant = parsed.merchant,
                                type = parsed.type.name,
                                account = accountStr,
                                pendingId = pendingId,
                                paymentMethod = parsed.paymentMethodName
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        composable(
            route = NavRoutes.ADD_EXPENSE_ROUTE,
            arguments = listOf(
                navArgument("amount") { type = NavType.StringType; defaultValue = "" },
                navArgument("currency") { type = NavType.StringType; defaultValue = "" },
                navArgument("merchant") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("type") { type = NavType.StringType; defaultValue = "" },
                navArgument("account") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("pendingId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("paymentMethod") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            AddExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    mainViewModel?.dismissBanner()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoutes.EDIT_EXPENSE,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) {
            EditExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EXPENSE_DETAIL,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) {
            ExpenseDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.editExpense(id)) },
                onConvertToEmi = { id -> navController.navigate(NavRoutes.emiCreate(id)) },
                onViewBill = { billId -> navController.navigate(NavRoutes.billDetail(billId)) }
            )
        }

        composable(
            route = NavRoutes.EMI_CREATE,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) {
            EmiCreateScreen(
                onBack = { navController.popBackStack() },
                onCreated = { groupId ->
                    // Pop back to expense list and navigate to EMI detail
                    navController.popBackStack(NavRoutes.EXPENSE_LIST, inclusive = false)
                    navController.navigate(NavRoutes.emiDetail(groupId))
                }
            )
        }

        composable(NavRoutes.PENDING_INBOX) {
            PendingInboxScreen(
                onBack = { navController.popBackStack() },
                onAddExpense = { amount, currency, merchant, type, account, pendingId, paymentMethod ->
                    navController.navigate(
                        NavRoutes.addExpenseFromNotification(
                            amount = amount,
                            currency = currency,
                            merchant = merchant,
                            type = type,
                            account = account,
                            pendingId = pendingId,
                            paymentMethod = paymentMethod
                        )
                    )
                }
            )
        }

        composable(NavRoutes.BILLS) {
            BillsScreen(
                onBillClick = { billId -> navController.navigate(NavRoutes.billDetail(billId)) }
            )
        }

        composable(
            route = NavRoutes.BILL_DETAIL,
            arguments = listOf(navArgument("billId") { type = NavType.LongType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getLong("billId") ?: return@composable
            BillDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(NavRoutes.editBill(billId)) },
                onViewPayment = { expenseId -> navController.navigate(NavRoutes.expenseDetail(expenseId)) }
            )
        }

        composable(
            route = NavRoutes.EDIT_BILL,
            arguments = listOf(navArgument("billId") { type = NavType.LongType })
        ) {
            EditBillScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.EMI_LIST) {
            EmiListScreen(
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.emiDetail(id)) }
            )
        }

        composable(
            route = NavRoutes.EMI_DETAIL,
            arguments = listOf(navArgument("emiGroupId") { type = NavType.LongType })
        ) {
            EmiDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            val context = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                onNavigateToSmsImport = { navController.navigate(NavRoutes.SMS_IMPORT) },
                onTestNotification = { mainViewModel?.testNotification(context) },
                onGrantNotificationAccess = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onNavigateToCategoryManagement = { navController.navigate(NavRoutes.CATEGORY_MANAGEMENT) },
                onNavigateToAccountManagement = { navController.navigate(NavRoutes.ACCOUNT_MANAGEMENT) },
                onNavigateToBudget = { navController.navigate(NavRoutes.BUDGET) },
                onNavigateToLoans = { navController.navigate(NavRoutes.LOANS) }
            )
        }

        composable(NavRoutes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(
                onBack = { navController.popBackStack() },
                onEditExpense = { expenseId -> navController.navigate(NavRoutes.editExpense(expenseId)) }
            )
        }

        composable(NavRoutes.CATEGORY_MANAGEMENT) {
            CategoryManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ANALYTICS) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { id -> navController.navigate(NavRoutes.expenseDetail(id)) }
            )
        }

        composable(NavRoutes.BUDGET) {
            BudgetScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.LOANS) {
            LoanListScreen(
                onAddLoan = { navController.navigate(NavRoutes.ADD_LOAN) },
                onLoanClick = { id -> navController.navigate(NavRoutes.loanDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADD_LOAN) {
            AddLoanScreen(
                loanId = null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EDIT_LOAN,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStack ->
            val loanId = backStack.arguments?.getLong("loanId") ?: return@composable
            AddLoanScreen(
                loanId = loanId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.LOAN_DETAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStack ->
            val loanId = backStack.arguments?.getLong("loanId") ?: return@composable
            LoanDetailScreen(
                loanId = loanId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(NavRoutes.editLoan(id)) }
            )
        }

        composable(
            route = NavRoutes.SMS_IMPORT_ROUTE,
            arguments = listOf(
                navArgument("autoStart") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            SmsImportScreen(
                onBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        // Came from onboarding (start dest was popped) — navigate to expense list
                        navController.navigate(NavRoutes.EXPENSE_LIST) {
                            popUpTo(NavRoutes.SMS_IMPORT_ROUTE) { inclusive = true }
                        }
                    }
                },
                onNavigateWithParsed = { parsed ->
                    val accountStr = parsed.accountLast4?.let { last4 ->
                        val bank = parsed.bankName.takeIf { it != "Unknown Bank" } ?: ""
                        if (bank.isNotBlank()) "$bank *$last4" else "*$last4"
                    }
                    navController.navigate(
                        NavRoutes.addExpenseFromNotification(
                            amount = parsed.amount,
                            currency = parsed.currencyCode,
                            merchant = parsed.merchant,
                            type = parsed.type.name,
                            account = accountStr
                        )
                    )
                },
                onNavigateManually = { navController.navigate(NavRoutes.ADD_EXPENSE) }
            )
        }
    }
}
