package com.distributor.app.utils

import java.text.NumberFormat
import java.util.Locale

private val IDR_LOCALE = Locale("in", "ID")

fun formatRupiah(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(IDR_LOCALE)
    nf.minimumFractionDigits = 0
    nf.maximumFractionDigits = 0
    return "Rp ${nf.format(amount)}"
}
