package com.asianmobile.emojibattery.shimeji.di

import android.content.Context
import com.asianmobile.emojibattery.shimeji.data.local.DataStoreManager
import com.asianmobile.emojibattery.shimeji.data.repository.OwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetStoreRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.impl.BatteryCatalogParser
import com.asianmobile.emojibattery.shimeji.data.repository.impl.DataStoreBatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.impl.DataStorePetSettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.impl.DataStorePetStoreRepository
import com.asianmobile.emojibattery.shimeji.data.repository.impl.HybridBatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.impl.OwnerPetCatalogParser
import com.asianmobile.emojibattery.shimeji.data.repository.impl.RemoteOwnerPetCatalogRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.FilePetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackArchiveExtractor
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackManifestParser
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): DataStoreManager = DataStoreManager(context)

    @Provides
    @Singleton
    fun providePetPackManifestParser(): PetPackManifestParser = PetPackManifestParser()

    @Provides
    @Singleton
    fun providePetPackValidator(): PetPackValidator = PetPackValidator()

    @Provides
    @Singleton
    fun providePetPackArchiveExtractor(): PetPackArchiveExtractor = PetPackArchiveExtractor()

    @Provides
    @Singleton
    fun provideOwnerPetCatalogParser(): OwnerPetCatalogParser = OwnerPetCatalogParser()

    @Provides
    @Singleton
    fun provideBatteryCatalogParser(): BatteryCatalogParser = BatteryCatalogParser()

    @Provides
    @Singleton
    fun providePetPackRepository(
        repository: FilePetPackRepository
    ): PetPackRepository = repository

    @Provides
    @Singleton
    fun providePetSettingsRepository(
        repository: DataStorePetSettingsRepository
    ): PetSettingsRepository = repository

    @Provides
    @Singleton
    fun providePetStoreRepository(
        repository: DataStorePetStoreRepository
    ): PetStoreRepository = repository

    @Provides
    @Singleton
    fun provideOwnerPetCatalogRepository(
        repository: RemoteOwnerPetCatalogRepository
    ): OwnerPetCatalogRepository = repository

    @Provides
    @Singleton
    fun provideBatteryCatalogRepository(
        repository: HybridBatteryCatalogRepository
    ): BatteryCatalogRepository = repository

    @Provides
    @Singleton
    fun provideBatterySettingsRepository(
        repository: DataStoreBatterySettingsRepository
    ): BatterySettingsRepository = repository
}
