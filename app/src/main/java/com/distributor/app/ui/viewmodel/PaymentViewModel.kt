package com.distributor.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.utils.LicensePackage
import com.distributor.app.utils.LicenseService
import com.distributor.app.utils.LicenseStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PaymentUiState {
    object LoadingPackages : PaymentUiState()
    data class SelectPackage(val packages: List<LicensePackage>) : PaymentUiState()
    data class SelectPaymentMethod(val pkg: LicensePackage) : PaymentUiState()
    object CreatingPayment : PaymentUiState()
    data class AwaitingPayment(
        val orderId: String,
        val qrBitmap: Bitmap?,
        val expiryMs: Long,
        val packageName: String,
        val amount: Long,
        val timeLeftMs: Long,
        val qrCodeUrl: String = "",
        val paymentType: String = "qris",
        val deeplinkUrl: String = ""
    ) : PaymentUiState()
    object PaymentSuccess : PaymentUiState()
    object PaymentExpired : PaymentUiState()
    data class Error(val message: String, val retryPackages: Boolean = true) : PaymentUiState()
}

class PaymentViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<PaymentUiState>(PaymentUiState.LoadingPackages)
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    private var selectedPackage: LicensePackage? = null
    private var pollingJob: Job? = null
    private var countdownJob: Job? = null

    init { loadPackages() }

    fun loadPackages() {
        viewModelScope.launch {
            _state.value = PaymentUiState.LoadingPackages
            val pkgs = LicenseService.fetchPackages()
            _state.value = if (pkgs.isNullOrEmpty()) {
                PaymentUiState.Error(
                    "Gagal memuat paket. Periksa koneksi internet dan coba lagi.",
                    retryPackages = true
                )
            } else {
                PaymentUiState.SelectPackage(pkgs)
            }
        }
    }

    fun selectPackage(pkg: LicensePackage) {
        selectedPackage = pkg
        _state.value = PaymentUiState.SelectPaymentMethod(pkg)
    }

    fun selectPaymentMethod(method: String) {
        val pkg = selectedPackage ?: return
        viewModelScope.launch {
            _state.value = PaymentUiState.CreatingPayment
            val ctx         = getApplication<Application>()
            val deviceId    = LicenseStore.getDeviceId(ctx)
            val deviceModel = android.os.Build.MODEL
            val order = LicenseService.createPayment(pkg.id, pkg.price, deviceId, deviceModel, method)
            if (order == null) {
                _state.value = PaymentUiState.Error(
                    "Gagal membuat pembayaran. Periksa koneksi internet dan coba lagi.",
                    retryPackages = false
                )
                return@launch
            }
            val qrBitmap = if (method == "qris" && order.qrString.isNotBlank()) {
                val sizePx = ctx.resources.displayMetrics.widthPixels * 3 / 4
                generateQrBitmap(order.qrString, sizePx)
            } else null
            _state.value = PaymentUiState.AwaitingPayment(
                orderId     = order.orderId,
                qrBitmap    = qrBitmap,
                expiryMs    = order.expiryMs,
                packageName = pkg.name,
                amount      = pkg.price,
                timeLeftMs  = (order.expiryMs - System.currentTimeMillis()).coerceAtLeast(0L),
                qrCodeUrl   = order.qrCodeUrl,
                paymentType = order.paymentType,
                deeplinkUrl = order.deeplinkUrl
            )
            startCountdown(order.expiryMs)
            startPolling(order.orderId)
        }
    }

    fun retryWithSamePackage() { selectedPackage?.let { _state.value = PaymentUiState.SelectPaymentMethod(it) } }

    private fun startCountdown(expiryMs: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val left = (expiryMs - System.currentTimeMillis()).coerceAtLeast(0L)
                (_state.value as? PaymentUiState.AwaitingPayment)?.let {
                    _state.value = it.copy(timeLeftMs = left)
                }
                if (left == 0L) {
                    if (_state.value is PaymentUiState.AwaitingPayment) {
                        stopJobs()
                        _state.value = PaymentUiState.PaymentExpired
                    }
                    break
                }
                delay(1_000)
            }
        }
    }

    private fun startPolling(orderId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val ctx      = getApplication<Application>()
            val deviceId = LicenseStore.getDeviceId(ctx)
            while (_state.value is PaymentUiState.AwaitingPayment) {
                delay(10_000)
                if (_state.value !is PaymentUiState.AwaitingPayment) break
                val result = LicenseService.checkPayment(orderId, deviceId) ?: continue
                when (result.status) {
                    "SETTLEMENT" -> {
                        LicenseStore.updateCheck(
                            ctx,
                            result.licenseStatus,
                            LicenseService.parseExpiryDate(result.expiryDate),
                            result.daysRemaining,
                            label = "Terdaftar"
                        )
                        stopJobs()
                        _state.value = PaymentUiState.PaymentSuccess
                    }
                    "EXPIRE" -> {
                        stopJobs()
                        _state.value = PaymentUiState.PaymentExpired
                    }
                }
            }
        }
    }

    private fun stopJobs() {
        pollingJob?.cancel()
        countdownJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopJobs()
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN          to 1
        )
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
        }
        return bmp
    }
}
