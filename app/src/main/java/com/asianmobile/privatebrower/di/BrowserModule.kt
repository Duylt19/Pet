package com.asianmobile.privatebrower.di

import android.content.Context
import com.asianmobile.privatebrower.data.browser.BrowserEngine
import com.asianmobile.privatebrower.data.browser.MediaCaptureServer
import com.asianmobile.privatebrower.data.browser.TabManager
import com.asianmobile.privatebrower.data.repository.TabRepository
import com.asianmobile.privatebrower.data.repository.HistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {

    @Provides
    @Singleton
    fun provideBrowserEngine(
        @ApplicationContext context: Context,
        mediaCaptureServer: MediaCaptureServer
    ): BrowserEngine = BrowserEngine(context, mediaCaptureServer)

    @Provides
    @Singleton
    fun provideTabManager(
        tabRepository: TabRepository,
        historyRepository: HistoryRepository,
        browserEngine: BrowserEngine,
        videoSniffer: com.asianmobile.privatebrower.data.browser.VideoSniffer,
        downloadRepository: com.asianmobile.privatebrower.data.repository.DownloadRepository,
        mediaCaptureServer: MediaCaptureServer,
        @ApplicationContext context: Context
    ): TabManager = TabManager(
        tabRepository,
        historyRepository,
        browserEngine,
        videoSniffer,
        downloadRepository,
        mediaCaptureServer,
        context
    )
}
