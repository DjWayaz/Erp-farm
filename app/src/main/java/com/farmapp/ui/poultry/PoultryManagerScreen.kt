package com.farmapp.ui.poultry

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.data.local.entity.PoultryBatchEntity
import com.farmapp.data.local.entity.PoultryType
import com.farmapp.ui.crop.EmptyState
import com.farmapp.ui.navigation.Screen
import com.farmapp.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoultryManagerScreen(navController: NavController, viewModel: PoultryViewModel = hiltViewModel()) {
    val batches by viewModel.batches.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐔 Poultry Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChickenYellow, titleContentColor = Color.Black)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddBatch.route) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Flock") },
                containerColor = AmberAccent
            )
        }
    ) { padding ->
        if (batches.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding), icon = Icons.Default.Egg, title = "No Flocks Yet", subtitle = "Add your first batch of birds")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(batches) { batch ->
                    BatchCard(batch = batch, onClick = { navController.navigate(Screen.BatchDetail.createRoute(batch.id)) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun BatchCard(batch: PoultryBatchEntity, onClick: () -> Unit) {
    val mortality = batch.initialCount - batch.aliveCount
    val mortalityRate = if (batch.initialCount > 0) (mortality.toDouble() / batch.initialCount * 100).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ChickenYellow.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Egg, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(batch.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(batch.type.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text("Alive: ${batch.aliveCount}", fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                    if (mortality > 0) Text("Lost: $mortality ($mortalityRate%)", color = RedAlert, style = MaterialTheme.typography.bodySmall)
                }
                if (batch.type == PoultryType.LAYER) {
                    LinearProgressIndicator(
                        progress = batch.aliveCount.toFloat() / batch.initialCount.coerceAtLeast(1),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = GreenPrimary,
                        trackColor = RedAlert.copy(alpha = 0.2f)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = AmberAccent)
        }
    }
}
