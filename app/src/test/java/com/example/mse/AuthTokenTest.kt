package com.example.mse

import com.example.mse.data.auth.InMemoryTokenStorage
import com.example.mse.data.remote.AuthNetwork
import com.example.mse.data.remote.mock.MockAuthServer
import com.example.mse.data.repository.AuthRepository
import com.example.mse.model.AuthError
import com.example.mse.model.AuthResult
import com.example.mse.model.UserRole
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTokenTest {
    private class Env {
        val storage = InMemoryTokenStorage()
        val api = AuthNetwork.createApi(storage, Gson(), MockAuthServer(Gson(), latencyMs = 0))
        val repo = AuthRepository(api, storage)
    }

    @Test
    fun observerGetsForbiddenOnAdminResource() = runBlocking {
        val env = Env()
        assertTrue(env.repo.login("observer", "Observer#2026", 60) is AuthResult.Success)
        assertEquals(UserRole.OBSERVER, env.repo.session.value?.role)
        assertTrue("Телеметрія доступна всім ролям", env.repo.telemetry() is AuthResult.Success)

        val result = env.repo.users() as AuthResult.Failure
        assertTrue(result.error is AuthError.Forbidden)
        assertNotNull("403 не скидає сесію", env.repo.session.value)
    }

    @Test
    fun forgedAdminTokenIsRejectedByServer() = runBlocking {
        val env = Env()
        env.repo.login("observer", "Observer#2026", 60)
        val forged = env.repo.forgeAdminToken()
        assertEquals("UI повірив підробленій ролі", UserRole.ADMIN, forged?.role)

        val result = env.repo.users() as AuthResult.Failure
        assertTrue("Підпис не збігається → 401", result.error is AuthError.InvalidToken)
        assertNull("Сесію очищено", env.repo.session.value)
        assertNull("Токен видалено зі сховища", env.storage.currentToken())
    }
}
