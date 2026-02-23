@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.farmapp.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    var currency by remember { mutableStateOf("USD") }
    var darkMode by remember { mutableStateOf(false) }
    var vaccinationNotifs by remember { mutableStateOf(true) }
    var inventoryAlerts by remember { mutableStateOf(true) }
    var harvestReminders by remember { mutableStateOf(true) }
    var defaultAreaUnit by remember { mutableStateOf("Hectares") }
    var defaultWeightUnit by remember { mutableStateOf("kg") }
    var farmerName by remember { mutableStateOf("") }
    var farmName by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings") },
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
                .verticalScroll(rememberScrollState())
        ) {

            // ── FARM PROFILE ──────────────────────────────
            SettingsSectionHeader(title = "🏡 Farm Profile")

            SettingsTextItem(
                icon = Icons.Default.Person,
                title = "Farmer Name",
                value = farmerName.ifBlank { "Not set" },
                onClick = { /* could open edit dialog */ }
            )
            OutlinedTextField(
                value = farmerName,
                onValueChange = { farmerName = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = farmName,
                onValueChange = { farmName = it },
                label = { Text("Farm / Business Name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = province,
                onValueChange = { province = it },
                label = { Text("Province / District") },
                placeholder = { Text("e.g., Mashonaland East") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── CURRENCY & UNITS ──────────────────────────
            SettingsSectionHeader(title = "💱 Currency & Units")

            SettingsClickItem(
                icon = Icons.Default.AttachMoney,
                title = "Currency",
                value = currency,
                onClick = { showCurrencyDialog = true }
            )
            SettingsClickItem(
                icon = Icons.Default.SquareFoot,
                title = "Area Unit",
                value = defaultAreaUnit,
                onClick = { showAreaDialog = true }
            )
            SettingsClickItem(
                icon = Icons.Default.Scale,
                title = "Weight / Yield Unit",
                value = defaultWeightUnit,
                onClick = { showWeightDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── NOTIFICATIONS ─────────────────────────────
            SettingsSectionHeader(title = "🔔 Notifications")

            SettingsToggleItem(
                icon = Icons.Default.Vaccines,
                title = "Vaccination Reminders",
                subtitle = "Notify before scheduled poultry vaccinations",
                checked = vaccinationNotifs,
                onCheckedChange = { vaccinationNotifs = it }
            )
            SettingsToggleItem(
                icon = Icons.Default.Inventory,
                title = "Low Stock Alerts",
                subtitle = "Alert when feed or supplies run low",
                checked = inventoryAlerts,
                onCheckedChange = { inventoryAlerts = it }
            )
            SettingsToggleItem(
                icon = Icons.Default.Agriculture,
                title = "Harvest Reminders",
                subtitle = "Notify when crops approach harvest date",
                checked = harvestReminders,
                onCheckedChange = { harvestReminders = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── CROPS ─────────────────────────────────────
            SettingsSectionHeader(title = "🌱 Crops")

            SettingsInfoItem(
                icon = Icons.Default.Grass,
                title = "Crop Season Reminders",
                subtitle = "Coming soon — planting season alerts for Zimbabwe's rainfall calendar"
            )
            SettingsInfoItem(
                icon = Icons.Default.WaterDrop,
                title = "Irrigation Tracker",
                subtitle = "Coming soon — log watering schedules per field"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── POULTRY ───────────────────────────────────
            SettingsSectionHeader(title = "🐔 Poultry")

            SettingsInfoItem(
                icon = Icons.Default.Egg,
                title = "Egg Production Targets",
                subtitle = "Coming soon — set daily egg targets per flock and track performance"
            )
            SettingsInfoItem(
                icon = Icons.Default.HealthAndSafety,
                title = "Mortality Rate Threshold",
                subtitle = "Coming soon — alert when mortality exceeds your set limit"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── FINANCE ───────────────────────────────────
            SettingsSectionHeader(title = "💰 Finance")

            SettingsToggleItem(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = "Dark Mode",
                subtitle = "Use dark theme throughout the app",
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )
            SettingsInfoItem(
                icon = Icons.Default.Receipt,
                title = "Budget Alerts",
                subtitle = "Coming soon — set monthly spend limits per category"
            )
            SettingsInfoItem(
                icon = Icons.Default.FileDownload,
                title = "Export Reports",
                subtitle = "Coming soon — export financial summaries as CSV or PDF"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── DATA ──────────────────────────────────────
            SettingsSectionHeader(title = "💾 Data & Backup")

            SettingsInfoItem(
                icon = Icons.Default.CloudUpload,
                title = "Google Drive Backup",
                subtitle = "Coming soon — automatic daily backup to your Google Drive"
            )
            SettingsInfoItem(
                icon = Icons.Default.Share,
                title = "Share Data",
                subtitle = "Coming soon — share farm reports with your extension officer or agronomy advisor"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── ABOUT ─────────────────────────────────────
            SettingsSectionHeader(title = "ℹ️ About")

            SettingsClickItem(
                icon = Icons.Default.Info,
                title = "About Zim Farmer",
                value = "v1.0",
                onClick = { showAboutDialog = true }
            )
            SettingsInfoItem(
                icon = Icons.Default.Phone,
                title = "Support",
                subtitle = "For help, contact your local Agritex extension officer"
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // Currency dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    listOf("USD (US Dollar)", "ZWL (Zimbabwe Gold)", "ZAR (Rand)").forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                currency = option.substringBefore(" ")
                                showCurrencyDialog = false
                            }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currency == option.substringBefore(" "), onClick = {
                                currency = option.substringBefore(" "); showCurrencyDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Area unit dialog
    if (showAreaDialog) {
        AlertDialog(
            onDismissRequest = { showAreaDialog = false },
            title = { Text("Select Area Unit") },
            text = {
                Column {
                    listOf("Hectares", "Acres", "Square Meters").forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                defaultAreaUnit = option; showAreaDialog = false
                            }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = defaultAreaUnit == option, onClick = {
                                defaultAreaUnit = option; showAreaDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("🌱 Zim Farmer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Murimi WeNhasi — The Modern Farmer", fontWeight = FontWeight.Bold)
                    Text("Version 1.0")
                    Text("Built for Zimbabwean farmers to manage crops, poultry, inventory and finances — 100% offline.")
                    Text("Designed for smallholder and commercial farmers across Zimbabwe's provinces.")
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    value: String,
    @Suppress("UNUSED_PARAMETER") onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsTextItem(
    icon: ImageVector,
    title: String,
    value: String,
    @Suppress("UNUSED_PARAMETER") onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
