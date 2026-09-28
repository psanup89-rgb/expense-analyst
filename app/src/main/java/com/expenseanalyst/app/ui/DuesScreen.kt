package com.expenseanalyst.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.feature.emi.ui.EmiListScreen
import com.expenseanalyst.feature.emi.ui.EmiListViewModel
import com.expenseanalyst.feature.expenses.ui.ReimbursementsScreen
import com.expenseanalyst.feature.expenses.ui.ReimbursementsViewModel
import com.expenseanalyst.feature.loans.ui.LoanListScreen
import com.expenseanalyst.feature.loans.ui.LoanListViewModel
import kotlinx.coroutines.launch

/**
 * The "Dues" bottom-nav tab: EMIs, Loans and Reimbursements as swipeable sub-tabs.
 *
 * Lives in :app because the three lists come from three feature modules (:feature:emi,
 * :feature:loans, :feature:expenses) and features may not import each other. Each list is its
 * own screen in embedded mode (no top bar / back arrow) with its own ViewModel; the ViewModels
 * are created here so the tab row can show their counts. Detail and add/edit screens stay
 * full-screen destinations. The last sub-tab used is remembered across launches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    onEmiClick: (Long) -> Unit,
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onExpenseClick: (Long) -> Unit,
    emiViewModel: EmiListViewModel = hiltViewModel(),
    loanViewModel: LoanListViewModel = hiltViewModel(),
    reimbursementsViewModel: ReimbursementsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val pagerState = rememberPagerState(
        initialPage = prefs.getInt(KEY_LAST_TAB, 0).coerceIn(0, DuesTab.entries.lastIndex),
        pageCount = { DuesTab.entries.size }
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putInt(KEY_LAST_TAB, pagerState.currentPage).apply()
    }

    val emiState by emiViewModel.uiState.collectAsStateWithLifecycle()
    val loanState by loanViewModel.uiState.collectAsStateWithLifecycle()
    val pendingReimbursements by reimbursementsViewModel.pending.collectAsStateWithLifecycle()
    val counts = mapOf(
        DuesTab.EMIS to emiState.activeGroups.size,
        DuesTab.LOANS to loanState.pendingItems.size,
        DuesTab.REIMBURSEMENTS to pendingReimbursements.size
    )

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dues", style = MaterialTheme.typography.titleLarge) },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            DuesTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // weight(fill = false): the label may shrink (ellipsis) but the
                            // badge always keeps its room — "Reimbursements" fills its tab.
                            Text(
                                tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val n = counts[tab] ?: 0
                            if (n > 0) {
                                Spacer(Modifier.width(6.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) { Text("$n") }
                            }
                        }
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (DuesTab.entries[page]) {
                DuesTab.EMIS -> EmiListScreen(
                    onNavigateToDetail = onEmiClick,
                    viewModel = emiViewModel,
                    embedded = true
                )
                DuesTab.LOANS -> LoanListScreen(
                    onAddLoan = onAddLoan,
                    onLoanClick = onLoanClick,
                    onBack = {},
                    viewModel = loanViewModel,
                    embedded = true
                )
                DuesTab.REIMBURSEMENTS -> ReimbursementsScreen(
                    onBack = {},
                    onExpenseClick = onExpenseClick,
                    viewModel = reimbursementsViewModel,
                    embedded = true
                )
            }
        }
    }
}

/** Order is the tab order and the saved index — append new tabs at the end. */
enum class DuesTab(val label: String) {
    EMIS("EMIs"),
    LOANS("Loans"),
    REIMBURSEMENTS("Reimbursements")
}

private const val PREFS = "dues_ui"
private const val KEY_LAST_TAB = "last_tab"
