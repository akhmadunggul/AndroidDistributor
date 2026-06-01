package com.distributor.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.distributor.app.data.dao.LedgerDao
import com.distributor.app.data.dao.PaymentLogDao
import com.distributor.app.data.dao.ProductDao
import com.distributor.app.data.dao.ResellerDao
import com.distributor.app.data.dao.ReturnDao
import com.distributor.app.data.dao.StockLedgerDao
import com.distributor.app.data.dao.TransactionDao
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.ReturnDetailEntity
import com.distributor.app.data.entity.ReturnEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.entity.TransactionDetailEntity
import com.distributor.app.data.entity.TransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        ResellerEntity::class,
        StockLedgerEntity::class,
        TransactionEntity::class,
        TransactionDetailEntity::class,
        PaymentLogEntity::class,
        ReturnEntity::class,
        ReturnDetailEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun resellerDao(): ResellerDao
    abstract fun stockLedgerDao(): StockLedgerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentLogDao(): PaymentLogDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun returnDao(): ReturnDao

    companion object {
        private const val DATABASE_NAME: String = "distributor.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stock_threshold REAL DEFAULT NULL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
