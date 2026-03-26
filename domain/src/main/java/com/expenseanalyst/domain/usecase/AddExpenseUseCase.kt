package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Long = repository.addExpense(expense)
}
