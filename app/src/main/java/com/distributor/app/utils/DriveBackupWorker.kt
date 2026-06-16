package com.distributor.app.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DriveBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val success = DriveBackupManager.backup(applicationContext)
            if (success) {
                BackupStore.saveLastBackupMs(applicationContext, System.currentTimeMillis())
                Result.success()
            } else {
                if (runAttemptCount < 2) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
