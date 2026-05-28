package com.distributor.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_logs",
    foreignKeys = [
        ForeignKey(
            entity = ResellerEntity::class,
            parentColumns = ["id"],
            childColumns = ["reseller_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["reseller_id"]),
        Index(value = ["transaction_id"]),
        Index(value = ["created_at"])
    ]
)
data class PaymentLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "reseller_id")
    val resellerId: Long,

    // Null when payment is allocated across multiple invoices by the FIFO engine
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    // Restricted to METHOD_CASH, METHOD_TRANSFER, METHOD_OTHER
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = METHOD_CASH,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val METHOD_CASH: String = "CASH"
        const val METHOD_TRANSFER: String = "TRANSFER"
        const val METHOD_OTHER: String = "OTHER"
    }
}
