package com.distributor.app.utils

import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.entity.TransactionDetailEntity
import com.distributor.app.data.entity.TransactionEntity

object DemoDataSeeder {

    suspend fun seed(db: AppDatabase) {
        val productDao  = db.productDao()
        val resellerDao = db.resellerDao()
        val stockDao    = db.stockLedgerDao()
        val txDao       = db.transactionDao()
        val paymentDao  = db.paymentLogDao()

        val now = System.currentTimeMillis()
        fun ago(days: Int): Long = now - days.toLong() * 86_400_000L

        // ── Products ───────────────────────────────────────────────────────
        val pMinyak  = productDao.insertProduct(ProductEntity(name = "Minyak Goreng Bimoli 2L",        sku = "MGR-001", unit = "botol",  basePrice = 28_000.0, sellingPrice = 33_000.0,  stockThreshold = 10.0,  createdAt = ago(60), updatedAt = ago(60)))
        val pGula    = productDao.insertProduct(ProductEntity(name = "Gula Pasir 1kg",                 sku = "GUL-001", unit = "kg",     basePrice = 12_500.0, sellingPrice = 15_000.0,  stockThreshold = 5.0,   createdAt = ago(60), updatedAt = ago(60)))
        val pTepung  = productDao.insertProduct(ProductEntity(name = "Tepung Terigu Segitiga 1kg",     sku = "TPG-001", unit = "kg",     basePrice = 10_000.0, sellingPrice = 12_500.0,  stockThreshold = 8.0,   createdAt = ago(60), updatedAt = ago(60)))
        val pBeras   = productDao.insertProduct(ProductEntity(name = "Beras Premium 5kg",              sku = "BRS-001", unit = "karung", basePrice = 63_000.0, sellingPrice = 72_000.0,  stockThreshold = 20.0,  createdAt = ago(60), updatedAt = ago(60)))
        val pIndomie = productDao.insertProduct(ProductEntity(name = "Indomie Goreng (box 40pcs)",     sku = "IDM-001", unit = "box",    basePrice = 115_000.0,sellingPrice = 132_000.0, stockThreshold = 5.0,   createdAt = ago(60), updatedAt = ago(60)))
        val pSabun   = productDao.insertProduct(ProductEntity(name = "Sabun Lifebuoy (karton 24pcs)",  sku = "SBN-001", unit = "karton", basePrice = 220_000.0,sellingPrice = 252_000.0, stockThreshold = 3.0,   createdAt = ago(60), updatedAt = ago(60)))
        val pAqua    = productDao.insertProduct(ProductEntity(name = "Aqua Galon 19L",                 sku = "AQG-001", unit = "galon",  basePrice = 15_000.0, sellingPrice = 18_500.0,  stockThreshold = 10.0,  createdAt = ago(60), updatedAt = ago(60)))
        val pSusu    = productDao.insertProduct(ProductEntity(name = "Susu Kental Manis Frisian 385g", sku = "SKM-001", unit = "kaleng", basePrice = 14_000.0, sellingPrice = 17_000.0,  stockThreshold = 15.0,  createdAt = ago(60), updatedAt = ago(60)))

        // ── Resellers ──────────────────────────────────────────────────────
        val rSari    = resellerDao.insertReseller(ResellerEntity(name = "Toko Sari Jaya",        phoneNumber = "08123456789", address = "Jl. Mawar No. 12, Jakarta Selatan",  createdAt = ago(55), updatedAt = ago(55)))
        val rBudi    = resellerDao.insertReseller(ResellerEntity(name = "Warung Pak Budi",        phoneNumber = "08234567890", address = "Jl. Melati No. 5, Tangerang",         createdAt = ago(55), updatedAt = ago(55)))
        val rAni     = resellerDao.insertReseller(ResellerEntity(name = "Minimarket Bu Ani",      phoneNumber = "08345678901", address = "Jl. Kenanga No. 8, Bekasi",           createdAt = ago(55), updatedAt = ago(55)))
        val rBerkah  = resellerDao.insertReseller(ResellerEntity(name = "Toko Berkah Mandiri",    phoneNumber = "08456789012", address = "Jl. Flamboyan No. 3, Depok",          createdAt = ago(55), updatedAt = ago(55)))
        val rGrosir  = resellerDao.insertReseller(ResellerEntity(name = "Grosir Maju Jaya",       phoneNumber = "08567890123", address = "Jl. Anggrek No. 15, Bogor",           createdAt = ago(55), updatedAt = ago(55)))

        // ── Initial stock IN ───────────────────────────────────────────────
        // Beras (30) and Aqua (15) set low intentionally — will drop below threshold after sales
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pMinyak,  type = StockLedgerEntity.TYPE_IN, quantity = 100.0, unitCost = 28_000.0,  notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pGula,    type = StockLedgerEntity.TYPE_IN, quantity = 80.0,  unitCost = 12_500.0,  notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pTepung,  type = StockLedgerEntity.TYPE_IN, quantity = 60.0,  unitCost = 10_000.0,  notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pBeras,   type = StockLedgerEntity.TYPE_IN, quantity = 30.0,  unitCost = 63_000.0,  notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pIndomie, type = StockLedgerEntity.TYPE_IN, quantity = 40.0,  unitCost = 115_000.0, notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pSabun,   type = StockLedgerEntity.TYPE_IN, quantity = 20.0,  unitCost = 220_000.0, notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pAqua,    type = StockLedgerEntity.TYPE_IN, quantity = 15.0,  unitCost = 15_000.0,  notes = "Stok awal", createdAt = ago(55)),
            StockLedgerEntity(productId = pSusu,    type = StockLedgerEntity.TYPE_IN, quantity = 60.0,  unitCost = 14_000.0,  notes = "Stok awal", createdAt = ago(55))
        ))

        // ── Transactions ───────────────────────────────────────────────────

        // TX1 — Toko Sari Jaya, 30 days ago, PAID
        // 10 Minyak × 33000 + 5 Gula × 15000 = 405 000
        val tx1 = txDao.insertTransaction(TransactionEntity(
            resellerId = rSari, invoiceNumber = "INV-DEMO-001",
            totalAmount = 405_000.0, amountPaid = 405_000.0,
            status = TransactionEntity.STATUS_PAID, createdAt = ago(30), updatedAt = ago(30)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx1, productId = pMinyak, quantity = 10.0, unitPrice = 33_000.0, subtotal = 330_000.0),
            TransactionDetailEntity(transactionId = tx1, productId = pGula,   quantity = 5.0,  unitPrice = 15_000.0, subtotal =  75_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pMinyak, type = StockLedgerEntity.TYPE_OUT, quantity = 10.0, referenceTransactionId = tx1, createdAt = ago(30)),
            StockLedgerEntity(productId = pGula,   type = StockLedgerEntity.TYPE_OUT, quantity =  5.0, referenceTransactionId = tx1, createdAt = ago(30))
        ))
        paymentDao.insertPaymentLog(PaymentLogEntity(resellerId = rSari, transactionId = tx1, amount = 405_000.0, paymentMethod = PaymentLogEntity.METHOD_CASH, notes = "Lunas tunai", createdAt = ago(30)))

        // TX2 — Warung Pak Budi, 28 days ago, PARTIAL
        // 3 Indomie × 132000 + 2 Sabun × 252000 = 900 000
        val tx2 = txDao.insertTransaction(TransactionEntity(
            resellerId = rBudi, invoiceNumber = "INV-DEMO-002",
            totalAmount = 900_000.0, amountPaid = 500_000.0,
            status = TransactionEntity.STATUS_PARTIAL, createdAt = ago(28), updatedAt = ago(26)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx2, productId = pIndomie, quantity = 3.0, unitPrice = 132_000.0, subtotal = 396_000.0),
            TransactionDetailEntity(transactionId = tx2, productId = pSabun,   quantity = 2.0, unitPrice = 252_000.0, subtotal = 504_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pIndomie, type = StockLedgerEntity.TYPE_OUT, quantity = 3.0, referenceTransactionId = tx2, createdAt = ago(28)),
            StockLedgerEntity(productId = pSabun,   type = StockLedgerEntity.TYPE_OUT, quantity = 2.0, referenceTransactionId = tx2, createdAt = ago(28))
        ))
        paymentDao.insertPaymentLog(PaymentLogEntity(resellerId = rBudi, transactionId = tx2, amount = 500_000.0, paymentMethod = PaymentLogEntity.METHOD_TRANSFER, notes = "DP transfer BCA", createdAt = ago(26)))

        // TX3 — Minimarket Bu Ani, 25 days ago, PAID
        // 5 Beras × 72000 + 8 Tepung × 12500 = 460 000
        val tx3 = txDao.insertTransaction(TransactionEntity(
            resellerId = rAni, invoiceNumber = "INV-DEMO-003",
            totalAmount = 460_000.0, amountPaid = 460_000.0,
            status = TransactionEntity.STATUS_PAID, createdAt = ago(25), updatedAt = ago(25)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx3, productId = pBeras,  quantity = 5.0, unitPrice = 72_000.0, subtotal = 360_000.0),
            TransactionDetailEntity(transactionId = tx3, productId = pTepung, quantity = 8.0, unitPrice = 12_500.0, subtotal = 100_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pBeras,  type = StockLedgerEntity.TYPE_OUT, quantity = 5.0, referenceTransactionId = tx3, createdAt = ago(25)),
            StockLedgerEntity(productId = pTepung, type = StockLedgerEntity.TYPE_OUT, quantity = 8.0, referenceTransactionId = tx3, createdAt = ago(25))
        ))
        paymentDao.insertPaymentLog(PaymentLogEntity(resellerId = rAni, transactionId = tx3, amount = 460_000.0, paymentMethod = PaymentLogEntity.METHOD_CASH, createdAt = ago(25)))

        // TX4 — Toko Berkah Mandiri, 21 days ago, UNPAID
        // 12 Aqua × 18500 + 10 Susu × 17000 = 392 000
        @Suppress("UNUSED_VARIABLE")
        val tx4 = txDao.insertTransaction(TransactionEntity(
            resellerId = rBerkah, invoiceNumber = "INV-DEMO-004",
            totalAmount = 392_000.0, amountPaid = 0.0,
            status = TransactionEntity.STATUS_UNPAID, createdAt = ago(21), updatedAt = ago(21)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx4, productId = pAqua, quantity = 12.0, unitPrice = 18_500.0, subtotal = 222_000.0),
            TransactionDetailEntity(transactionId = tx4, productId = pSusu, quantity = 10.0, unitPrice = 17_000.0, subtotal = 170_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pAqua, type = StockLedgerEntity.TYPE_OUT, quantity = 12.0, referenceTransactionId = tx4, createdAt = ago(21)),
            StockLedgerEntity(productId = pSusu, type = StockLedgerEntity.TYPE_OUT, quantity = 10.0, referenceTransactionId = tx4, createdAt = ago(21))
        ))

        // TX5 — Toko Sari Jaya, 16 days ago, PARTIAL
        // 8 Minyak × 33000 + 6 Indomie × 132000 = 1 056 000
        val tx5 = txDao.insertTransaction(TransactionEntity(
            resellerId = rSari, invoiceNumber = "INV-DEMO-005",
            totalAmount = 1_056_000.0, amountPaid = 600_000.0,
            status = TransactionEntity.STATUS_PARTIAL, createdAt = ago(16), updatedAt = ago(14)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx5, productId = pMinyak,  quantity = 8.0, unitPrice = 33_000.0,  subtotal =  264_000.0),
            TransactionDetailEntity(transactionId = tx5, productId = pIndomie, quantity = 6.0, unitPrice = 132_000.0, subtotal =  792_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pMinyak,  type = StockLedgerEntity.TYPE_OUT, quantity = 8.0, referenceTransactionId = tx5, createdAt = ago(16)),
            StockLedgerEntity(productId = pIndomie, type = StockLedgerEntity.TYPE_OUT, quantity = 6.0, referenceTransactionId = tx5, createdAt = ago(16))
        ))
        paymentDao.insertPaymentLog(PaymentLogEntity(resellerId = rSari, transactionId = tx5, amount = 600_000.0, paymentMethod = PaymentLogEntity.METHOD_TRANSFER, notes = "Transfer BRI", createdAt = ago(14)))

        // TX6 — Grosir Maju Jaya, 12 days ago, UNPAID
        // 20 Beras × 72000 + 15 Minyak × 33000 + 10 Gula × 15000 = 2 085 000
        @Suppress("UNUSED_VARIABLE")
        val tx6 = txDao.insertTransaction(TransactionEntity(
            resellerId = rGrosir, invoiceNumber = "INV-DEMO-006",
            totalAmount = 2_085_000.0, amountPaid = 0.0,
            status = TransactionEntity.STATUS_UNPAID, createdAt = ago(12), updatedAt = ago(12)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx6, productId = pBeras,  quantity = 20.0, unitPrice =  72_000.0, subtotal = 1_440_000.0),
            TransactionDetailEntity(transactionId = tx6, productId = pMinyak, quantity = 15.0, unitPrice =  33_000.0, subtotal =   495_000.0),
            TransactionDetailEntity(transactionId = tx6, productId = pGula,   quantity = 10.0, unitPrice =  15_000.0, subtotal =   150_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pBeras,  type = StockLedgerEntity.TYPE_OUT, quantity = 20.0, referenceTransactionId = tx6, createdAt = ago(12)),
            StockLedgerEntity(productId = pMinyak, type = StockLedgerEntity.TYPE_OUT, quantity = 15.0, referenceTransactionId = tx6, createdAt = ago(12)),
            StockLedgerEntity(productId = pGula,   type = StockLedgerEntity.TYPE_OUT, quantity = 10.0, referenceTransactionId = tx6, createdAt = ago(12))
        ))

        // TX7 — Warung Pak Budi, 9 days ago, UNPAID
        // 5 Sabun × 252000 + 4 Tepung × 12500 = 1 310 000
        @Suppress("UNUSED_VARIABLE")
        val tx7 = txDao.insertTransaction(TransactionEntity(
            resellerId = rBudi, invoiceNumber = "INV-DEMO-007",
            totalAmount = 1_310_000.0, amountPaid = 0.0,
            status = TransactionEntity.STATUS_UNPAID, createdAt = ago(9), updatedAt = ago(9)
        ))
        txDao.insertDetails(listOf(
            TransactionDetailEntity(transactionId = tx7, productId = pSabun,  quantity = 5.0, unitPrice = 252_000.0, subtotal = 1_260_000.0),
            TransactionDetailEntity(transactionId = tx7, productId = pTepung, quantity = 4.0, unitPrice =  12_500.0, subtotal =    50_000.0)
        ))
        stockDao.insertEntries(listOf(
            StockLedgerEntity(productId = pSabun,  type = StockLedgerEntity.TYPE_OUT, quantity = 5.0, referenceTransactionId = tx7, createdAt = ago(9)),
            StockLedgerEntity(productId = pTepung, type = StockLedgerEntity.TYPE_OUT, quantity = 4.0, referenceTransactionId = tx7, createdAt = ago(9))
        ))

    }
}
