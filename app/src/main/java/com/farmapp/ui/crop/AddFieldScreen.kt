@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.crop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.ui.theme.GreenPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddFieldScreen(navController: NavController, viewModel: CropViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("") }
    var cropType by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var soilType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var plantingDate by remember { mutableStateOf(LocalDate.now()) }
    var harvestDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPlantingDatePicker by remember { mutableStateOf(false) }
    var showHarvestDatePicker by remember { mutableStateOf(false) }

    val cropTypes = listOf(
        "Maize", "Beans", "Tomatoes", "Kale", "Cabbage", "Onions", "Potatoes",
        "Wheat", "Sorghum", "Groundnuts", "Sweet Potato", "Cassava",
        "Tobacco", "Cotton", "Sunflower", "Other"
    )
    val soilTypes = listOf("Sandy Loam", "Clay Loam", "Red Sandy", "Black Cotton", "Loam", "Sandy", "Clay")
    var cropExpanded by remember { mutableStateOf(false) }
    var soilExpanded by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun isValid() = name.isNotBlank() && sizeText.toDoubleOrNull() != null && cropType.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Field") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Field Details", style = MaterialTheme.typography.titleSmall, color = GreenPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Field Name *") },
                placeholder = { Text("e.g., Home Garden, Plot A, North Field") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Grass, null) }
            )

            OutlinedTextField(
                value = sizeText,
                onValueChange = { sizeText = it },
                label = { Text("Size (Hectares) *") },
                placeholder = { Text("e.g., 0.5 or 2.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.SquareFoot, null) }
            )

            // Crop Type Dropdown
            ExposedDropdownMenuBox(expanded = cropExpanded, onExpandedChange = { cropExpanded = it }) {
                OutlinedTextField(
                    value = cropType,
                    onValueChange = { cropType = it },
                    label = { Text("Crop Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropExpanded) },
                    leadingIcon = { Icon(Icons.Default.LocalFlorist, null) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = cropExpanded, onDismissRequest = { cropExpanded = false }) {
                    cropTypes.forEach { crop ->
                        DropdownMenuItem(text = { Text(crop) }, onClick = { cropType = crop; cropExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = variety,
                onValueChange = { variety = it },
                label = { Text("Variety (Optional)") },
                placeholder = { Text("e.g., SC403, DK8031, H614D") },
                modifier = Modifier.fillMaxWidth()
            )

            // Soil Type Dropdown
            ExposedDropdownMenuBox(expanded = soilExpanded, onExpandedChange = { soilExpanded = it }) {
                OutlinedTextField(
                    value = soilType,
                    onValueChange = { soilType = it },
                    label = { Text("Soil Type (Optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soilExpanded) },
                    leadingIcon = { Icon(Icons.Default.Terrain, null) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = soilExpanded, onDismissRequest = { soilExpanded = false }) {
                    soilTypes.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { soilType = s; soilExpanded = false })
                    }
                }
            }

            HorizontalDivider()
            Text("Dates", style = MaterialTheme.typography.titleSmall, color = GreenPrimary)

            // Planting Date Picker
            OutlinedTextField(
                value = plantingDate.format(fmt),
                onValueChange = {},
                readOnly = true,
                label = { Text("Planting Date *") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = { showPlantingDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Expected Harvest Date Picker
            OutlinedTextField(
                value = harvestDate?.format(fmt) ?: "Not set (Optional)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Expected Harvest Date") },
                leadingIcon = { Icon(Icons.Default.Celebration, null) },
                trailingIcon = {
                    IconButton(onClick = { showHarvestDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Fertilizer plan, irrigation source, any notes...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isValid()) {
                        viewModel.saveField(
                            name = name.trim(),
                            size = sizeText.toDouble(),
                            cropType = cropType.trim(),
                            variety = variety.trim().ifBlank { null },
                            plantingDate = plantingDate,
                            expectedHarvestDate = harvestDate,
                            soilType = soilType.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null }
                        )
                        navController.popBackStack()
                    }
                },
                enabled = isValid(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Field", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Planting date picker dialog
    if (showPlantingDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = plantingDate.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showPlantingDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        plantingDate = LocalDate.ofEpochDay(millis / 86400000L)
                    }
                    showPlantingDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPlantingDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Harvest date picker dialog
    if (showHarvestDatePicker) {
        val harvestPickerState = rememberDatePickerState(
            initialSelectedDateMillis = (harvestDate ?: plantingDate.plusMonths(3)).toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showHarvestDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    harvestPickerState.selectedDateMillis?.let { millis ->
                        harvestDate = LocalDate.ofEpochDay(millis / 86400000L)
                    }
                    showHarvestDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { harvestDate = null; showHarvestDatePicker = false }) { Text("Clear") }
                    TextButton(onClick = { showHarvestDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = harvestPickerState)
        }
    }
}
