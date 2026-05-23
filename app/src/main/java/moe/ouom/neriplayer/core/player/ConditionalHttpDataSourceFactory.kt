package moe.ouom.neriplayer.core.player

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
 * File: moe.ouom.neriplayer.core.player/ConditionalHttpDataSourceFactory
 * Created: 2025/8/15
 */


import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.data.auth.bili.BiliCookieRepository
import moe.ouom.neriplayer.data.platform.bili.isBiliStreamHost
import moe.ouom.neriplayer.data.platform.bili.isBiliStreamUrl
import moe.ouom.neriplayer.util.NPLogger

/**
 * 自定义的 HttpDataSource.Factory：
 * - 按 host/路径动态注入请求头（B 站拉流）
 * - 监听鉴权仓库变化，实时刷新注入的 Cookie 字符串
 */
@UnstableApi
class ConditionalHttpDataSourceFactory(
    private val baseFactory: HttpDataSource.Factory,
    cookieRepo: BiliCookieRepository
) : HttpDataSource.Factory {

    companion object {
        private const val BILI_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
    }

    @Volatile
    private var latestCookieHeader: String = ""
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        scope.launch {
            cookieRepo.cookieFlow.collect { cookies ->
                latestCookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            }
        }
    }

    override fun createDataSource(): HttpDataSource {
        val upstream = baseFactory.createDataSource()
        return InjectingHttpDataSource(
            upstream = upstream,
            shouldInjectHeaders = { uri -> shouldInjectBiliHeaders(uri) },
            buildHeaders = { original -> buildBiliHeaders(original) }
        )
    }

    override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory {
        baseFactory.setDefaultRequestProperties(defaultRequestProperties)
        return this
    }

    fun close() {
        scope.cancel()
    }

    /**
     * Wraps an HttpDataSource and injects Bilibili request headers.
     */
    private class InjectingHttpDataSource(
        private val upstream: HttpDataSource,
        private val shouldInjectHeaders: (Uri) -> Boolean,
        private val buildHeaders: (Map<String, String>) -> Map<String, String>
    ) : HttpDataSource {

        override fun open(dataSpec: DataSpec): Long {
            val modified = if (shouldInjectHeaders(dataSpec.uri)) {
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(buildHeaders(dataSpec.httpRequestHeaders))
                    .build()
            } else {
                dataSpec
            }
            return upstream.open(modified)
        }

        override fun read(b: ByteArray, offset: Int, length: Int): Int = upstream.read(b, offset, length)
        override fun getUri(): Uri? = upstream.uri
        override fun getResponseHeaders(): Map<String, MutableList<String>> = upstream.responseHeaders
        override fun close() = upstream.close()
        override fun getResponseCode(): Int = upstream.responseCode
        override fun addTransferListener(listener: TransferListener) = upstream.addTransferListener(listener)
        override fun setRequestProperty(name: String, value: String) = upstream.setRequestProperty(name, value)
        override fun clearRequestProperty(name: String) = upstream.clearRequestProperty(name)
        override fun clearAllRequestProperties() = upstream.clearAllRequestProperties()
    }

    /**
     * 是否需要为该 URI 注入 B 站拉流所需的请求头
     */
    private fun shouldInjectBiliHeaders(uri: Uri): Boolean {
        val host = uri.host.orEmpty()
        return isBiliStreamHost(host) || isBiliStreamUrl(uri.toString())
    }

    /**
     * 基于原始请求头构建 B 站拉流所需的头部（Referer/UA/Cookie）
     */
    private fun buildBiliHeaders(original: Map<String, String>): Map<String, String> {
        val newHeaders = LinkedHashMap<String, String>(original)
        newHeaders["Referer"] = "https://www.bilibili.com"
        newHeaders["User-Agent"] = BILI_USER_AGENT
        if (latestCookieHeader.isNotBlank()) newHeaders["Cookie"] = latestCookieHeader
        return newHeaders
    }
}
