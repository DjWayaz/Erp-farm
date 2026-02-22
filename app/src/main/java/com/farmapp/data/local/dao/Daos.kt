package com.farmapp.data.local.dao

import androidx.room.*
import com.farmapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ─── Dashboard DAO ────────────────────────────────────────────────────────────

@Dao
interface DashboardDao {
    @Query("SELECT COALESCE(SUM(aliveCount), 0) FROM poultry_batches WHERE isActive = 1")
    fun getTotalLiveBirds(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'EXPENSE' AND date >= :monthStart AND date <= :monthEnd
    """)
    fun getMonthlyExpenses(monthStart: Long, monthEnd: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'INCOME' AND date >= :monthStart AND date <= :monthEnd
    """)
    fun getMonthlyIncome(monthStart: Long, monthEnd: Long): Flow<Double>

    @Query("SELECT * FROM fields WHERE isActive = 1 ORDER BY plantingDate DESC")
    fun getActiveFields(): Flow<List<FieldEntity>>

    @Query("SELECT * FROM poultry_batches WHERE isActive = 1 ORDER BY dateAcquired DESC")
    fun getActiveBatches(): Flow<List<PoultryBatchEntity>>

    @Query("""
        SELECT * FROM vaccinations 
        WHERE administeredDate IS NULL AND dueDate >= :today AND dueDate <= :warningDate
        ORDER BY dueDate ASC
    """)
    fun getUpcomingVaccinations(today: Long, warningDate: Long): Flow<List<VaccinationEntity>>

    @Query("""
        SELECT * FROM inventory_items
        WHERE (currentQuantity / CASE WHEN consumptionRatePerDay = 0 THEN 1 ELSE consumptionRatePerDay END) <= lowStockThresholdDays
        AND consumptionRatePerDay > 0
    """)
    fun getLowStockItems(): Flow<List<InventoryItemEntity>>
}

// ─── Field / Crop DAO ─────────────────────────────────────────────────────────

@Dao
interface FieldDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldEntity): Long

    @Update
    suspend fun updateField(field: FieldEntity)

    @Query("UPDATE fields SET isActive = 0 WHERE id = :fieldId")
    suspend fun archiveField(fieldId: Long)

    @Query("SELECT * FROM fields WHERE isActive = 1 ORDER BY plantingDate DESC")
    fun getAllActiveFields(): Flow<List<FieldEntity>>

    @Query("SELECT * FROM fields WHERE id = :fieldId")
    fun getFieldById(fieldId: Long): Flow<FieldEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE fieldId = :fieldId ORDER BY date DESC")
    fun getActivitiesForField(fieldId: Long): Flow<List<ActivityEntity>>

    @Query("SELECT COALESCE(SUM(cost), 0.0) FROM activities WHERE fieldId = :fieldId")
    fun getTotalCostForField(fieldId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHarvest(harvest: HarvestEntity): Long

    @Delete
    suspend fun deleteHarvest(harvest: HarvestEntity)

    @Query("SELECT * FROM harvests WHERE fieldId = :fieldId ORDER BY date DESC")
    fun getHarvestsForField(fieldId: Long): Flow<List<HarvestEntity>>
}

// ─── Poultry DAO ──────────────────────────────────────────────────────────────

@Dao
interface PoultryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: PoultryBatchEntity): Long

    @Update
    suspend fun updateBatch(batch: PoultryBatchEntity)

    @Query("UPDATE poultry_batches SET isActive = 0 WHERE id = :batchId")
    suspend fun archiveBatch(batchId: Long)

    @Query("UPDATE poultry_batches SET aliveCount = aliveCount - :count WHERE id = :batchId")
    suspend fun decrementAliveCount(batchId: Long, count: Int)

    @Query("SELECT * FROM poultry_batches WHERE isActive = 1 ORDER BY dateAcquired DESC")
    fun getAllActiveBatches(): Flow<List<PoultryBatchEntity>>

    @Query("SELECT * FROM poultry_batches WHERE id = :batchId")
    fun getBatchById(batchId: Long): Flow<PoultryBatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMortality(mortality: MortalityEntity): Long

    @Query("SELECT * FROM mortalities WHERE batchId = :batchId ORDER BY date DESC")
    fun getMortalitiesForBatch(batchId: Long): Flow<List<MortalityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(vaccination: VaccinationEntity): Long

    @Update
    suspend fun updateVaccination(vaccination: VaccinationEntity)

    @Delete
    suspend fun deleteVaccination(vaccination: VaccinationEntity)

    @Query("SELECT * FROM vaccinations WHERE batchId = :batchId ORDER BY dueDate ASC")
    fun getVaccinationsForBatch(batchId: Long): Flow<List<VaccinationEntity>>

    @Query("""
        SELECT * FROM vaccinations 
        WHERE administeredDate IS NULL AND dueDate >= :today
        ORDER BY dueDate ASC LIMIT 20
    """)
    fun getUpcomingVaccinations(today: Long): Flow<List<VaccinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedEvent(event: FeedEventEntity): Long

    @Query("SELECT * FROM feed_events WHERE batchId = :batchId ORDER BY date DESC")
    fun getFeedEventsForBatch(batchId: Long): Flow<List<FeedEventEntity>>

    @Query("SELECT COALESCE(SUM(bagsUsed * costPerBag), 0.0) FROM feed_events WHERE batchId = :batchId")
    fun getTotalFeedCostForBatch(batchId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEggCount(eggCount: EggCountEntity): Long

    @Query("SELECT * FROM egg_counts WHERE batchId = :batchId ORDER BY date DESC LIMIT 30")
    fun getRecentEggCountsForBatch(batchId: Long): Flow<List<EggCountEntity>>

    @Query("SELECT COALESCE(SUM(count), 0) FROM egg_counts WHERE batchId = :batchId AND date >= :from AND date <= :to")
    fun getTotalEggsForPeriod(batchId: Long, from: Long, to: Long): Flow<Int>
}

// ─── Inventory DAO ────────────────────────────────────────────────────────────

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    fun getItemById(itemId: Long): Flow<InventoryItemEntity?>

    @Query("""
        UPDATE inventory_items 
        SET currentQuantity = currentQuantity + :quantity 
        WHERE id = :itemId
    """)
    suspend fun addStock(itemId: Long, quantity: Double)

    @Query("""
        UPDATE inventory_items 
        SET currentQuantity = currentQuantity - :quantity 
        WHERE id = :itemId
    """)
    suspend fun removeStock(itemId: Long, quantity: Double)
}

// ─── Finance DAO ──────────────────────────────────────────────────────────────

@Dao
interface FinanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :from AND date <= :to ORDER BY date DESC")
    fun getTransactionsInPeriod(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = :type AND date >= :from AND date <= :to
    """)
    fun getTotalByTypeInPeriod(type: String, from: Long, to: Long): Flow<Double>

    @Query("""
        SELECT category, COALESCE(SUM(amount), 0.0) as total 
        FROM transactions 
        WHERE type = :type AND date >= :from AND date <= :to
        GROUP BY category
    """)
    fun getSummaryByCategory(type: String, from: Long, to: Long): Flow<List<CategorySummary>>

    @Query("SELECT * FROM transactions WHERE relatedEntityId = :entityId AND relatedEntityType = :entityType ORDER BY date DESC")
    fun getTransactionsForEntity(entityId: Long, entityType: String): Flow<List<TransactionEntity>>
}

data class CategorySummary(
    val category: TransactionCategory,
    val total: Double
)

// ─── Pest Guide DAO ───────────────────────────────────────────────────────────

@Dao
interface PestGuideDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(pests: List<PestGuideEntity>)

    @Query("SELECT * FROM pest_guide ORDER BY name ASC")
    fun getAllPests(): Flow<List<PestGuideEntity>>

    @Query("SELECT * FROM pest_guide WHERE name LIKE '%' || :query || '%' OR affectedCrop LIKE '%' || :query || '%' OR localName LIKE '%' || :query || '%'")
    fun searchPests(query: String): Flow<List<PestGuideEntity>>

    @Query("SELECT * FROM pest_guide WHERE affectedCrop = :crop ORDER BY name ASC")
    fun getPestsByCrop(crop: String): Flow<List<PestGuideEntity>>

    @Query("SELECT * FROM pest_guide WHERE id = :id")
    fun getPestById(id: String): Flow<PestGuideEntity?>

    @Query("SELECT COUNT(*) FROM pest_guide")
    suspend fun getCount(): Int
}
