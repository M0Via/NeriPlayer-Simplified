package moe.ouom.neriplayer.core.api.youtube

import java.io.IOException

/**
 * Stub: YouTube support has been removed from this build.
 * These types exist only to allow the remaining code to compile.
 * At runtime, isYouTubeMusicSong() returns false, so YouTube code paths never execute.
 */

enum class YouTubePlayableStreamType {
    DIRECT, HLS
}

class ChunkRequestIOException(val responseCode: Int, message: String? = null) : IOException(message)

/** YouTube Music client stub - never actually used */
class YouTubeMusicClient {
    fun getLyrics(videoId: String): YouTubeMusicLyricsResult? = null
    fun getLibraryPlaylists(): List<Nothing> = emptyList()
    fun getPlaylistDetail(browseId: String): Nothing = throw UnsupportedOperationException()
    fun clearBootstrapCache() {}
    fun getLibraryPlaylist(@Suppress("UNUSED_PARAMETER") browseId: String): Nothing = 
        throw UnsupportedOperationException()
}

data class YouTubeMusicLyricsResult(val lyrics: String)

/** YouTube Music playback repository stub - never actually used */
class YouTubeMusicPlaybackRepository {
    suspend fun getBestPlayableAudio(
        videoId: String,
        forceRefresh: Boolean = false,
        requireDirect: Boolean = false,
        preferM4a: Boolean = false
    ): YouTubePlayableAudio? = null
    
    suspend fun warmBootstrapAsync() {}
    fun clearAuthBoundCaches(@Suppress("UNUSED_PARAMETER") includeVideo: Boolean) {}
}

data class YouTubePlayableAudio(
    val url: String = "",
    val mimeType: String? = null,
    val contentLength: Long? = null,
    val durationMs: Long? = null,
    val bitrateKbps: Int? = null,
    val streamType: YouTubePlayableStreamType = YouTubePlayableStreamType.DIRECT
)
