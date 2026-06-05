package com.expenseanalyst.feature.loans.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.feature.loans.service.LentReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LentReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val lentRepository: LentRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val lentId = inputData.getLong("lent_id", -1L)
        if (lentId == -1L) return Result.success()

        val item = lentRepository.getLentItemById(lentId) ?: return Result.success()
        if (item.status == LentStatus.SETTLED || item.isDeleted) return Result.success()

        LentReminderNotification.post(applicationContext, item)
        return Result.success()
    }
}
