package com.expenseanalyst.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expenseanalyst.app.navigation.AppNavGraph
import com.expenseanalyst.app.ui.MainBottomNav
import com.expenseanalyst.core.navigation.NavRoutes
import com.expenseanalyst.core.theme.ExpenseAnalystTheme
import com.expenseanalyst.feature.notification.service.TransactionAlertNotification
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent {
            ExpenseAnalystTheme {
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

                // Wait until we know onboarding status before rendering nav
                if (isOnboardingCompleted == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@ExpenseAnalystTheme
                }

                // Lock startDestination to the value at first render — prevents NavHost from
                // resetting when isOnboardingCompleted flips from false→true after completeOnboarding()
                val startDestination = remember {
                    if (isOnboardingCompleted == true) NavRoutes.EXPENSE_LIST else NavRoutes.ONBOARDING
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomNav = currentRoute in listOf(
                    NavRoutes.EXPENSE_LIST,
                    NavRoutes.EMI_LIST,
                    NavRoutes.SETTINGS
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            MainBottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(NavRoutes.EXPENSE_LIST) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        mainViewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == TransactionAlertNotification.ACTION_OPEN_ADD_EXPENSE) {
            val amount = intent.getDoubleExtra(TransactionAlertNotification.EXTRA_AMOUNT, 0.0)
            val currency = intent.getStringExtra(TransactionAlertNotification.EXTRA_CURRENCY) ?: "SAR"
            val merchant = intent.getStringExtra(TransactionAlertNotification.EXTRA_MERCHANT)
            val type = intent.getStringExtra(TransactionAlertNotification.EXTRA_TYPE) ?: "DEBIT"
            val route = NavRoutes.addExpenseFromNotification(
                amount = amount,
                currency = currency,
                merchant = merchant,
                type = type
            )
            viewModel.setPendingRoute(route)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }
}
