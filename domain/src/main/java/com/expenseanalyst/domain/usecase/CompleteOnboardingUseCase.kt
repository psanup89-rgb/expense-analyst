package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.repository.OnboardingRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke() = onboardingRepository.setOnboardingCompleted()
}
