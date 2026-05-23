package moe.ouom.neriplayer.core.player

import okhttp3.Request

data class ChunkDownloadValue(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val isEndOfStream: Boolean
)

data class ChunkLengthFallbackResult(
    val value: ChunkDownloadValue,
    val chunkLength: Long
)

/** Stub: YouTube has been removed from this build. All methods are no-ops. */
object YouTubeGoogleVideoRangeSupport {
    fun shouldUseChunkedRange(request: Request): Boolean = false
    fun hasExplicitRangeHeader(headers: Map<String, String>): Boolean = true
    fun resolveQueryContentLength(url: String): Long? = null
    fun shouldForceExplicitFullRange(url: String): Boolean = false
    fun buildFullRangeHeader(totalContentLength: Long): String = ""
    fun executeChunkLengthFallback(
        requestLength: Long,
        preferredChunkSize: Long,
        block: (Long) -> Unit = {}
    ): ChunkLengthFallbackResult = ChunkLengthFallbackResult(
        value = ChunkDownloadValue(
            downloadedBytes = 0L,
            totalBytes = 0L,
            isEndOfStream = true
        ),
        chunkLength = 0L
    )
    fun candidateChunkLengths(
        requestLength: Long,
        preferredChunkSize: Long
    ): List<Long> = listOf(preferredChunkSize)
    fun buildChunkedRequest(
        request: Request,
        start: Long,
        length: Long
    ): Request = request
    fun resolveTotalContentLength(uri: Any, headers: Map<String, List<String>>): Long? = null
    fun resolveChunkResponseLength(
        requestedLength: Long,
        headers: Map<String, List<String>>,
        delegateOpenLength: Long
    ): Long = 0L
    fun buildChunkedResponse(response: Any): Any = response
}
