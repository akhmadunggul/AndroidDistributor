package com.distributor.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distributor.app.R
import com.distributor.app.ui.components.LanguageMenuIcon

private data class FaqItem(
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int
)

private data class FaqSection(
    @StringRes val titleRes: Int,
    val items: List<FaqItem>
)

private val FAQ_SECTIONS: List<FaqSection> = listOf(
    FaqSection(
        titleRes = R.string.faq_section_general,
        items = listOf(
            FaqItem(R.string.faq_q_what_is_app,      R.string.faq_a_what_is_app),
            FaqItem(R.string.faq_q_need_internet,    R.string.faq_a_need_internet),
            FaqItem(R.string.faq_q_data_backup,      R.string.faq_a_data_backup),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_products,
        items = listOf(
            FaqItem(R.string.faq_q_add_product,      R.string.faq_a_add_product),
            FaqItem(R.string.faq_q_what_is_sku,      R.string.faq_a_what_is_sku),
            FaqItem(R.string.faq_q_threshold,        R.string.faq_a_threshold),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_stoking,
        items = listOf(
            FaqItem(R.string.faq_q_what_is_stoking,  R.string.faq_a_what_is_stoking),
            FaqItem(R.string.faq_q_auto_price,       R.string.faq_a_auto_price),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_opname,
        items = listOf(
            FaqItem(R.string.faq_q_what_is_opname,   R.string.faq_a_what_is_opname),
            FaqItem(R.string.faq_q_when_opname,      R.string.faq_a_when_opname),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_sales,
        items = listOf(
            FaqItem(R.string.faq_q_create_invoice,   R.string.faq_a_create_invoice),
            FaqItem(R.string.faq_q_invoice_status,   R.string.faq_a_invoice_status),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_payment,
        items = listOf(
            FaqItem(R.string.faq_q_record_payment,   R.string.faq_a_record_payment),
            FaqItem(R.string.faq_q_share_receipt,    R.string.faq_a_share_receipt),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_reseller,
        items = listOf(
            FaqItem(R.string.faq_q_add_reseller,     R.string.faq_a_add_reseller),
            FaqItem(R.string.faq_q_return,           R.string.faq_a_return),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_ledger,
        items = listOf(
            FaqItem(R.string.faq_q_what_is_ledger,   R.string.faq_a_what_is_ledger),
            FaqItem(R.string.faq_q_gross_profit,     R.string.faq_a_gross_profit),
        )
    ),
    FaqSection(
        titleRes = R.string.faq_section_settings,
        items = listOf(
            FaqItem(R.string.faq_q_pin_lock,         R.string.faq_a_pin_lock),
            FaqItem(R.string.faq_q_reset_data,       R.string.faq_a_reset_data),
        )
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_faq)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { LanguageMenuIcon() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FAQ_SECTIONS.forEachIndexed { sectionIndex, section ->
                item(key = "header_$sectionIndex") {
                    if (sectionIndex > 0) Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                section.items.forEachIndexed { itemIndex, faqItem ->
                    item(key = "item_${sectionIndex}_$itemIndex") {
                        FaqItemCard(faqItem)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FaqItemCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(item.questionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit  = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(item.answerRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
