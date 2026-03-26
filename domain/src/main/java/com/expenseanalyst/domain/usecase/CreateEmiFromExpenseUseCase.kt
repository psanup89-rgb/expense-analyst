package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.EmiRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.pow

class CreateEmiFromExpenseUseCase @Inject constructor(
    private val emiRepository: EmiRepository,
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(
        expense: Expense,
        numberOfMonths: Int,
        annualInterestRate: Double = 0.0,
        startDate: kotlinx.datetime.Instant = expense.date
    ): Long {
        require(numberOfMonths >= 2) { "EMI requires at least 2 months" }
        require(numberOfMonths <= 60) { "EMI cannot exceed 60 months" }
        require(expense.amount > 0) { "Expense amount must be positive" }

        val installmentAmount = calculateInstallment(expense.amount, numberOfMonths, annualInterestRate)

        val emiGroup = EmiGroup(
            totalAmount = expense.amount,
            currencyCode = expense.currencyCode,
            numberOfInstallments = numberOfMonths,
            installmentAmount = installmentAmount,
            interestRate = annualInterestRate.takeIf { it > 0 },
            startDate = startDate,
            description = expense.description,
            category = expense.category,
            paymentMethod = expense.paymentMethod
        )
        val groupId = emiRepository.createEmiGroup(emiGroup)

        // Generate N installment expense entries
        for (i in 1..numberOfMonths) {
            val tz = TimeZone.currentSystemDefault()
            val ldt = startDate.toLocalDateTime(tz)
            val newDate = ldt.date.plus(i - 1, DateTimeUnit.MONTH)
            val installmentDate = LocalDateTime(newDate, ldt.time).toInstant(tz)
            val installment = expense.copy(
                id = if (i == 1) expense.id else 0, // reuse original for first
                amount = installmentAmount,
                homeAmount = expense.homeAmount?.let { it / expense.amount * installmentAmount },
                date = installmentDate,
                description = expense.description,
                sourceType = SourceType.MANUAL,
                emiGroupId = groupId,
                emiInstallmentNumber = i
            )
            if (i == 1 && expense.id != 0L) {
                expenseRepository.updateExpense(installment)
            } else {
                expenseRepository.addExpense(installment)
            }
        }

        return groupId
    }

    private fun calculateInstallment(principal: Double, months: Int, annualRate: Double): Double {
        if (annualRate <= 0) return principal / months
        val monthlyRate = annualRate / 12.0 / 100.0
        val factor = (1 + monthlyRate).pow(months.toDouble())
        return principal * monthlyRate * factor / (factor - 1)
    }
}
