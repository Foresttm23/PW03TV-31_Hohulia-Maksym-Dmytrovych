package com.example.mse.data.remote

import com.example.mse.data.auth.TokenStorage
import com.example.mse.data.remote.mock.MockAuthServer
import com.example.mse.model.AuditEntryDto
import com.example.mse.model.GateCommandRequest
import com.example.mse.model.GateCommandResponse
import com.example.mse.model.LoginRequest
import com.example.mse.model.LoginResponse
import com.example.mse.model.SecureTelemetryDto
import com.example.mse.model.UserDto
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers("${AuthInterceptor.NO_AUTH_HEADER}: true")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/secure/telemetry")
    suspend fun telemetry(): SecureTelemetryDto

    @Headers("${AuthInterceptor.NO_AUTH_HEADER}: true")
    @GET("api/secure/telemetry")
    suspend fun telemetryWithoutToken(): SecureTelemetryDto

    @POST("api/secure/gates/command")
    suspend fun gateCommand(@Body request: GateCommandRequest): GateCommandResponse

    @GET("api/secure/admin/users")
    suspend fun users(): List<UserDto>

    @GET("api/secure/admin/audit")
    suspend fun audit(): List<AuditEntryDto>
}

class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Службовий заголовок позначає запити без токена і не повинен потрапити на сервер.
        if (original.header(NO_AUTH_HEADER) != null) {
            return chain.proceed(original.newBuilder().removeHeader(NO_AUTH_HEADER).build())
        }
        val token = tokenStorage.currentToken() ?: return chain.proceed(original)
        return chain.proceed(original.newBuilder().header("Authorization", "Bearer $token").build())
    }

    companion object {
        const val NO_AUTH_HEADER = "No-Auth"
    }
}

object AuthNetwork {
    private const val BASE_URL = "https://auth.hydro-ges.mock/"

    fun createApi(tokenStorage: TokenStorage, gson: Gson = Gson(), server: Interceptor = MockAuthServer(gson)): AuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
            })
            .addInterceptor(server)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApi::class.java)
    }
}
