package com.distributor.app.data.model

import androidx.room.ColumnInfo

data class ProductStockSummary(
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "stock")
    val stock: Double
)
