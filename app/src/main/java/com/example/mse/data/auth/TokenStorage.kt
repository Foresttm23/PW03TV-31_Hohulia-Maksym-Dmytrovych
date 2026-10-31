package com.example.mse.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface TokenStorage {
    fun currentToken(): String?
    suspend fun restore(): String?
    suspend fun save(token: String)
    suspend fun clear()
}

private val Context.authDataStore by preferencesDataStore(name = "hydro_auth")

class DataStoreTokenStorage(private val context: Context) : TokenStorage {
    private val tokenKey = stringPreferencesKey("access_token")

    // Синхронна копія для AuthInterceptor: OkHttp-потік не може викликати suspend-функції DataStore.
    @Volatile
    private var cached: String? = null

    override fun currentToken(): String? = cached

    override suspend fun restore(): String? =
        context.authDataStore.data.map { it[tokenKey] }.first().also { cached = it }

    override suspend fun save(token: String) {
        cached = token
        context.authDataStore.edit { it[tokenKey] = token }
    }

    override suspend fun clear() {
        cached = null
        context.authDataStore.edit { it.remove(tokenKey) }
    }
}

class InMemoryTokenStorage(initial: String? = null) : TokenStorage {
    @Volatile private var token: String? = initial
    override fun currentToken() = token
    override suspend fun restore() = token
    override suspend fun save(token: String) { this.token = token }
    override suspend fun clear() { token = null }
}
