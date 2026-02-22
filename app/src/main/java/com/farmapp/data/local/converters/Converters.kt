package com.farmapp.data.local.converters

import androidx.room.TypeConverter
import com.farmapp.data.local.entity.ActivityType
import com.farmapp.data.local.entity.PoultryType
import com.farmapp.data.local.entity.TransactionCategory
import com.farmapp.data.local.entity.TransactionType
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromActivityType(value: String?): ActivityType? = value?.let { ActivityType.valueOf(it) }

    @TypeConverter
    fun activityTypeToString(type: ActivityType?): String? = type?.name

    @TypeConverter
    fun fromPoultryType(value: String?): PoultryType? = value?.let { PoultryType.valueOf(it) }

    @TypeConverter
    fun poultryTypeToString(type: PoultryType?): String? = type?.name

    @TypeConverter
    fun fromTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun transactionTypeToString(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun fromTransactionCategory(value: String?): TransactionCategory? = value?.let { TransactionCategory.valueOf(it) }

    @TypeConverter
    fun transactionCategoryToString(category: TransactionCategory?): String? = category?.name
}
