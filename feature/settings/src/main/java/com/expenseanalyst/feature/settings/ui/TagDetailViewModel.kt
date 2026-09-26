package com.expenseanalyst.feature.settings.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.util.SpendClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Expenses carrying one tag. The total uses [SpendClassifier.isSpend] — the same rule as the
 * home screen's Spent — so a tag that also sits on a loan leg or an own-account transfer doesn't
 * report a figure the rest of the app disagrees with.
 */
@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tagRepository: TagRepository,
    expenseRepository: ExpenseRepository,
    currencyRepository: CurrencyRepository
) : ViewModel() {

    private val tagId: Long = checkNotNull(savedStateHandle["tagId"])

    val uiState: StateFlow<TagDetailUiState> = combine(
        tagRepository.getTag(tagId),
        expenseRepository.getExpensesByTag(tagId),
        currencyRepository.getHomeCurrency()
    ) { tag, expenses, home ->
        val spend = expenses.filter(SpendClassifier::isSpend)
        TagDetailUiState(
            isLoading = false,
            tagName = tag?.name.orEmpty(),
            expenses = expenses,
            homeCurrencyCode = home,
            spentTotal = spend.sumOf(SpendClassifier::homeValue),
            notCountedCount = expenses.size - spend.size,
            tagMissing = tag == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagDetailUiState())
}
