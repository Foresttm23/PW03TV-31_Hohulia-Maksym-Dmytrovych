package com.example.mse.ui.screens.task11

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.data.remote.mock.MockAuthServer
import com.example.mse.model.AuthSession
import com.example.mse.model.Permission
import com.example.mse.model.UserRole
import com.example.mse.ui.components.LabeledValue
import com.example.mse.ui.components.SectionTitle
import com.example.mse.ui.components.StateColors
import com.example.mse.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecureAccessScreen(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messageFlow.collect { snackbar.showSnackbar(it) } }

    // Системні відступи вже враховує зовнішній Scaffold у MainActivity.
    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isRestoring -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.session == null -> LoginForm(state.form, viewModel)
                else -> SecureHome(state, state.session!!, viewModel)
            }
        }
    }

    state.reloginReason?.let { reason ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRelogin,
            icon = { Icon(Icons.Default.LockClock, null) },
            title = { Text("Потрібен повторний вхід") },
            text = { Text(reason) },
            confirmButton = { TextButton(onClick = viewModel::dismissRelogin) { Text("До форми входу") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginForm(form: LoginFormState, viewModel: AuthViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(Modifier.widthIn(max = 520.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Вхід до SCADA ГЕС", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Mocked API видає підписаний токен із роллю та терміном дії", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = form.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Логін") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    isError = form.usernameError != null,
                    supportingText = form.usernameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Пароль") },
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    isError = form.passwordError != null,
                    supportingText = form.passwordError?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Термін дії токена", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TokenTtl.entries.forEach { ttl ->
                        FilterChip(form.ttl == ttl, { viewModel.onTtlChange(ttl) }, { Text(ttl.title) })
                    }
                }
                form.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                Button(onClick = viewModel::login, enabled = !form.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    if (form.isSubmitting) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                    else Text("Увійти")
                }
            }
        }
        Spacer(Modifier.padding(6.dp))
        Card(Modifier.widthIn(max = 520.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionTitle("Тестові користувачі (mocked)", "Натисніть, щоб підставити дані")
                MockAuthServer.USERS.forEach { user ->
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.fillTestAccount(user.username, user.password) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(user.role.title, roleColor(user.role))
                        Spacer(Modifier.width(8.dp))
                        Text("${user.username} / ${user.password}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecureHome(state: AuthUiState, session: AuthSession, viewModel: AuthViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(0.4f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionCard(state, session, viewModel)
                    SecurityTestsCard(state, viewModel)
                }
                Column(Modifier.weight(0.6f).verticalScroll(rememberScrollState())) { SectionsPanel(state, viewModel) }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionCard(state, session, viewModel)
                SectionsPanel(state, viewModel)
                SecurityTestsCard(state, viewModel)
            }
        }
    }
}

@Composable
private fun SessionCard(state: AuthUiState, session: AuthSession, viewModel: AuthViewModel) {
    val total = (session.claims.exp - session.claims.iat).coerceAtLeast(1)
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, null, tint = roleColor(session.role))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.claims.name, fontWeight = FontWeight.Bold)
                    Text("login: ${session.claims.sub}", style = MaterialTheme.typography.labelSmall)
                }
                StatusPill(session.role.title, roleColor(session.role))
            }
            val left = state.secondsLeft
            Text(
                "Токен дійсний ще: %d:%02d".format(left / 60, left % 60),
                color = if (left < 20) StateColors.Danger else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { (left.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (left < 20) StateColors.Danger else StateColors.Ok
            )
            Text(
                "payload: role=${session.claims.role}, iat=${time(session.claims.iat)}, exp=${time(session.claims.exp)}, jti=${session.claims.jti}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            Text("token: ${session.token.take(36)}…${session.token.takeLast(12)}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            Text(
                "Права: " + Permission.entries.joinToString { (if (session.role.has(it)) "✓ " else "✗ ") + it.name },
                style = MaterialTheme.typography.labelSmall
            )
            OutlinedButton(onClick = viewModel::logout) {
                Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(6.dp)); Text("Вийти")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionsPanel(state: AuthUiState, viewModel: AuthViewModel) {
    val sections = state.availableSections
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryTabRow(selectedTabIndex = sections.indexOf(state.selectedSection).coerceAtLeast(0)) {
            sections.forEach { section ->
                Tab(selected = section == state.selectedSection, onClick = { viewModel.selectSection(section) }, text = { Text(section.title) })
            }
        }
        if (state.isLoadingSecure) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (state.selectedSection) {
            SecureSection.TELEMETRY -> TelemetrySection(state, viewModel)
            SecureSection.GATES -> if (state.can(Permission.CONTROL_GATES)) GatesSection(state, viewModel)
            SecureSection.ADMIN -> if (state.can(Permission.MANAGE_USERS)) AdminSection(state, viewModel)
        }
    }
}

@Composable
private fun TelemetrySection(state: AuthUiState, viewModel: AuthViewModel) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("GET /api/secure/telemetry", "Доступно всім ролям", Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::loadTelemetry) { Icon(Icons.Default.Refresh, null) }
            }
            val t = state.telemetry
            if (t == null) {
                Text("Дані ще не завантажено")
            } else {
                Text(t.station, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledValue("ВБ", "%.2f м".format(t.upstreamLevelM))
                    LabeledValue("НБ", "%.2f м".format(t.downstreamLevelM))
                    LabeledValue("Приплив", "%.0f м³/с".format(t.inflowM3s))
                    LabeledValue("P", "%.1f МВт".format(t.totalPowerMw))
                }
                t.units.forEach { u ->
                    Row {
                        Text(u.unit, Modifier.weight(1f))
                        Text("%.1f МВт".format(u.powerMw), Modifier.weight(1f))
                        StatusPill(u.state, if (u.powerMw > 0) StateColors.Ok else StateColors.Neutral)
                    }
                }
                Text("Відповідь сформовано для: ${t.servedTo}", style = MaterialTheme.typography.labelSmall)
            }
            if (!state.can(Permission.CONTROL_GATES)) {
                Text("Режим лише перегляду: керування затворами недоступне для вашої ролі", color = StateColors.Warning, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GatesSection(state: AuthUiState, viewModel: AuthViewModel) {
    var gate by remember { mutableIntStateOf(1) }
    var opening by remember { mutableFloatStateOf(30f) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("POST /api/secure/gates/command", "Права: CONTROL_GATES (інженер, адміністратор)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..6).forEach { id -> FilterChip(gate == id, { gate = id }, { Text("№$id") }) }
            }
            Text("Відкриття затвора: ${opening.toInt()}%")
            Slider(value = opening, onValueChange = { opening = it }, valueRange = 0f..100f, steps = 9)
            Button(onClick = { viewModel.sendGateCommand(gate, opening.toInt()) }) {
                Icon(Icons.Default.Send, null); Spacer(Modifier.width(6.dp)); Text("Надіслати команду")
            }
            state.gateResponse?.let { Text(it, color = StateColors.Ok, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun AdminSection(state: AuthUiState, viewModel: AuthViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, null)
                Spacer(Modifier.width(8.dp))
                SectionTitle("Адміністрування", "GET /admin/users, /admin/audit — лише ADMIN", Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::loadAdminData) { Icon(Icons.Default.Refresh, null) }
            }
            Text("Користувачі", fontWeight = FontWeight.Bold)
            state.users.forEach { u ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${u.fullName} (${u.username}), зміна: ${u.shift}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    UserRole.fromClaim(u.role)?.let { StatusPill(it.title, roleColor(it)) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Серверний журнал аудиту", fontWeight = FontWeight.Bold)
            state.audit.take(12).forEach { a ->
                Text(
                    "${clock(a.time)}  ${a.status}  ${a.user.padEnd(9)} ${a.endpoint}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (a.status >= 400) StateColors.Danger else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun SecurityTestsCard(state: AuthUiState, viewModel: AuthViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GppBad, null, tint = StateColors.Danger)
                Spacer(Modifier.width(8.dp))
                SectionTitle("Перевірка захисту API", "Сервер не довіряє клієнтському UI")
            }
            OutlinedButton(onClick = viewModel::probeWithoutToken, modifier = Modifier.fillMaxWidth()) { Text("Запит без токена → 401") }
            if (!state.can(Permission.MANAGE_USERS)) {
                OutlinedButton(onClick = viewModel::probeAdminResource, modifier = Modifier.fillMaxWidth()) { Text("Звернутися до /admin/users → 403") }
            }
            OutlinedButton(onClick = viewModel::forgeAdminRole, modifier = Modifier.fillMaxWidth()) { Text("Підробити роль ADMIN у токені → 401") }
        }
    }
}

private fun roleColor(role: UserRole) = when (role) {
    UserRole.ADMIN -> StateColors.Danger
    UserRole.ENGINEER -> StateColors.Info
    UserRole.OBSERVER -> StateColors.Neutral
}

private fun time(epochSeconds: Long) = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochSeconds * 1000))
private fun clock(epochMs: Long) = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
