package com.farmapp.data.repository

import com.farmapp.data.local.dao.*
import com.farmapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ─── Dashboard Repository ─────────────────────────────────────────────────────

@Singleton
class DashboardRepository @Inject constructor(private val dao: DashboardDao) {

    fun getTotalLiveBirds(): Flow<Int> = dao.getTotalLiveBirds()

    fun getMonthlyExpenses(start: Long, end: Long): Flow<Double> = dao.getMonthlyExpenses(start, end)

    fun getMonthlyIncome(start: Long, end: Long): Flow<Double> = dao.getMonthlyIncome(start, end)

    fun getActiveFields(): Flow<List<FieldEntity>> = dao.getActiveFields()

    fun getActiveBatches(): Flow<List<PoultryBatchEntity>> = dao.getActiveBatches()

    fun getUpcomingVaccinations(today: Long, warningDate: Long): Flow<List<VaccinationEntity>> =
        dao.getUpcomingVaccinations(today, warningDate)

    fun getLowStockItems(): Flow<List<InventoryItemEntity>> = dao.getLowStockItems()
}

// ─── Field Repository ─────────────────────────────────────────────────────────

@Singleton
class FieldRepository @Inject constructor(private val dao: FieldDao) {

    fun getAllActiveFields(): Flow<List<FieldEntity>> = dao.getAllActiveFields()

    fun getFieldById(id: Long): Flow<FieldEntity?> = dao.getFieldById(id)

    suspend fun saveField(field: FieldEntity): Long = dao.insertField(field)

    suspend fun updateField(field: FieldEntity) = dao.updateField(field)

    suspend fun archiveField(id: Long) = dao.archiveField(id)

    fun getActivitiesForField(fieldId: Long): Flow<List<ActivityEntity>> =
        dao.getActivitiesForField(fieldId)

    fun getTotalCostForField(fieldId: Long): Flow<Double> = dao.getTotalCostForField(fieldId)

    suspend fun addActivity(activity: ActivityEntity): Long = dao.insertActivity(activity)

    suspend fun deleteActivity(activity: ActivityEntity) = dao.deleteActivity(activity)

    fun getHarvestsForField(fieldId: Long): Flow<List<HarvestEntity>> =
        dao.getHarvestsForField(fieldId)

    suspend fun addHarvest(harvest: HarvestEntity): Long = dao.insertHarvest(harvest)

    suspend fun deleteHarvest(harvest: HarvestEntity) = dao.deleteHarvest(harvest)
}

// ─── Poultry Repository ───────────────────────────────────────────────────────

@Singleton
class PoultryRepository @Inject constructor(
    private val dao: PoultryDao,
    private val db: com.farmapp.data.local.FarmDatabase
) {

    fun getAllActiveBatches(): Flow<List<PoultryBatchEntity>> = dao.getAllActiveBatches()

    fun getBatchById(id: Long): Flow<PoultryBatchEntity?> = dao.getBatchById(id)

    suspend fun saveBatch(batch: PoultryBatchEntity): Long = dao.insertBatch(batch)

    suspend fun archiveBatch(id: Long) = dao.archiveBatch(id)

    /** Records a mortality and atomically decrements aliveCount in a single transaction */
    suspend fun recordMortality(batchId: Long, count: Int, cause: String?) {
        db.runInTransaction {
            kotlinx.coroutines.runBlocking {
                dao.insertMortality(
                    MortalityEntity(
                        batchId = batchId,
                        date = LocalDate.now(),
                        count = count,
                        cause = cause
                    )
                )
                dao.decrementAliveCount(batchId, count)
            }
        }
    }

    fun getMortalitiesForBatch(batchId: Long): Flow<List<MortalityEntity>> =
        dao.getMortalitiesForBatch(batchId)

    suspend fun addVaccination(vaccination: VaccinationEntity): Long =
        dao.insertVaccination(vaccination)

    suspend fun markVaccinationDone(vaccination: VaccinationEntity) =
        dao.updateVaccination(vaccination.copy(administeredDate = LocalDate.now()))

    suspend fun deleteVaccination(vaccination: VaccinationEntity) =
        dao.deleteVaccination(vaccination)

    fun getVaccinationsForBatch(batchId: Long): Flow<List<VaccinationEntity>> =
        dao.getVaccinationsForBatch(batchId)

    fun getUpcomingVaccinations(today: Long): Flow<List<VaccinationEntity>> =
        dao.getUpcomingVaccinations(today)

    suspend fun addFeedEvent(event: FeedEventEntity): Long = dao.insertFeedEvent(event)

    fun getFeedEventsForBatch(batchId: Long): Flow<List<FeedEventEntity>> =
        dao.getFeedEventsForBatch(batchId)

    fun getTotalFeedCostForBatch(batchId: Long): Flow<Double> =
        dao.getTotalFeedCostForBatch(batchId)

    suspend fun addEggCount(eggCount: EggCountEntity): Long = dao.insertEggCount(eggCount)

    fun getRecentEggCountsForBatch(batchId: Long): Flow<List<EggCountEntity>> =
        dao.getRecentEggCountsForBatch(batchId)
}

// ─── Inventory Repository ─────────────────────────────────────────────────────

@Singleton
class InventoryRepository @Inject constructor(private val dao: InventoryDao) {

    fun getAllItems(): Flow<List<InventoryItemEntity>> = dao.getAllItems()

    fun getItemById(id: Long): Flow<InventoryItemEntity?> = dao.getItemById(id)

    suspend fun saveItem(item: InventoryItemEntity): Long = dao.insertItem(item)

    suspend fun updateItem(item: InventoryItemEntity) = dao.updateItem(item)

    suspend fun deleteItem(item: InventoryItemEntity) = dao.deleteItem(item)

    suspend fun addStock(itemId: Long, quantity: Double) = dao.addStock(itemId, quantity)

    suspend fun removeStock(itemId: Long, quantity: Double) = dao.removeStock(itemId, quantity)
}

// ─── Finance Repository ───────────────────────────────────────────────────────

@Singleton
class FinanceRepository @Inject constructor(private val dao: FinanceDao) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTransactionsInPeriod(from: Long, to: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsInPeriod(from, to)

    fun getTotalByTypeInPeriod(type: String, from: Long, to: Long): Flow<Double> =
        dao.getTotalByTypeInPeriod(type, from, to)

    fun getSummaryByCategory(type: String, from: Long, to: Long): Flow<List<CategorySummary>> =
        dao.getSummaryByCategory(type, from, to)

    suspend fun addTransaction(transaction: TransactionEntity): Long =
        dao.insertTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        dao.deleteTransaction(transaction)
}

// ─── Pest Guide Repository ────────────────────────────────────────────────────

@Singleton
class PestGuideRepository @Inject constructor(private val dao: PestGuideDao) {

    fun getAllPests(): Flow<List<PestGuideEntity>> = dao.getAllPests()

    fun searchPests(query: String): Flow<List<PestGuideEntity>> = dao.searchPests(query)

    fun getPestsByCrop(crop: String): Flow<List<PestGuideEntity>> = dao.getPestsByCrop(crop)

    fun getPestById(id: String): Flow<PestGuideEntity?> = dao.getPestById(id)
}
