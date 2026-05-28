package com.distributor.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_ledger",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["type"]),
        Index(value = ["created_at"])
    ]
)
data class StockLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "product_id")
    val productId: Long,

    // Restricted to TYPE_IN, TYPE_OUT, TYPE_ADJUST_IN, TYPE_ADJUST_OUT
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    // Recorded for inbound entries; null for outbound movements
    @ColumnInfo(name = "unit_cost")
    val unitCost: Double? = null,

    // Links outbound ledger entries back to the originating transaction
    @ColumnInfo(name = "reference_transaction_id")
    val referenceTransactionId: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_IN: String = "IN"
        const val TYPE_OUT: String = "OUT"
        const val TYPE_ADJUST_IN: String = "ADJUST_IN"
        const val TYPE_ADJUST_OUT: String = "ADJUST_OUT"
        const val TYPE_RETURN_IN: String = "RETURN_IN"

        val INBOUND_TYPES: Set<String> = setOf(TYPE_IN, TYPE_ADJUST_IN, TYPE_RETURN_IN)
        val OUTBOUND_TYPES: Set<String> = setOf(TYPE_OUT, TYPE_ADJUST_OUT)
    }
}
