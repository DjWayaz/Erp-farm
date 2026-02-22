package com.farmapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmapp.data.local.entity.FieldEntity
import com.farmapp.data.local.entity.InventoryItemEntity
import com.farmapp.data.local.entity.PoultryBatchEntity
import com.farmapp.data.local.entity.VaccinationEntity
import com.farmapp.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardUiState(
    val totalLiveBirds: Int = 0,
    val monthlyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val activeFields: List<FieldEntity> = emptyList(),
    val activeBatches: List<PoultryBatchEntity> = emptyList(),
    val upcomingVaccinations: List<VaccinationEntity> = emptyList(),
    val lowStockItems: List<InventoryItemEntity> = emptyList()
) {
    val monthlyProfit: Double get() = monthlyIncome - monthlyExpenses
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DashboardRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val monthStart = now.withDayOfMonth(1).toEpochDay()
    private val monthEnd = now.withDayOfMonth(now.lengthOfMonth()).toEpochDay()
    private val today = now.toEpochDay()
    private val sevenDaysAhead = now.plusDays(7).toEpochDay()

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getTotalLiveBirds(),
        repo.getMonthlyExpenses(monthStart, monthEnd),
        repo.getMonthlyIncome(monthStart, monthEnd),
        repo.getActiveFields(),
        repo.getLowStockItems()
    ) { birds, expenses, income, fields, lowStock ->
        DashboardUiState(
            totalLiveBirds = birds,
            monthlyExpenses = expenses,
            monthlyIncome = income,
            activeFields = fields,
            lowStockItems = lowStock
        )
    }.combine(repo.getActiveBatches()) { state, batches ->
        state.copy(activeBatches = batches)
    }.combine(repo.getUpcomingVaccinations(today, sevenDaysAhead)) { state, vaccinations ->
        state.copy(upcomingVaccinations = vaccinations)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState()
    )
}
