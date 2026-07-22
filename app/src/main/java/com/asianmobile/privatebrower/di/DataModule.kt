package com.asianmobile.privatebrower.di

import android.content.Context
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.data.repository.PetSettingsRepository
import com.asianmobile.privatebrower.data.repository.impl.DataStorePetSettingsRepository
import com.asianmobile.privatebrower.pet.pack.FilePetPackRepository
import com.asianmobile.privatebrower.pet.pack.PetPackArchiveExtractor
import com.asianmobile.privatebrower.pet.pack.PetPackManifestParser
import com.asianmobile.privatebrower.pet.pack.PetPackRepository
import com.asianmobile.privatebrower.pet.pack.PetPackValidator
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
    fun providePetPackRepository(
        repository: FilePetPackRepository
    ): PetPackRepository = repository

    @Provides
    @Singleton
    fun providePetSettingsRepository(
        repository: DataStorePetSettingsRepository
    ): PetSettingsRepository = repository
}
