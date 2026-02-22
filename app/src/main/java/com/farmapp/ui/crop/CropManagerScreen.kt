package com.farmapp.ui.crop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.data.local.entity.FieldEntity
import com.farmapp.ui.navigation.Screen
import com.farmapp.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropManagerScreen(navController: NavController, viewModel: CropViewModel = hiltViewModel()) {
    val fields by viewModel.fields.collectAsState()
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌱 Crop Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CropGreen, titleContentColor = androidx.compose.ui.graphics.Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddField.route) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Field") },
                containerColor = GreenPrimary
            )
        }
    ) { padding ->
        if (fields.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                icon = Icons.Default.Grass,
                title = "No Fields Yet",
                subtitle = "Tap the button below to add your first field"
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(fields) { field ->
                    FieldCard(field = field, fmt = fmt, onClick = {
                        navController.navigate(Screen.FieldDetail.createRoute(field.id))
                    })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun FieldCard(field: FieldEntity, fmt: DateTimeFormatter, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CropGreen.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Grass, contentDescription = null, tint = CropGreen, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(field.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${field.cropType}${field.variety?.let { " — $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("${field.sizeHectares} ha • Planted: ${field.plantingDate.format(fmt)}", style = MaterialTheme.typography.bodySmall)
                field.expectedHarvestDate?.let {
                    Text("Expected harvest: ${it.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = AmberAccent)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CropGreen)
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
