package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.ExpenseRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) = repository.updateExpense(expense)
}
