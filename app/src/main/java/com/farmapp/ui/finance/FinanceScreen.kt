@file:OptIn(ExperimentalMaterial3Api::class)
package com.farmapp.ui.finance

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.farmapp.data.local.dao.CategorySummary
import com.farmapp.data.local.entity.TransactionCategory
import com.farmapp.data.local.entity.TransactionEntity
import com.farmapp.data.local.entity.TransactionType
import com.farmapp.data.repository.FinanceRepository
import com.farmapp.ui.crop.EmptyState
import com.farmapp.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val expensesByCategory: List<CategorySummary> = emptyList()
) {
    val profit: Double get() = totalIncome - totalExpenses
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val monthStart = now.withDayOfMonth(1).toEpochDay()
    private val monthEnd = now.withDayOfMonth(now.lengthOfMonth()).toEpochDay()

    val uiState: StateFlow<FinanceUiState> = combine(
        repo.getTransactionsInPeriod(monthStart, monthEnd),
        repo.getTotalByTypeInPeriod(TransactionType.INCOME.name, monthStart, monthEnd),
        repo.getTotalByTypeInPeriod(TransactionType.EXPENSE.name, monthStart, monthEnd),
        repo.getSummaryByCategory(TransactionType.EXPENSE.name, monthStart, monthEnd)
    ) { txns, income, expenses, catSummary ->
        FinanceUiState(transactions = txns, totalIncome = income, totalExpenses = expenses, expensesByCategory = catSummary)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    fun addTransaction(type: TransactionType, category: TransactionCategory, amount: Double, description: String) {
        viewModelScope.launch {
            repo.addTransaction(
                TransactionEntity(date = LocalDate.now(), type = type, category = category, amount = amount, description = description)
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repo.deleteTransaction(transaction) }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(@Suppress("UNUSED_PARAMETER") navController: NavController, viewModel: FinanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("dd MMM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💰 Finance") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MoneyGreen, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Transaction") },
                containerColor = MoneyGreen
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MoneyGreen.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("This Month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            FinanceStat("Income", state.totalIncome, MoneyGreen)
                            FinanceStat("Expenses", state.totalExpenses, RedAlert)
                            FinanceStat("Net Profit", state.profit, if (state.profit >= 0) GreenPrimary else RedAlert)
                        }
                    }
                }
            }

            // Expenses by category
            if (state.expensesByCategory.isNotEmpty()) {
                item {
                    Text("Expenses Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(state.expensesByCategory) { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat.category.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text("USD ${String.format("%,.0f", cat.total)}", fontWeight = FontWeight.SemiBold, color = RedAlert)
                    }
                }
            }

            // Transactions list
            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.AttachMoney, title = "No Transactions", subtitle = "Add your income and expenses to track your farm's finances.", actionLabel = "Add Transaction", onAction = { showAddDialog = true })
                }
            } else {
                item {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(state.transactions) { txn ->
                    TransactionRow(transaction = txn, fmt = fmt, onDelete = { viewModel.deleteTransaction(txn) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, category, amount, description ->
                viewModel.addTransaction(type, category, amount, description)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FinanceStat(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("USD ${String.format("%,.0f", amount)}", fontWeight = FontWeight.ExtraBold, color = color, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TransactionRow(transaction: TransactionEntity, fmt: DateTimeFormatter, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val color = if (isIncome) MoneyGreen else RedAlert

    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, fontWeight = FontWeight.SemiBold)
                Text("${transaction.category.displayName} • ${transaction.date.format(fmt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${if (isIncome) "+" else "-"} USD ${String.format("%,.0f", transaction.amount)}",
                fontWeight = FontWeight.Bold,
                color = color
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onSave: (TransactionType, TransactionCategory, Double, String) -> Unit) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.FEED) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Type toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (type == TransactionType.INCOME) MoneyGreen.copy(0.2f) else RedAlert.copy(0.2f)
                            )
                        )
                    }
                }

                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        TransactionCategory.values().forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { selectedCategory = cat; catExpanded = false })
                        }
                    }
                }

                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (USD) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description *") }, placeholder = { Text("e.g., Bought 10 bags of feed") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedType, selectedCategory, amountText.toDoubleOrNull() ?: 0.0, description.trim()) },
                enabled = amountText.toDoubleOrNull() != null && description.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
