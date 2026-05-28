package com.distributor.app.ui.model

import com.distributor.app.data.entity.TransactionEntity

data class AllocationPreview(
    val invoice: TransactionEntity,
    val amountApplied: Double,
    val balanceAfter: Double
)
