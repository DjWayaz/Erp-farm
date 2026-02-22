package com.farmapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class ActivityType(val displayName: String) {
    FERTILIZED("Fertilized"),
    SPRAYED("Sprayed"),
    WEEDED("Weeded"),
    IRRIGATED("Irrigated"),
    THINNED("Thinned"),
    PLANTED("Planted"),
    OTHER("Other")
}

enum class PoultryType(val displayName: String) {
    BROILER("Broiler"),
    LAYER("Layer"),
    KIENYEJI("Kienyeji (Indigenous)")
}

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class TransactionCategory(val displayName: String) {
    SEEDS("Seeds"),
    FEED("Feed"),
    VET("Veterinary"),
    LABOUR("Labour"),
    EQUIPMENT("Equipment"),
    SALES("Sales"),
    FERTILIZER("Fertilizer"),
    PESTICIDE("Pesticide"),
    OTHER("Other")
}

// ─── Crop / Field Entities ────────────────────────────────────────────────────

@Entity(tableName = "fields")
data class FieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sizeHectares: Double,
    val soilType: String? = null,
    val cropType: String,
    val variety: String? = null,
    val plantingDate: LocalDate,
    val expectedHarvestDate: LocalDate? = null,
    val notes: String? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "activities",
    foreignKeys = [ForeignKey(
        entity = FieldEntity::class,
        parentColumns = ["id"],
        childColumns = ["fieldId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fieldId")]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fieldId: Long,
    val date: LocalDate,
    val type: ActivityType,
    val notes: String? = null,
    val cost: Double = 0.0
)

@Entity(
    tableName = "harvests",
    foreignKeys = [ForeignKey(
        entity = FieldEntity::class,
        parentColumns = ["id"],
        childColumns = ["fieldId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fieldId")]
)
data class HarvestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fieldId: Long,
    val date: LocalDate,
    val yieldQuantity: Double,
    val unit: String,
    val sellingPricePerUnit: Double? = null
)

// ─── Poultry Entities ─────────────────────────────────────────────────────────

@Entity(tableName = "poultry_batches")
data class PoultryBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: PoultryType,
    val dateAcquired: LocalDate,
    val initialCount: Int,
    val aliveCount: Int,
    val costPerBird: Double = 0.0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "mortalities",
    foreignKeys = [ForeignKey(
        entity = PoultryBatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("batchId")]
)
data class MortalityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val date: LocalDate,
    val count: Int,
    val cause: String? = null
)

@Entity(
    tableName = "vaccinations",
    foreignKeys = [ForeignKey(
        entity = PoultryBatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("batchId")]
)
data class VaccinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val vaccineName: String,
    val dueDate: LocalDate,
    val administeredDate: LocalDate? = null
)

@Entity(
    tableName = "feed_events",
    foreignKeys = [ForeignKey(
        entity = PoultryBatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("batchId")]
)
data class FeedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val date: LocalDate,
    val bagsUsed: Double,
    val costPerBag: Double
)

@Entity(
    tableName = "egg_counts",
    foreignKeys = [ForeignKey(
        entity = PoultryBatchEntity::class,
        parentColumns = ["id"],
        childColumns = ["batchId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("batchId")]
)
data class EggCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val date: LocalDate,
    val count: Int
)

// ─── Inventory & Finance ──────────────────────────────────────────────────────

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val currentQuantity: Double,
    val consumptionRatePerDay: Double,
    val lowStockThresholdDays: Int = 7
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: TransactionType,
    val category: TransactionCategory,
    val amount: Double,
    val description: String,
    val relatedEntityId: Long? = null,
    val relatedEntityType: String? = null
)

// ─── Pest Guide ───────────────────────────────────────────────────────────────

@Entity(tableName = "pest_guide")
data class PestGuideEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localName: String? = null,
    val affectedCrop: String,
    val symptoms: String,
    val treatment: String,
    val prevention: String? = null,
    val severity: String = "MEDIUM"
)
