package com.farmapp.ui.crop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.FieldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class CropViewModel @Inject constructor(
    private val repo: FieldRepository
) : ViewModel() {

    val fields: StateFlow<List<FieldEntity>> = repo.getAllActiveFields()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getFieldById(id: Long): Flow<FieldEntity?> = repo.getFieldById(id)
    fun getActivities(fieldId: Long): Flow<List<ActivityEntity>> = repo.getActivitiesForField(fieldId)
    fun getHarvests(fieldId: Long): Flow<List<HarvestEntity>> = repo.getHarvestsForField(fieldId)
    fun getTotalCost(fieldId: Long): Flow<Double> = repo.getTotalCostForField(fieldId)

    fun saveField(
        name: String, size: Double, cropType: String, variety: String?,
        plantingDate: LocalDate, expectedHarvestDate: LocalDate?, soilType: String?, notes: String?
    ) {
        viewModelScope.launch {
            repo.saveField(
                FieldEntity(
                    name = name, sizeHectares = size, cropType = cropType,
                    variety = variety?.ifBlank { null }, plantingDate = plantingDate,
                    expectedHarvestDate = expectedHarvestDate, soilType = soilType?.ifBlank { null },
                    notes = notes?.ifBlank { null }
                )
            )
        }
    }

    fun addActivity(fieldId: Long, type: ActivityType, notes: String?, cost: Double, date: LocalDate) {
        viewModelScope.launch {
            repo.addActivity(ActivityEntity(fieldId = fieldId, date = date, type = type, notes = notes?.ifBlank { null }, cost = cost))
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch { repo.deleteActivity(activity) }
    }

    fun addHarvest(fieldId: Long, quantity: Double, unit: String, sellingPrice: Double?, date: LocalDate) {
        viewModelScope.launch {
            repo.addHarvest(HarvestEntity(fieldId = fieldId, date = date, yieldQuantity = quantity, unit = unit, sellingPricePerUnit = sellingPrice))
        }
    }

    fun archiveField(fieldId: Long) {
        viewModelScope.launch { repo.archiveField(fieldId) }
    }
}
