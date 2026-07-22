package com.asianmobile.privatebrower.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.asianmobile.privatebrower.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies.
 * Configures the shared OkHttp client used by browser downloads.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // Retries a fresh request when a stale pooled connection dies mid-download.
            .retryOnConnectionFailure(true)
            // Retries an idempotent GET on transient network/5xx failures (flaky CDNs).
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            // Connect/read/write bound each socket op; the read timeout resets on every byte,
            // so it only trips on a genuine stall — not on a long-but-progressing download.
            // callTimeout stays 0 (unbounded) so large videos aren't cut off overall.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build()
    }
}

