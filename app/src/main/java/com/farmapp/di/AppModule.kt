package com.farmapp.di

import android.content.Context
import com.farmapp.data.local.FarmDatabase
import com.farmapp.data.local.dao.*
import com.farmapp.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideFarmDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): FarmDatabase = FarmDatabase.getDatabase(context, scope)

    @Provides
    fun provideDashboardDao(db: FarmDatabase): DashboardDao = db.dashboardDao()

    @Provides
    fun provideFieldDao(db: FarmDatabase): FieldDao = db.fieldDao()

    @Provides
    fun providePoultryDao(db: FarmDatabase): PoultryDao = db.poultryDao()

    @Provides
    fun provideInventoryDao(db: FarmDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideFinanceDao(db: FarmDatabase): FinanceDao = db.financeDao()

    @Provides
    fun providePestGuideDao(db: FarmDatabase): PestGuideDao = db.pestGuideDao()
}
