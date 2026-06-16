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
import com.distributor.app.data.dao.PurchasePaymentDao
import com.distributor.app.data.dao.ResellerDao
import com.distributor.app.data.dao.ReturnDao
import com.distributor.app.data.dao.StockLedgerDao
import com.distributor.app.data.dao.SupplierDao
import com.distributor.app.data.dao.TransactionDao
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.PurchasePaymentEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.ReturnDetailEntity
import com.distributor.app.data.entity.ReturnEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.entity.SupplierEntity
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
        ReturnDetailEntity::class,
        SupplierEntity::class,
        PurchasePaymentEntity::class
    ],
    version = 5,
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
    abstract fun supplierDao(): SupplierDao
    abstract fun purchasePaymentDao(): PurchasePaymentDao

    companion object {
        private const val DATABASE_NAME: String = "distributor.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stock_threshold REAL DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE resellers ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name         TEXT NOT NULL,
                        phone_number TEXT NOT NULL DEFAULT '',
                        address      TEXT NOT NULL DEFAULT '',
                        email        TEXT NOT NULL DEFAULT '',
                        notes        TEXT NOT NULL DEFAULT '',
                        created_at   INTEGER NOT NULL,
                        updated_at   INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_payments (
                        id             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        supplier_id    INTEGER NOT NULL,
                        amount         REAL NOT NULL,
                        payment_method TEXT NOT NULL DEFAULT 'CASH',
                        notes          TEXT NOT NULL DEFAULT '',
                        purchase_date  INTEGER NOT NULL,
                        created_at     INTEGER NOT NULL
                    )
                """.trimIndent())
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
