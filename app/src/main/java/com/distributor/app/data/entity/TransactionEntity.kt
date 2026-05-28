package com.distributor.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = ResellerEntity::class,
            parentColumns = ["id"],
            childColumns = ["reseller_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["reseller_id"]),
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "reseller_id")
    val resellerId: Long,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    // Restricted to STATUS_UNPAID, STATUS_PARTIAL, STATUS_PAID
    @ColumnInfo(name = "status")
    val status: String = STATUS_UNPAID,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "amount_paid")
    val amountPaid: Double = 0.0,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_UNPAID: String = "UNPAID"
        const val STATUS_PARTIAL: String = "PARTIAL"
        const val STATUS_PAID: String = "PAID"
    }
}
