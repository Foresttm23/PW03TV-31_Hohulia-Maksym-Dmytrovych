package com.example.mse.ui.screens.task11

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.auth.DataStoreTokenStorage
import com.example.mse.data.remote.AuthNetwork
import com.example.mse.data.repository.AuthRepository
import com.example.mse.logic.LoginFormValidator
import com.example.mse.model.AuditEntryDto
import com.example.mse.model.AuthError
import com.example.mse.model.AuthResult
import com.example.mse.model.AuthSession
import com.example.mse.model.Permission
import com.example.mse.model.SecureTelemetryDto
import com.example.mse.model.UserDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TokenTtl(val seconds: Long, val title: String) {
    SEC_30(30, "30 с"),
    MIN_2(120, "2 хв"),
    MIN_15(900, "15 хв")
}

enum class SecureSection(val title: String, val permission: Permission) {
    TELEMETRY("Телеметрія", Permission.VIEW_TELEMETRY),
    GATES("Затвори", Permission.CONTROL_GATES),
    ADMIN("Адміністрування", Permission.MANAGE_USERS)
}

data class LoginFormState(
    val username: String = "",
    val password: String = "",
    val ttl: TokenTtl = TokenTtl.MIN_2,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

data class AuthUiState(
    val isRestoring: Boolean = true,
    val session: AuthSession? = null,
    val form: LoginFormState = LoginFormState(),
    val secondsLeft: Long = 0,
    val selectedSection: SecureSection = SecureSection.TELEMETRY,
    val isLoadingSecure: Boolean = false,
    val telemetry: SecureTelemetryDto? = null,
    val gateResponse: String? = null,
    val users: List<UserDto> = emptyList(),
    val audit: List<AuditEntryDto> = emptyList(),
    val reloginReason: String? = null
) {
    val availableSections: List<SecureSection>
        get() = session?.let { s -> SecureSection.entries.filter { s.role.has(it.permission) } }.orEmpty()

    fun can(permission: Permission): Boolean = session?.role?.has(permission) == true
}

class AuthViewModel(
    application: Application,
    private val repository: AuthRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, createRepository(application))

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    private var expiryTimer: Job? = null

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                _uiState.update { state ->
                    val sections = session?.let { s -> SecureSection.entries.filter { s.role.has(it.permission) } }.orEmpty()
                    state.copy(
                        session = session,
                        selectedSection = if (state.selectedSection in sections) state.selectedSection else SecureSection.TELEMETRY,
                        telemetry = if (session == null) null else state.telemetry,
                        users = if (session == null) emptyList() else state.users,
                        audit = if (session == null) emptyList() else state.audit
                    )
                }
                startExpiryTimer(session)
            }
        }
        viewModelScope.launch {
            val restored = repository.restoreSession()
            _uiState.update { it.copy(isRestoring = false) }
            if (restored is AuthResult.Failure) _uiState.update { it.copy(reloginReason = restored.error.userMessage) }
            if (restored is AuthResult.Success && restored.data != null) messages.send("Сесію відновлено з DataStore")
        }
    }

    fun onUsernameChange(v: String) = _uiState.update { it.copy(form = it.form.copy(username = v, usernameError = null, error = null)) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(form = it.form.copy(password = v, passwordError = null, error = null)) }
    fun onTtlChange(ttl: TokenTtl) = _uiState.update { it.copy(form = it.form.copy(ttl = ttl)) }
    fun fillTestAccount(username: String, password: String) =
        _uiState.update { it.copy(form = it.form.copy(username = username, password = password, usernameError = null, passwordError = null, error = null)) }

    fun login() {
        val form = _uiState.value.form
        val usernameError = LoginFormValidator.validateUsername(form.username)
        val passwordError = LoginFormValidator.validatePassword(form.password)
        if (usernameError != null || passwordError != null) {
            _uiState.update { it.copy(form = form.copy(usernameError = usernameError, passwordError = passwordError)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(form = form.copy(isSubmitting = true, error = null)) }
            when (val result = repository.login(form.username, form.password, form.ttl.seconds)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(form = LoginFormState(ttl = form.ttl), reloginReason = null) }
                    messages.send("Вхід виконано: ${result.data.claims.name} · ${result.data.role.title}")
                    loadTelemetry()
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(form = form.copy(isSubmitting = false, error = result.error.userMessage))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            messages.send("Ви вийшли із системи, токен видалено")
        }
    }

    fun dismissRelogin() = _uiState.update { it.copy(reloginReason = null) }
    fun selectSection(section: SecureSection) {
        _uiState.update { it.copy(selectedSection = section) }
        if (section == SecureSection.ADMIN && _uiState.value.users.isEmpty()) loadAdminData()
    }

    fun loadTelemetry() = secure({ repository.telemetry() }) { data -> _uiState.update { it.copy(telemetry = data) } }

    fun sendGateCommand(gateId: Int, opening: Int) =
        secure({ repository.gateCommand(gateId, opening) }) { r -> _uiState.update { it.copy(gateResponse = "${r.message} (${r.acceptedBy})") } }

    fun loadAdminData() {
        secure({ repository.users() }) { data -> _uiState.update { it.copy(users = data) } }
        secure({ repository.audit() }) { data -> _uiState.update { it.copy(audit = data) } }
    }

    fun probeAdminResource() = secure({ repository.users() }) { data -> _uiState.update { it.copy(users = data) } }

    fun probeWithoutToken() = secure({ repository.telemetryWithoutToken() }) {
        messages.send("Неочікувано: сервер віддав дані без токена")
    }

    fun forgeAdminRole() {
        viewModelScope.launch {
            val forged = repository.forgeAdminToken()
            if (forged != null) {
                messages.send("Роль у payload замінено на ADMIN — UI показав адмін-розділи. Перевіримо на сервері…")
                delay(1200)
                loadAdminData()
            }
        }
    }

    private fun <T> secure(call: suspend () -> AuthResult<T>, onSuccess: suspend (T) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSecure = true) }
            val result = call()
            _uiState.update { it.copy(isLoadingSecure = false) }
            when (result) {
                is AuthResult.Success -> onSuccess(result.data)
                is AuthResult.Failure -> handleAuthError(result.error)
            }
        }
    }

    private suspend fun handleAuthError(error: AuthError) {
        if (error.requiresRelogin && repository.session.value == null) {
            _uiState.update { it.copy(reloginReason = error.userMessage) }
        } else {
            messages.send(error.userMessage)
        }
    }

    private fun startExpiryTimer(session: AuthSession?) {
        expiryTimer?.cancel()
        if (session == null) return
        expiryTimer = viewModelScope.launch {
            while (isActive) {
                val left = session.secondsLeft()
                _uiState.update { it.copy(secondsLeft = left.coerceAtLeast(0)) }
                if (left <= 0) {
                    // Причина — до logout(): очищення сесії скасовує цю корутину, і код після logout() не виконався б.
                    _uiState.update { it.copy(reloginReason = AuthError.Expired.userMessage) }
                    repository.logout()
                    break
                }
                delay(1000)
            }
        }
    }

    companion object {
        private fun createRepository(application: Application): AuthRepository {
            val storage = DataStoreTokenStorage(application)
            return AuthRepository(AuthNetwork.createApi(storage), storage)
        }
    }
}
