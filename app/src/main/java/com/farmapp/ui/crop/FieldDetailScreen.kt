@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.crop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.data.local.entity.ActivityEntity
import com.farmapp.data.local.entity.ActivityType
import com.farmapp.data.local.entity.HarvestEntity
import com.farmapp.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldDetailScreen(
    fieldId: Long,
    navController: NavController,
    viewModel: CropViewModel = hiltViewModel()
) {
    val field by viewModel.getFieldById(fieldId).collectAsState(initial = null)
    val activities by viewModel.getActivities(fieldId).collectAsState(initial = emptyList())
    val harvests by viewModel.getHarvests(fieldId).collectAsState(initial = emptyList())
    val totalCost by viewModel.getTotalCost(fieldId).collectAsState(initial = 0.0)

    var showActivityDialog by remember { mutableStateOf(false) }
    var showHarvestDialog by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(field?.name ?: "Field Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.archiveField(fieldId)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Archive, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CropGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { showHarvestDialog = true }, containerColor = AmberAccent) {
                    Icon(Icons.Default.Agriculture, null)
                }
                FloatingActionButton(onClick = { showActivityDialog = true }, containerColor = GreenPrimary) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Field Info Card
            field?.let { f ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = CropGreen.copy(alpha = 0.1f))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${f.cropType}${f.variety?.let { " — $it" } ?: ""}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${f.sizeHectares} hectares • ${f.soilType ?: "Soil type not set"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Planted: ${f.plantingDate.format(fmt)}", style = MaterialTheme.typography.bodySmall)
                            f.expectedHarvestDate?.let { Text("Expected Harvest: ${it.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = AmberAccent) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Input Cost:", fontWeight = FontWeight.SemiBold)
                                Text("USD ${String.format("%,.0f", totalCost)}", fontWeight = FontWeight.Bold, color = RedAlert)
                            }
                        }
                    }
                }
            }

            // Activities Timeline
            if (activities.isNotEmpty()) {
                item {
                    Text("📋 Activity Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(activities) { activity ->
                    ActivityRow(activity = activity, fmt = fmt, onDelete = { viewModel.deleteActivity(activity) })
                }
            }

            // Harvests
            if (harvests.isNotEmpty()) {
                item {
                    Text("🌾 Harvests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(harvests) { harvest ->
                    HarvestRow(harvest = harvest, fmt = fmt)
                }
            }

            if (activities.isEmpty() && harvests.isEmpty()) {
                item {
                    Text(
                        "No activities recorded yet.\nTap + to log fertilizing, spraying, weeding, or a harvest.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (showActivityDialog) {
        AddActivityDialog(
            onDismiss = { showActivityDialog = false },
            onSave = { type, notes, cost ->
                viewModel.addActivity(fieldId, type, notes, cost, LocalDate.now())
                showActivityDialog = false
            }
        )
    }

    if (showHarvestDialog) {
        AddHarvestDialog(
            onDismiss = { showHarvestDialog = false },
            onSave = { qty, unit, price ->
                viewModel.addHarvest(fieldId, qty, unit, price, LocalDate.now())
                showHarvestDialog = false
            }
        )
    }
}

@Composable
fun ActivityRow(activity: ActivityEntity, fmt: DateTimeFormatter, onDelete: () -> Unit) {
    val color = when (activity.type) {
        ActivityType.FERTILIZED -> AmberAccent
        ActivityType.SPRAYED -> SkyBlue
        ActivityType.WEEDED -> CropGreen
        ActivityType.IRRIGATED -> SkyBlue.copy(alpha = 0.7f)
        else -> GreenPrimary
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.type.displayName, fontWeight = FontWeight.Bold, color = color)
                Text(activity.date.format(fmt), style = MaterialTheme.typography.bodySmall)
                activity.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (activity.cost > 0) Text("Cost: USD ${String.format("%,.0f", activity.cost)}", style = MaterialTheme.typography.bodySmall, color = RedAlert)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun HarvestRow(harvest: HarvestEntity, fmt: DateTimeFormatter) {
    Card(colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Agriculture, null, tint = AmberAccent)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${harvest.yieldQuantity} ${harvest.unit}", fontWeight = FontWeight.Bold)
                Text(harvest.date.format(fmt), style = MaterialTheme.typography.bodySmall)
            }
            harvest.sellingPricePerUnit?.let {
                Text("USD ${String.format("%,.0f", it * harvest.yieldQuantity)}", fontWeight = FontWeight.Bold, color = MoneyGreen)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(onDismiss: () -> Unit, onSave: (ActivityType, String?, Double) -> Unit) {

    var selectedType by remember { mutableStateOf(ActivityType.WEEDED) }
    var notes by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        label = { Text("Activity Type") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        ActivityType.values().forEach { type ->
                            DropdownMenuItem(text = { Text(type.displayName) }, onClick = { selectedType = type; typeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Cost (USD)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedType, notes.ifBlank { null }, costText.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHarvestDialog(onDismiss: () -> Unit, onSave: (Double, String, Double?) -> Unit) {
    var quantityText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("90kg bags") }
    var priceText by remember { mutableStateOf("") }
    val units = listOf("90kg bags", "tonnes", "kg", "crates", "pieces")
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Harvest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = quantityText, onValueChange = { quantityText = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; unitExpanded = false }) }
                    }
                }
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Selling Price per Unit (USD, Optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(quantityText.toDoubleOrNull() ?: 0.0, unit, priceText.toDoubleOrNull()) },
                enabled = quantityText.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
