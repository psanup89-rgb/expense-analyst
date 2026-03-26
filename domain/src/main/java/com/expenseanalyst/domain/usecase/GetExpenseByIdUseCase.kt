package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(id: Long): Flow<Expense?> = repository.getExpenseById(id)
}
