package com.distributor.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.distributor.app.R
import com.distributor.app.utils.ReceiptShareHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfPreviewSheet(
    file: File?,
    isGenerating: Boolean,
    title: String,
    subtitle: String = "",
    resellerEmail: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var renderedPages by remember { mutableStateOf(emptyList<Bitmap>()) }
    var renderError by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        if (file == null) return@LaunchedEffect
        confirmed = false
        renderError = false
        renderedPages = emptyList()
        scale = 1f
        try {
            val pages = withContext(Dispatchers.IO) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val result = mutableListOf<Bitmap>()
                val targetWidth = context.resources.displayMetrics.widthPixels
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val s = targetWidth.toFloat() / page.width
                    val bmp = Bitmap.createBitmap(targetWidth, (page.height * s).toInt(), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    result.add(bmp)
                }
                renderer.close()
                fd.close()
                result.toList()
            }
            renderedPages = pages
        } catch (_: Exception) {
            renderError = true
        }
    }

    val isLoading = isGenerating || (file != null && renderedPages.isEmpty() && !renderError)

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
    }

    ModalBottomSheet(
        onDismissRequest = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        sheetState = sheetState,
        dragHandle = {},
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Actions (above PDF so they stay visible when scrolling) ───────
            HorizontalDivider()
            if (!isLoading && !renderError) {
                if (confirmed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PdfShareOption(
                            icon    = Icons.AutoMirrored.Filled.Send,
                            label   = stringResource(R.string.share_whatsapp),
                            enabled = file != null
                        ) {
                            file?.let { ReceiptShareHandler.shareToWhatsApp(context, it) }
                        }
                        PdfShareOption(
                            icon    = Icons.Default.Business,
                            label   = stringResource(R.string.share_whatsapp_business),
                            enabled = file != null
                        ) {
                            file?.let { ReceiptShareHandler.shareToWhatsAppBusiness(context, it) }
                        }
                        PdfShareOption(
                            icon    = Icons.Default.Email,
                            label   = stringResource(R.string.share_via_email),
                            enabled = file != null
                        ) {
                            file?.let {
                                ReceiptShareHandler.shareViaEmail(
                                    context  = context,
                                    file     = it,
                                    toEmail  = resellerEmail,
                                    subject  = subtitle.ifBlank { title }
                                )
                            }
                        }
                        PdfShareOption(
                            icon    = Icons.Default.Share,
                            label   = stringResource(R.string.share_other_apps),
                            enabled = file != null
                        ) {
                            file?.let { ReceiptShareHandler.shareViaSystemSheet(context, it) }
                        }
                    }
                } else {
                    Button(
                        onClick  = { confirmed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                    ) {
                        Text(stringResource(R.string.share_confirm_button))
                    }
                }
            }
            OutlinedButton(
                onClick  = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
            ) {
                Text(stringResource(if (confirmed) R.string.share_done else R.string.share_not_now))
            }
            HorizontalDivider()

            // ── PDF preview (fills remaining space, scrollable) ───────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.share_generating),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    renderError -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.share_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .transformable(state = transformableState),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(renderedPages) { bmp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfShareOption(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.size(width = 72.dp, height = 32.dp)
        )
    }
}
