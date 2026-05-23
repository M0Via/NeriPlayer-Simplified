package moe.ouom.neriplayer.listentogether

import android.net.Uri
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.local.media.LocalSongSupport
import moe.ouom.neriplayer.ui.viewmodel.playlist.SongItem
import moe.ouom.neriplayer.util.NPLogger
import androidx.core.net.toUri

fun SongItem.toListenTogetherTrackOrNull(includeLocal: Boolean = false): ListenTogetherTrack? {
    val channel = resolvedChannelId() ?: return null
    if (channel == ListenTogetherChannels.LOCAL && !includeLocal) {
        return null
    }

    val audio = resolvedAudioId() ?: return null
    val subAudio = resolvedSubAudioId()
    val playlistContext = resolvedPlaylistContextId()
    return ListenTogetherTrack(
        stableKey = buildStableTrackKey(channel, audio, subAudio, playlistContext),
        channelId = channel,
        audioId = audio,
        subAudioId = subAudio,
        playlistContextId = playlistContext,
        mediaUri = mediaUri,
        streamUrl = streamUrl,
        name = customName ?: name,
        artist = customArtist ?: artist,
        album = album,
        durationMs = durationMs,
        coverUrl = customCoverUrl ?: coverUrl
    )
}

fun ListenTogetherTrack.toSongItem(): SongItem {
    val trustedStreamUrl = trustedListenTogetherStreamUrl(channelId, streamUrl)
    return when (channelId) {
        ListenTogetherChannels.BILIBILI -> {
            val songId = audioId.toLongOrNull() ?: stableKey.hashCode().toLong()
            val albumTag = subAudioId?.takeIf { it.isNotBlank() }
                ?.let { "${PlayerManager.BILI_SOURCE_TAG}|$it" }
                ?: PlayerManager.BILI_SOURCE_TAG
            SongItem(
                id = songId,
                name = name,
                artist = artist,
                album = albumTag,
                albumId = 0L,
                durationMs = durationMs,
                coverUrl = coverUrl,
                channelId = channelId,
                audioId = audioId,
                subAudioId = subAudioId,
                playlistContextId = playlistContextId,
                streamUrl = trustedStreamUrl
            )
        }

        ListenTogetherChannels.LOCAL -> {
            val songId = audioId.toLongOrNull() ?: stableKey.hashCode().toLong()
            SongItem(
                id = songId,
                name = name,
                artist = artist,
                album = album ?: LocalSongSupport.LOCAL_ALBUM_IDENTITY,
                albumId = 0L,
                durationMs = durationMs,
                coverUrl = coverUrl,
                mediaUri = mediaUri,
                originalName = name,
                originalArtist = artist,
                originalCoverUrl = coverUrl,
                localFilePath = mediaUri,
                channelId = channelId,
                audioId = audioId,
                subAudioId = subAudioId,
                playlistContextId = playlistContextId,
                streamUrl = trustedStreamUrl
            )
        }

        else -> {
            val songId = audioId.toLongOrNull() ?: stableKey.hashCode().toLong()
            SongItem(
                id = songId,
                name = name,
                artist = artist,
                album = album.orEmpty(),
                albumId = 0L,
                durationMs = durationMs,
                coverUrl = coverUrl,
                channelId = ListenTogetherChannels.NETEASE,
                audioId = audioId,
                subAudioId = subAudioId,
                playlistContextId = playlistContextId,
                streamUrl = trustedStreamUrl
            )
        }
    }
}

fun ListenTogetherTrack.withStreamUrl(streamUrl: String?): ListenTogetherTrack {
    val normalizedStreamUrl = trustedListenTogetherStreamUrl(channelId, streamUrl)
    if (normalizedStreamUrl == this.streamUrl) return this
    return copy(streamUrl = normalizedStreamUrl)
}

private fun trustedListenTogetherStreamUrl(
    channelId: String,
    streamUrl: String?
): String? {
    val candidate = streamUrl?.trim().orEmpty()
    if (candidate.isBlank()) return null
    val uri = runCatching { candidate.toUri() }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase().orEmpty()
    if (scheme != "https" && scheme != "http") return null
    val host = uri.host?.lowercase().orEmpty()
    if (host.isBlank()) return null
    val trusted = when (channelId) {
        ListenTogetherChannels.NETEASE -> host == "music.126.net" || host.endsWith(".music.126.net")
        ListenTogetherChannels.BILIBILI -> {
            host == "bilivideo.com" ||
                host.endsWith(".bilivideo.com") ||
                host == "bilivideo.cn" ||
                host.endsWith(".bilivideo.cn") ||
                host == "hdslb.com" ||
                host.endsWith(".hdslb.com")
        }
        else -> false
    }
    if (!trusted) {
        NPLogger.w(
            "NERI-ListenTogether",
            "Blocked non-whitelisted streamUrl for listen together: channelId=$channelId, host=$host"
        )
        return null
    }
    return candidate
}

fun buildStableTrackKey(
    channelId: String,
    audioId: String,
    subAudioId: String? = null,
    playlistContextId: String? = null
): String {
    return when (channelId) {
        ListenTogetherChannels.BILIBILI -> {
            listOf(channelId, audioId, subAudioId).filterNot { it.isNullOrBlank() }.joinToString(":")
        }

        else -> "$channelId:$audioId"
    }
}

fun SongItem.resolvedChannelId(): String? {
    channelId?.takeIf { it.isNotBlank() }?.let { return it }
    return when {
        LocalSongSupport.isLocalSong(this, null) -> ListenTogetherChannels.LOCAL
        album.startsWith(PlayerManager.BILI_SOURCE_TAG) -> ListenTogetherChannels.BILIBILI
        else -> ListenTogetherChannels.NETEASE
    }
}

fun SongItem.resolvedAudioId(): String? {
    audioId?.takeIf { it.isNotBlank() }?.let { return it }
    return when (resolvedChannelId()) {
        else -> id.toString()
    }
}

fun SongItem.resolvedSubAudioId(): String? {
    subAudioId?.takeIf { it.isNotBlank() }?.let { return it }
    if (resolvedChannelId() != ListenTogetherChannels.BILIBILI) {
        return null
    }
    return album.substringAfter('|', "").takeIf { it.isNotBlank() }
}

fun SongItem.resolvedPlaylistContextId(): String? {
    return playlistContextId?.takeIf { it.isNotBlank() }
        ?: mediaUri?.let(Uri::parse)?.getQueryParameter("playlistId")?.takeIf { it.isNotBlank() }
}
