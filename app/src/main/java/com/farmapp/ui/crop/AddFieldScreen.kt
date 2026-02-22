@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.crop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.ui.theme.GreenPrimary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldScreen(navController: NavController, viewModel: CropViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("") }
    var cropType by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var soilType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val cropTypes = listOf("Maize", "Beans", "Tomatoes", "Kale", "Cabbage", "Onions", "Potatoes", "Wheat", "Sorghum", "Sweet Potato", "Cassava", "Other")
    var cropExpanded by remember { mutableStateOf(false) }

    fun isValid() = name.isNotBlank() && sizeText.toDoubleOrNull() != null && cropType.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Field") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary, titleContentColor = androidx.compose.ui.graphics.Color.White, navigationIconContentColor = androidx.compose.ui.graphics.Color.White)
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Field Name *") }, placeholder = { Text("e.g., North Field, Plot A") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = sizeText, onValueChange = { sizeText = it }, label = { Text("Size (Hectares) *") }, placeholder = { Text("e.g., 0.5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

            // Crop Type Dropdown
            ExposedDropdownMenuBox(expanded = cropExpanded, onExpandedChange = { cropExpanded = it }) {
                OutlinedTextField(
                    value = cropType,
                    onValueChange = { cropType = it },
                    label = { Text("Crop Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = cropExpanded, onDismissRequest = { cropExpanded = false }) {
                    cropTypes.forEach { crop ->
                        DropdownMenuItem(text = { Text(crop) }, onClick = { cropType = crop; cropExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = variety, onValueChange = { variety = it }, label = { Text("Variety (Optional)") }, placeholder = { Text("e.g., DK8031, H614D") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = soilType, onValueChange = { soilType = it }, label = { Text("Soil Type (Optional)") }, placeholder = { Text("e.g., Loam, Clay, Sandy") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, placeholder = { Text("Any additional notes about this field") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveField(
                        name = name.trim(),
                        size = sizeText.toDouble(),
                        cropType = cropType.trim(),
                        variety = variety.trim().ifBlank { null },
                        plantingDate = LocalDate.now(),
                        expectedHarvestDate = null,
                        soilType = soilType.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null }
                    )
                    navController.popBackStack()
                },
                enabled = isValid(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Save Field", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                "* Required fields. Planting date is set to today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
