package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.repository.ExpenseRepository
import javax.inject.Inject

class SoftDeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(id: Long) = repository.softDeleteExpense(id)
}
