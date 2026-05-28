package com.distributor.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "returns",
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
        Index(value = ["return_number"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "reseller_id")
    val resellerId: Long,

    @ColumnInfo(name = "return_number")
    val returnNumber: String,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    // UNSOLD | DEFECT | OTHER
    @ColumnInfo(name = "reason")
    val reason: String = REASON_UNSOLD,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val REASON_UNSOLD: String = "UNSOLD"
        const val REASON_DEFECT: String = "DEFECT"
        const val REASON_OTHER: String = "OTHER"
    }
}
