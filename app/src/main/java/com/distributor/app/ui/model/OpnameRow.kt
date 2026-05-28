package com.distributor.app.ui.model

import com.distributor.app.data.entity.ProductEntity

data class OpnameRow(
    val product: ProductEntity,
    val systemStock: Double,
    val physicalInput: String = "",
    val discrepancy: Double = 0.0
)
