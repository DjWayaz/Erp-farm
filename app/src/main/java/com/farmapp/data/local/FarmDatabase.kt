package com.farmapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.farmapp.data.local.converters.Converters
import com.farmapp.data.local.dao.*
import com.farmapp.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Database(
    entities = [
        FieldEntity::class,
        ActivityEntity::class,
        HarvestEntity::class,
        PoultryBatchEntity::class,
        MortalityEntity::class,
        VaccinationEntity::class,
        FeedEventEntity::class,
        EggCountEntity::class,
        InventoryItemEntity::class,
        TransactionEntity::class,
        PestGuideEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FarmDatabase : RoomDatabase() {

    abstract fun dashboardDao(): DashboardDao
    abstract fun fieldDao(): FieldDao
    abstract fun poultryDao(): PoultryDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun financeDao(): FinanceDao
    abstract fun pestGuideDao(): PestGuideDao

    companion object {
        @Volatile private var INSTANCE: FarmDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    "farm_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                scope.launch(Dispatchers.IO) {
                                    seedPestGuide(context, database.pestGuideDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedPestGuide(context: Context, dao: PestGuideDao) {
            if (dao.getCount() > 0) return
            try {
                val json = context.assets.open("pest_guide.json")
                    .bufferedReader().use { it.readText() }
                val pests = Json.decodeFromString<List<PestGuideJson>>(json)
                val entities = pests.map {
                    PestGuideEntity(
                        id = it.id,
                        name = it.name,
                        localName = it.localName,
                        affectedCrop = it.affectedCrop,
                        symptoms = it.symptoms,
                        treatment = it.treatment,
                        prevention = it.prevention,
                        severity = it.severity
                    )
                }
                dao.insertAll(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Serializable
data class PestGuideJson(
    val id: String,
    val name: String,
    val localName: String? = null,
    val affectedCrop: String,
    val symptoms: String,
    val treatment: String,
    val prevention: String? = null,
    val severity: String = "MEDIUM"
)
