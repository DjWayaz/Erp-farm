@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.poultry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.data.local.entity.PoultryType
import com.farmapp.ui.theme.AmberAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatchScreen(navController: NavController, viewModel: PoultryViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PoultryType.BROILER) }
    var countText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    fun isValid() = name.isNotBlank() && countText.toIntOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Flock") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmberAccent, titleContentColor = Color.Black, navigationIconContentColor = Color.Black)
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Batch Name *") }, placeholder = { Text("e.g., Batch 1, January Broilers") }, modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    label = { Text("Bird Type *") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    PoultryType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) }, onClick = { selectedType = type; typeExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = countText, onValueChange = { countText = it }, label = { Text("Number of Birds *") }, placeholder = { Text("e.g., 100") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Cost Per Bird (USD)") }, placeholder = { Text("e.g., 85") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveBatch(
                        name = name.trim(),
                        type = selectedType,
                        count = countText.toInt(),
                        costPerBird = costText.toDoubleOrNull() ?: 0.0
                    )
                    navController.popBackStack()
                },
                enabled = isValid(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
            ) {
                Text("Add Flock", style = MaterialTheme.typography.titleMedium, color = Color.Black)
            }
        }
    }
}
