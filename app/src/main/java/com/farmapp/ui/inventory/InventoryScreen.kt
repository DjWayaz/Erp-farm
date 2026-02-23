@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.farmapp.data.local.entity.InventoryItemEntity
import com.farmapp.data.repository.InventoryRepository
import com.farmapp.ui.crop.EmptyState
import com.farmapp.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repo: InventoryRepository
) : ViewModel() {

    val items: StateFlow<List<InventoryItemEntity>> = repo.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveItem(name: String, unit: String, quantity: Double, ratePerDay: Double, thresholdDays: Int) {
        viewModelScope.launch {
            repo.saveItem(InventoryItemEntity(name = name, unit = unit, currentQuantity = quantity, consumptionRatePerDay = ratePerDay, lowStockThresholdDays = thresholdDays))
        }
    }

    fun addStock(itemId: Long, quantity: Double) {
        viewModelScope.launch { repo.addStock(itemId, quantity) }
    }

    fun removeStock(itemId: Long, quantity: Double) {
        viewModelScope.launch { repo.removeStock(itemId, quantity) }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch { repo.deleteItem(item) }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(@Suppress("UNUSED_PARAMETER") navController: NavController, viewModel: InventoryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showStockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📦 Inventory") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlue, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Item") },
                containerColor = SkyBlue
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding), icon = Icons.Default.Inventory, title = "No Inventory Items", subtitle = "Track your feed, medicine, and supplies.", actionLabel = "Add Item", onAction = { showAddDialog = true })
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    val daysLeft = if (item.consumptionRatePerDay > 0) (item.currentQuantity / item.consumptionRatePerDay).roundToInt() else Int.MAX_VALUE
                    val isLow = daysLeft <= item.lowStockThresholdDays
                    InventoryCard(
                        item = item,
                        daysLeft = daysLeft,
                        isLow = isLow,
                        onAddStock = { selectedItem = item; showStockDialog = true },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddInventoryItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, unit, qty, rate, threshold ->
                viewModel.saveItem(name, unit, qty, rate, threshold)
                showAddDialog = false
            }
        )
    }

    selectedItem?.let { item ->
        if (showStockDialog) {
            AdjustStockDialog(
                itemName = item.name,
                unit = item.unit,
                onDismiss = { showStockDialog = false; selectedItem = null },
                onAdd = { qty -> viewModel.addStock(item.id, qty); showStockDialog = false; selectedItem = null },
                onRemove = { qty -> viewModel.removeStock(item.id, qty); showStockDialog = false; selectedItem = null }
            )
        }
    }
}

@Composable
fun InventoryCard(item: InventoryItemEntity, daysLeft: Int, isLow: Boolean, onAddStock: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (isLow) OrangeWarn.copy(alpha = 0.12f) else SkyBlue.copy(alpha = 0.08f)
    val accentColor = if (isLow) OrangeWarn else SkyBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (isLow) {
                            Surface(color = OrangeWarn, shape = MaterialTheme.shapes.small) {
                                Text("LOW STOCK", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${item.currentQuantity} ${item.unit} remaining", style = MaterialTheme.typography.bodyMedium)
                    if (item.consumptionRatePerDay > 0) {
                        Text(
                            if (daysLeft == Int.MAX_VALUE) "Consumption rate not set" else "$daysLeft days remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor
                        )
                    }
                }
                Row {
                    IconButton(onClick = onAddStock) { Icon(Icons.Default.AddCircle, "Adjust stock", tint = accentColor) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (item.consumptionRatePerDay > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (daysLeft.toFloat() / item.lowStockThresholdDays.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isLow) OrangeWarn else GreenPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryItemDialog(onDismiss: () -> Unit, onSave: (String, String, Double, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("bags") }
    var qtyText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("7") }
    val units = listOf("bags", "litres", "kg", "pieces", "sachets", "bottles")
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inventory Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name *") }, placeholder = { Text("e.g., Broiler Feed, Vaccine A") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; unitExpanded = false }) }
                    }
                }
                OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Current Quantity *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rateText, onValueChange = { rateText = it }, label = { Text("Daily Consumption Rate (Optional)") }, placeholder = { Text("e.g., 0.5 bags/day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = thresholdText, onValueChange = { thresholdText = it }, label = { Text("Low Stock Alert (days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), unit, qtyText.toDoubleOrNull() ?: 0.0, rateText.toDoubleOrNull() ?: 0.0, thresholdText.toIntOrNull() ?: 7) },
                enabled = name.isNotBlank() && qtyText.toDoubleOrNull() != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdjustStockDialog(itemName: String, unit: String, onDismiss: () -> Unit, onAdd: (Double) -> Unit, onRemove: (Double) -> Unit) {
    var quantityText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: $itemName") },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Quantity ($unit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { quantityText.toDoubleOrNull()?.let { onRemove(it) } }, enabled = quantityText.toDoubleOrNull() != null) { Text("Remove", color = RedAlert) }
                TextButton(onClick = { quantityText.toDoubleOrNull()?.let { onAdd(it) } }, enabled = quantityText.toDoubleOrNull() != null) { Text("Add") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
