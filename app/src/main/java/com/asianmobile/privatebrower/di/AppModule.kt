package com.asianmobile.privatebrower.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Add your singleton dependencies here
    // Example:
    // @Provides
    // @Singleton
    // fun provideSomeDependency(): SomeType = SomeImplementation()
}


