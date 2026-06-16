package com.distributor.app.utils

import android.content.Context

object BackupStore {
    private const val PREFS    = "backup_prefs"
    private const val KEY_LAST = "last_backup_ms"

    fun saveLastBackupMs(ctx: Context, ms: Long) =
        prefs(ctx).edit().putLong(KEY_LAST, ms).apply()

    fun getLastBackupMs(ctx: Context): Long =
        prefs(ctx).getLong(KEY_LAST, 0L)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
