@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.poultry

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
import com.farmapp.data.local.entity.PoultryType
import com.farmapp.data.local.entity.VaccinationEntity
import com.farmapp.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(batchId: Long, navController: NavController, viewModel: PoultryViewModel = hiltViewModel()) {
    val batch by viewModel.getBatchById(batchId).collectAsState(initial = null)
    val mortalities by viewModel.getMortalities(batchId).collectAsState(initial = emptyList())
    val vaccinations by viewModel.getVaccinations(batchId).collectAsState(initial = emptyList())
    val feedEvents by viewModel.getFeedEvents(batchId).collectAsState(initial = emptyList())
    val eggCounts by viewModel.getEggCounts(batchId).collectAsState(initial = emptyList())
    val totalFeedCost by viewModel.getTotalFeedCost(batchId).collectAsState(initial = 0.0)

    var showMortalityDialog by remember { mutableStateOf(false) }
    var showVaccinationDialog by remember { mutableStateOf(false) }
    var showFeedDialog by remember { mutableStateOf(false) }
    var showEggDialog by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(batch?.name ?: "Flock Detail") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.archiveBatch(batchId); navController.popBackStack() }) {
                        Icon(Icons.Default.Archive, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmberAccent, titleContentColor = Color.Black, navigationIconContentColor = Color.Black, actionIconContentColor = Color.Black)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Batch Stats Card
            batch?.let { b ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = ChickenYellow.copy(alpha = 0.15f))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(b.type.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Acquired: ${b.dateAcquired.format(fmt)}", style = MaterialTheme.typography.bodySmall)
                            Divider()
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                StatItem("Initial", b.initialCount.toString(), GreenPrimary)
                                StatItem("Alive", b.aliveCount.toString(), GreenPrimary)
                                StatItem("Mortality", "${b.initialCount - b.aliveCount}", RedAlert)
                                StatItem("Feed Cost", "USD ${String.format("%,.0f", totalFeedCost)}", AmberAccent)
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("💀 Mortality", RedAlert.copy(alpha = 0.15f), RedAlert, Modifier.weight(1f)) { showMortalityDialog = true }
                        ActionButton("💉 Vaccinate", SkyBlue.copy(alpha = 0.15f), SkyBlue, Modifier.weight(1f)) { showVaccinationDialog = true }
                        ActionButton("🌾 Feed", AmberAccent.copy(alpha = 0.15f), AmberAccent, Modifier.weight(1f)) { showFeedDialog = true }
                        if (b.type == PoultryType.LAYER) {
                            ActionButton("🥚 Eggs", GreenPrimary.copy(alpha = 0.15f), GreenPrimary, Modifier.weight(1f)) { showEggDialog = true }
                        }
                    }
                }
            }

            // Vaccinations
            if (vaccinations.isNotEmpty()) {
                item { SectionHeader("💉 Vaccinations") }
                items(vaccinations) { vac ->
                    VaccinationRow(vac, fmt,
                        onMarkDone = { viewModel.markVaccinationDone(vac) },
                        onDelete = { viewModel.deleteVaccination(vac) }
                    )
                }
            }

            // Recent Egg Counts (Layers only)
            if (eggCounts.isNotEmpty()) {
                item { SectionHeader("🥚 Recent Egg Collection") }
                items(eggCounts.take(7)) { egg ->
                    Card(colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f))) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(egg.date.format(fmt))
                            Text("${egg.count} eggs", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
            }

            // Feed Events
            if (feedEvents.isNotEmpty()) {
                item { SectionHeader("🌾 Feed Log") }
                items(feedEvents.take(5)) { feed ->
                    Card(colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.08f))) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${feed.bagsUsed} bags", fontWeight = FontWeight.Bold)
                                Text(feed.date.format(fmt), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("USD ${String.format("%,.0f", feed.bagsUsed * feed.costPerBag)}", color = AmberAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mortalities
            if (mortalities.isNotEmpty()) {
                item { SectionHeader("💀 Mortality Log") }
                items(mortalities.take(5)) { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.07f))) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${m.count} birds", fontWeight = FontWeight.Bold, color = RedAlert)
                                m.cause?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                            Text(m.date.format(fmt), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    val batchName = batch?.name ?: ""

    if (showMortalityDialog) {
        MortalityDialog(
            onDismiss = { showMortalityDialog = false },
            onSave = { count, cause -> viewModel.recordMortality(batchId, count, cause); showMortalityDialog = false }
        )
    }
    if (showVaccinationDialog) {
        VaccinationDialog(
            onDismiss = { showVaccinationDialog = false },
            onSave = { name, date -> viewModel.addVaccination(batchId, batchName, name, date); showVaccinationDialog = false }
        )
    }
    if (showFeedDialog) {
        FeedDialog(
            onDismiss = { showFeedDialog = false },
            onSave = { bags, cost -> viewModel.addFeedEvent(batchId, bags, cost); showFeedDialog = false }
        )
    }
    if (showEggDialog) {
        EggDialog(
            onDismiss = { showEggDialog = false },
            onSave = { count -> viewModel.addEggCount(batchId, count); showEggDialog = false }
        )
    }
}



@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActionButton(label: String, bgColor: Color, contentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor, contentColor = contentColor)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun VaccinationRow(vac: VaccinationEntity, fmt: DateTimeFormatter, onMarkDone: () -> Unit, onDelete: () -> Unit) {
    val isDone = vac.administeredDate != null
    Card(colors = CardDefaults.cardColors(containerColor = if (isDone) GreenPrimary.copy(alpha = 0.08f) else SkyBlue.copy(alpha = 0.08f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vac.vaccineName, fontWeight = FontWeight.Bold)
                Text("Due: ${vac.dueDate.format(fmt)}", style = MaterialTheme.typography.bodySmall)
                if (isDone) Text("✅ Done: ${vac.administeredDate?.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = GreenPrimary)
            }
            if (!isDone) {
                IconButton(onClick = onMarkDone) { Icon(Icons.Default.CheckCircle, "Mark done", tint = GreenPrimary) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun MortalityDialog(onDismiss: () -> Unit, onSave: (Int, String?) -> Unit) {
    var countText by remember { mutableStateOf("") }
    var cause by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Mortality") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = countText, onValueChange = { countText = it }, label = { Text("Number of Birds Lost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cause, onValueChange = { cause = it }, label = { Text("Cause (Optional)") }, placeholder = { Text("e.g., Newcastle disease, injury") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(countText.toIntOrNull() ?: 0, cause.ifBlank { null }) }, enabled = countText.toIntOrNull() != null) { Text("Record") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationDialog(onDismiss: () -> Unit, onSave: (String, LocalDate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("7") }
    val commonVaccines = listOf("Newcastle Disease", "Gumboro (IBD)", "Marek's Disease", "Fowl Pox", "Infectious Bronchitis", "Fowl Typhoid")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Vaccination") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Vaccine Name") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        commonVaccines.forEach { v -> DropdownMenuItem(text = { Text(v) }, onClick = { name = v; expanded = false }) }
                    }
                }
                OutlinedTextField(value = daysText, onValueChange = { daysText = it }, label = { Text("Due in how many days?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val days = daysText.toLongOrNull() ?: 7
                    onSave(name, LocalDate.now().plusDays(days))
                },
                enabled = name.isNotBlank()
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FeedDialog(onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    var bagsText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Feed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = bagsText, onValueChange = { bagsText = it }, label = { Text("Bags Used") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Cost Per Bag (USD)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(bagsText.toDoubleOrNull() ?: 0.0, costText.toDoubleOrNull() ?: 0.0) }, enabled = bagsText.toDoubleOrNull() != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EggDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var countText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Egg Collection") },
        text = {
            OutlinedTextField(value = countText, onValueChange = { countText = it }, label = { Text("Number of Eggs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onSave(countText.toIntOrNull() ?: 0) }, enabled = countText.toIntOrNull() != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
