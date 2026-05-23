package moe.ouom.neriplayer.data.auth.youtube

/** Stub: YouTube auth is not used in this build. */

class YouTubeAuthBundle {
    fun normalized(): YouTubeAuthBundle = this
    fun hasLoginCookies(): Boolean = false
    fun hasData(): Boolean = false
    val origin: String = ""
}

class YouTubeAuthRepository(private val context: Any? = null) {
    fun getAuthOnce(): YouTubeAuthBundle = YouTubeAuthBundle()
    fun saveAuth(@Suppress("UNUSED_PARAMETER") bundle: YouTubeAuthBundle) {}
    fun clear() {}
    val authFlow: kotlinx.coroutines.flow.StateFlow<YouTubeAuthBundle> = 
        kotlinx.coroutines.flow.MutableStateFlow(YouTubeAuthBundle())
}

class YouTubeAuthAutoRefreshManager(
    context: Any? = null,
    authProvider: (() -> YouTubeAuthBundle)? = null,
    authHealthProvider: (() -> Any)? = null,
    authUpdater: ((YouTubeAuthBundle) -> Unit)? = null
)
