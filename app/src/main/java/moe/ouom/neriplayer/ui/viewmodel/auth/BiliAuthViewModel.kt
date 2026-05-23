package moe.ouom.neriplayer.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import moe.ouom.neriplayer.data.auth.common.SavedCookieAuthState

class BiliAuthViewModel : ViewModel() {
    val authFlow: MutableStateFlow<Unit> = MutableStateFlow(Unit)
    val uiState: MutableStateFlow<BiliAuthUiState> = MutableStateFlow(BiliAuthUiState())
    val events: Flow<BiliAuthEvent> = emptyFlow()
    fun refreshAuthHealth() {}
    val hasSavedCookies: Boolean = false
    fun clearCookies() {}

    data class BiliAuthUiState(val health: BiliAuthHealth = BiliAuthHealth(), val hasSavedCookies: Boolean = false)
    data class BiliAuthHealth(
        val state: SavedCookieAuthState = SavedCookieAuthState.Valid,
        val savedAt: Long = 0L
    )
    fun importCookiesFromMap(map: Map<String, String>) {}
    fun importCookiesFromRaw(raw: String) {}
    fun parseJsonToMap(json: String): Map<String, String> = emptyMap()
}

sealed class BiliAuthEvent {
    data class ShowSnack(val message: String) : BiliAuthEvent()
    data class ShowCookies(val cookies: Map<String, String>) : BiliAuthEvent()
    data object LoginSuccess : BiliAuthEvent()
}
