@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.farmapp.ui.navigation.Screen
import com.farmapp.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getNumberInstance(Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌾 My Farm", fontWeight = FontWeight.Bold)
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Finance Summary Cards ──────────────────────────────────────
            item {
                Text(
                    "This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanceCard(
                        label = "Income",
                        amount = state.monthlyIncome,
                        icon = Icons.Default.TrendingUp,
                        color = MoneyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceCard(
                        label = "Expenses",
                        amount = state.monthlyExpenses,
                        icon = Icons.Default.TrendingDown,
                        color = RedAlert,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceCard(
                        label = "Profit",
                        amount = state.monthlyProfit,
                        icon = Icons.Default.AccountBalance,
                        color = if (state.monthlyProfit >= 0) GreenPrimary else RedAlert,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Quick Stats ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BigStatCard(
                        label = "Live Birds",
                        value = state.totalLiveBirds.toString(),
                        icon = Icons.Default.Egg,
                        color = ChickenYellow,
                        onClick = { navController.navigate(Screen.PoultryManager.route) },
                        modifier = Modifier.weight(1f)
                    )
                    BigStatCard(
                        label = "Active Fields",
                        value = state.activeFields.size.toString(),
                        icon = Icons.Default.Grass,
                        color = CropGreen,
                        onClick = { navController.navigate(Screen.CropManager.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Alerts ────────────────────────────────────────────────────
            if (state.upcomingVaccinations.isNotEmpty()) {
                item {
                    AlertCard(
                        title = "💉 Vaccinations Due (${state.upcomingVaccinations.size})",
                        subtitle = "Tap to manage your flock",
                        color = AmberAccent,
                        onClick = { navController.navigate(Screen.PoultryManager.route) }
                    )
                }
            }

            if (state.lowStockItems.isNotEmpty()) {
                item {
                    AlertCard(
                        title = "⚠️ Low Stock (${state.lowStockItems.size} items)",
                        subtitle = state.lowStockItems.joinToString(", ") { it.name },
                        color = OrangeWarn,
                        onClick = { navController.navigate(Screen.Inventory.route) }
                    )
                }
            }

            // ── Active Batches ────────────────────────────────────────────
            if (state.activeBatches.isNotEmpty()) {
                item {
                    Text(
                        "🐔 Active Flocks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.activeBatches.take(3)) { batch ->
                    BatchSummaryRow(
                        name = batch.name,
                        type = batch.type.displayName,
                        aliveCount = batch.aliveCount,
                        initialCount = batch.initialCount,
                        onClick = { navController.navigate(Screen.BatchDetail.createRoute(batch.id)) }
                    )
                }
                if (state.activeBatches.size > 3) {
                    item {
                        TextButton(onClick = { navController.navigate(Screen.PoultryManager.route) }) {
                            Text("View all ${state.activeBatches.size} flocks →")
                        }
                    }
                }
            }

            // ── Active Fields ─────────────────────────────────────────────
            if (state.activeFields.isNotEmpty()) {
                item {
                    Text(
                        "🌱 Active Fields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.activeFields.take(3)) { field ->
                    FieldSummaryRow(
                        name = field.name,
                        cropType = field.cropType,
                        size = "${field.sizeHectares} ha",
                        onClick = { navController.navigate(Screen.FieldDetail.createRoute(field.id)) }
                    )
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        QuickActionChip("Add Field", Icons.Default.Add) {
                            navController.navigate(Screen.AddField.route)
                        }
                    }
                    item {
                        QuickActionChip("Add Flock", Icons.Default.Add) {
                            navController.navigate(Screen.AddBatch.route)
                        }
                    }
                    item {
                        QuickActionChip("Add Transaction", Icons.Default.Add) {
                            navController.navigate(Screen.Finance.route)
                        }
                    }
                    item {
                        QuickActionChip("Pest Guide", Icons.Default.Search) {
                            navController.navigate(Screen.PestGuide.route)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun FinanceCard(label: String, amount: Double, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                "USD ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BigStatCard(label: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(36.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AlertCard(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = color)
        }
    }
}

@Composable
fun BatchSummaryRow(name: String, type: String, aliveCount: Int, initialCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ChickenYellow.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, fontWeight = FontWeight.Bold)
                Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$aliveCount birds", fontWeight = FontWeight.Bold, color = GreenPrimary)
                val mortality = initialCount - aliveCount
                if (mortality > 0) {
                    Text("$mortality lost", style = MaterialTheme.typography.bodySmall, color = RedAlert)
                }
            }
        }
    }
}

@Composable
fun FieldSummaryRow(name: String, cropType: String, size: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CropGreen.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, fontWeight = FontWeight.Bold)
                Text(cropType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(size, style = MaterialTheme.typography.bodySmall, color = CropGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun QuickActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}
