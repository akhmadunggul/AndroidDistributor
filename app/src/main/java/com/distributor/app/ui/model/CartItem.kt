package com.distributor.app.ui.model

import com.distributor.app.data.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Double,
    val unitPrice: Double,
    val subtotal: Double
)
