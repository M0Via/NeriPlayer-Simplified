package moe.ouom.neriplayer.data.platform.youtube

import moe.ouom.neriplayer.data.auth.youtube.YouTubeAuthBundle
import moe.ouom.neriplayer.ui.viewmodel.playlist.SongItem

/** YouTube support has been removed from this build. All stubs return false/null/empty. */

const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"

fun isYouTubeMusicSong(song: SongItem): Boolean = false
fun extractYouTubeMusicVideoId(mediaUri: String?): String? = null
fun stableYouTubeMusicId(mediaUri: String?): String? = null
fun buildYouTubeMusicMediaUri(videoId: String): String = ""

fun YouTubeAuthBundle.buildYouTubeStreamRequestHeaders(
    refererOrigin: String = "",
    streamUrl: String = "",
    original: Map<String, String> = emptyMap()
): Map<String, String> = emptyMap()
