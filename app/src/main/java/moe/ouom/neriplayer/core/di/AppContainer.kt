package moe.ouom.neriplayer.core.di

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.core.di/AppContainer
 * Created: 2025/8/19
 */

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.ouom.neriplayer.core.api.bili.BiliClient
import moe.ouom.neriplayer.core.api.bili.BiliClientAudioDataSource
import moe.ouom.neriplayer.core.api.bili.BiliPlaybackRepository
import moe.ouom.neriplayer.core.api.netease.NeteaseClient
import moe.ouom.neriplayer.core.api.search.CloudMusicSearchApi
import moe.ouom.neriplayer.core.api.search.QQMusicSearchApi
import moe.ouom.neriplayer.core.download.ManagedDownloadStorage
import moe.ouom.neriplayer.data.ListenTogetherPreferences
import moe.ouom.neriplayer.data.auth.bili.BiliCookieRepository
import moe.ouom.neriplayer.data.auth.netease.NeteaseCookieRepository
import moe.ouom.neriplayer.data.history.PlayHistoryRepository
import moe.ouom.neriplayer.data.playlist.usage.PlaylistUsageRepository
import moe.ouom.neriplayer.data.stats.PlaybackStatsRepository
import moe.ouom.neriplayer.listentogether.ListenTogetherApi
import moe.ouom.neriplayer.listentogether.ListenTogetherSessionManager
import moe.ouom.neriplayer.listentogether.ListenTogetherWebSocketClient
import moe.ouom.neriplayer.data.settings.dataStore
import moe.ouom.neriplayer.data.settings.persistBootstrapSettingsSnapshot
import moe.ouom.neriplayer.data.settings.persistPlaybackPreferenceSnapshot
import moe.ouom.neriplayer.data.settings.readBootstrapSettingsSnapshotSync
import moe.ouom.neriplayer.data.settings.SettingsRepository
import moe.ouom.neriplayer.data.settings.toBootstrapSettingsSnapshot
import moe.ouom.neriplayer.data.settings.toPlaybackPreferenceSnapshot
import moe.ouom.neriplayer.util.DynamicProxySelector
import okhttp3.OkHttpClient
import kotlin.LazyThreadSafetyMode

internal fun resolveInitialBypassProxy(
    currentValue: Boolean,
    loadPersistedValue: () -> Boolean
): Boolean = runCatching(loadPersistedValue).getOrDefault(currentValue)

internal data class InitialManagedDownloadSettings(
    val directoryUri: String? = null,
    val directoryLabel: String? = null,
    val fileNameTemplate: String? = null
)

internal fun resolveInitialManagedDownloadSettings(
    currentDirectoryUri: String? = null,
    currentDirectoryLabel: String? = null,
    currentFileNameTemplate: String? = null,
    loadDirectoryUri: () -> String?,
    loadDirectoryLabel: () -> String?,
    loadFileNameTemplate: () -> String?
): InitialManagedDownloadSettings {
    return InitialManagedDownloadSettings(
        directoryUri = runCatching(loadDirectoryUri).getOrDefault(currentDirectoryUri),
        directoryLabel = runCatching(loadDirectoryLabel).getOrDefault(currentDirectoryLabel),
        fileNameTemplate = runCatching(loadFileNameTemplate).getOrDefault(currentFileNameTemplate)
    ).let { resolved ->
        InitialManagedDownloadSettings(
            directoryUri = resolved.directoryUri?.takeIf(String::isNotBlank),
            directoryLabel = resolved.directoryLabel?.takeIf(String::isNotBlank),
            fileNameTemplate = resolved.fileNameTemplate?.takeIf(String::isNotBlank)
        )
    }
}

/**
 * 全局依赖容器，使用 Service Locator 模式管理 App 的单例
 */
object AppContainer {
    private lateinit var application: Application
    val applicationContext: Application
        get() = application

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 基础 Repo
    val settingsRepo by lazy { SettingsRepository(application) }
    val listenTogetherPreferences by lazy { ListenTogetherPreferences(application) }
    val neteaseCookieRepo by lazy { NeteaseCookieRepository(application) }
    val biliCookieRepo by lazy { BiliCookieRepository(application) }

    // YouTube 桩（已禁用，仅用于编译兼容）
    val youtubeAuthRepo by lazy { moe.ouom.neriplayer.data.auth.youtube.YouTubeAuthRepository(application) }
    val youtubeMusicClient by lazy { moe.ouom.neriplayer.core.api.youtube.YouTubeMusicClient() }
    val youtubeMusicPlaybackRepository by lazy { moe.ouom.neriplayer.core.api.youtube.YouTubeMusicPlaybackRepository() }

    val playHistoryRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlayHistoryRepository.getInstance(application)
    }
    val playbackStatsRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlaybackStatsRepository.getInstance(application)
    }
    val playlistUsageRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlaylistUsageRepository(application)
    }

    // 共享 OkHttpClient：受 DynamicProxySelector 管理
    val sharedOkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(DynamicProxySelector)
            .build()
    }

    // 网络客户端
    val neteaseClient by lazy {
        NeteaseClient().also { client ->
            val cookies = neteaseCookieRepo.cookieFlow.value.toMutableMap()
            cookies.putIfAbsent("os", "pc")
            client.setPersistedCookies(cookies)
        }
    }

    val biliClient by lazy { BiliClient(biliCookieRepo, client = sharedOkHttpClient) }

    // 功能 Repo 和 API
    val biliPlaybackRepository by lazy {
        val dataSource = BiliClientAudioDataSource(biliClient)
        BiliPlaybackRepository(dataSource, settingsRepo)
    }

    val cloudMusicSearchApi by lazy { CloudMusicSearchApi(neteaseClient) }
    val qqMusicSearchApi by lazy { QQMusicSearchApi() }
    val lrcLibClient by lazy { moe.ouom.neriplayer.core.api.lyrics.LrcLibClient(sharedOkHttpClient) }
    val listenTogetherApi by lazy { ListenTogetherApi(sharedOkHttpClient) }
    val listenTogetherWebSocketClient by lazy { ListenTogetherWebSocketClient(sharedOkHttpClient) }
    val listenTogetherSessionManager by lazy {
        ListenTogetherSessionManager(
            api = listenTogetherApi,
            webSocketClient = listenTogetherWebSocketClient
        )
    }

    fun launchBackgroundIo(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    fun initialize(app: Application) {
        this.application = app
        primeProxySetting()
        startCookieObserver()
        startSettingsObserver()
    }

    private fun primeProxySetting() {
        val initialBootstrapSettings = readBootstrapSettingsSnapshotSync(application)
        DynamicProxySelector.bypassProxy = initialBootstrapSettings.bypassProxy
        ManagedDownloadStorage.primeSettings(
            directoryUri = initialBootstrapSettings.downloadDirectoryUri,
            directoryLabel = initialBootstrapSettings.downloadDirectoryLabel,
            fileNameTemplate = initialBootstrapSettings.downloadFileNameTemplate
        )
    }

    private fun startCookieObserver() {
        neteaseCookieRepo.cookieFlow
            .onEach { cookies ->
                val mutableCookies = cookies.toMutableMap()
                mutableCookies.putIfAbsent("os", "pc")

                neteaseClient.setPersistedCookies(mutableCookies)
            }
            .launchIn(scope)
    }

    private fun startSettingsObserver() {
        application.dataStore.data
            .onEach { preferences ->
                persistBootstrapSettingsSnapshot(
                    application,
                    preferences.toBootstrapSettingsSnapshot()
                )
                persistPlaybackPreferenceSnapshot(
                    application,
                    preferences.toPlaybackPreferenceSnapshot()
                )
            }
            .launchIn(scope)

        settingsRepo.bypassProxyFlow
            .onEach { enabled ->
                DynamicProxySelector.bypassProxy = enabled
                sharedOkHttpClient.connectionPool.evictAll()
                neteaseClient.evictConnections()
            }
            .launchIn(scope)

        settingsRepo.downloadDirectoryUriFlow
            .onEach { uri ->
                ManagedDownloadStorage.updateCustomDirectoryUri(uri)
            }
            .launchIn(scope)

        settingsRepo.downloadDirectoryLabelFlow
            .onEach { label ->
                ManagedDownloadStorage.updateCustomDirectoryLabel(label)
            }
            .launchIn(scope)

        settingsRepo.downloadFileNameTemplateFlow
            .onEach { template ->
                ManagedDownloadStorage.updateDownloadFileNameTemplate(template)
            }
            .launchIn(scope)
    }
}
