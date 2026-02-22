package com.farmapp.ui.poultry

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.PoultryRepository
import com.farmapp.worker.VaccinationReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class PoultryViewModel @Inject constructor(
    private val repo: PoultryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val batches: StateFlow<List<PoultryBatchEntity>> = repo.getAllActiveBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getBatchById(id: Long): Flow<PoultryBatchEntity?> = repo.getBatchById(id)
    fun getMortalities(batchId: Long): Flow<List<MortalityEntity>> = repo.getMortalitiesForBatch(batchId)
    fun getVaccinations(batchId: Long): Flow<List<VaccinationEntity>> = repo.getVaccinationsForBatch(batchId)
    fun getFeedEvents(batchId: Long): Flow<List<FeedEventEntity>> = repo.getFeedEventsForBatch(batchId)
    fun getEggCounts(batchId: Long): Flow<List<EggCountEntity>> = repo.getRecentEggCountsForBatch(batchId)
    fun getTotalFeedCost(batchId: Long): Flow<Double> = repo.getTotalFeedCostForBatch(batchId)

    fun saveBatch(name: String, type: PoultryType, count: Int, costPerBird: Double) {
        viewModelScope.launch {
            repo.saveBatch(
                PoultryBatchEntity(
                    name = name, type = type, dateAcquired = LocalDate.now(),
                    initialCount = count, aliveCount = count, costPerBird = costPerBird
                )
            )
        }
    }

    fun recordMortality(batchId: Long, count: Int, cause: String?) {
        viewModelScope.launch { repo.recordMortality(batchId, count, cause) }
    }

    fun addVaccination(batchId: Long, batchName: String, vaccineName: String, dueDate: LocalDate) {
        viewModelScope.launch {
            val id = repo.addVaccination(VaccinationEntity(batchId = batchId, vaccineName = vaccineName, dueDate = dueDate))
            val daysFromNow = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
            VaccinationReminderWorker.schedule(context, id, batchName, vaccineName, daysFromNow)
        }
    }

    fun markVaccinationDone(vaccination: VaccinationEntity) {
        viewModelScope.launch {
            repo.markVaccinationDone(vaccination)
            VaccinationReminderWorker.cancel(context, vaccination.id)
        }
    }

    fun deleteVaccination(vaccination: VaccinationEntity) {
        viewModelScope.launch {
            repo.deleteVaccination(vaccination)
            VaccinationReminderWorker.cancel(context, vaccination.id)
        }
    }

    fun addFeedEvent(batchId: Long, bags: Double, costPerBag: Double) {
        viewModelScope.launch {
            repo.addFeedEvent(FeedEventEntity(batchId = batchId, date = LocalDate.now(), bagsUsed = bags, costPerBag = costPerBag))
        }
    }

    fun addEggCount(batchId: Long, count: Int) {
        viewModelScope.launch {
            repo.addEggCount(EggCountEntity(batchId = batchId, date = LocalDate.now(), count = count))
        }
    }

    fun archiveBatch(batchId: Long) {
        viewModelScope.launch { repo.archiveBatch(batchId) }
    }
}
