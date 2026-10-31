package com.example.mse.data.remote.mock

import com.example.mse.model.FaultScenario
import com.example.mse.model.LevelPointDto
import com.example.mse.model.StationTelemetryResponse
import com.example.mse.model.TelemetryFlowsDto
import com.example.mse.model.TelemetryReservoirDto
import com.example.mse.model.TelemetryStationDto
import com.example.mse.model.UnitStatusDto
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sin
import kotlin.random.Random

class FaultInjectionScadaServer(
    private val gson: Gson = Gson(),
    private val latencyMs: Long = 300,
    private val timeoutDelayMs: Long = 1_200,
    private val random: Random = Random.Default
) : Interceptor {
    @Volatile
    var scenario: FaultScenario = FaultScenario.SUCCESS
        set(value) {
            field = value
            flakyCounter.set(0)
        }

    private val flakyCounter = AtomicInteger(0)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (latencyMs > 0) Thread.sleep(latencyMs)
        val effective = if (scenario == FaultScenario.RANDOM) randomScenario() else scenario
        return when (effective) {
            FaultScenario.SUCCESS, FaultScenario.RANDOM -> success(request)
            FaultScenario.FLAKY_503 ->
                if (flakyCounter.incrementAndGet() <= 2) http(request, 503, "service_unavailable", retryAfter = null) else success(request)
            FaultScenario.FLAKY_TIMEOUT -> {
                if (flakyCounter.incrementAndGet() <= 1) {
                    Thread.sleep(timeoutDelayMs)
                    throw SocketTimeoutException("timeout: SCADA did not respond in time")
                }
                success(request)
            }
            FaultScenario.NO_INTERNET -> throw UnknownHostException("Unable to resolve host \"${request.url.host}\": No address associated with hostname")
            FaultScenario.TIMEOUT -> {
                Thread.sleep(timeoutDelayMs)
                throw SocketTimeoutException("timeout")
            }
            FaultScenario.HTTP_400 -> http(request, 400, "bad_request")
            FaultScenario.HTTP_401 -> http(request, 401, "unauthorized")
            FaultScenario.HTTP_403 -> http(request, 403, "forbidden")
            FaultScenario.HTTP_404 -> http(request, 404, "not_found")
            FaultScenario.HTTP_429 -> http(request, 429, "too_many_requests", retryAfter = 2)
            FaultScenario.HTTP_500 -> http(request, 500, "internal_error")
            FaultScenario.HTTP_502 -> http(request, 502, "bad_gateway")
            FaultScenario.HTTP_503 -> http(request, 503, "service_unavailable")
            FaultScenario.MALFORMED_JSON -> raw(request, 200, """{"station": {"code": "KYV-01" "name": "Київська ГЕС"}, "reservoir": {"levelM": 102.7}}""")
        }
    }

    private fun randomScenario(): FaultScenario {
        val pool = listOf(FaultScenario.SUCCESS, FaultScenario.SUCCESS, FaultScenario.HTTP_503, FaultScenario.HTTP_500, FaultScenario.TIMEOUT, FaultScenario.HTTP_404)
        return pool[random.nextInt(pool.size)]
    }

    private fun success(request: Request): Response {
        val code = request.url.pathSegments.lastOrNull().orEmpty()
        val station = STATIONS[code] ?: return http(request, 404, "station_not_found")
        val hours = request.url.queryParameter("hours")?.toIntOrNull()?.coerceIn(1, 72) ?: 12
        val level = station.second + random.nextDouble(-0.15, 0.1)
        val units = (1..4).map { i ->
            val running = random.nextDouble() > 0.2
            UnitStatusDto("ГА-$i", if (running) random.nextDouble(15.0, 21.0) else 0.0, if (running) "В роботі" else "Резерв")
        }
        val body = StationTelemetryResponse(
            station = TelemetryStationDto(code, station.first, if (code.startsWith("DNS")) "Дністер" else "Дніпро"),
            reservoir = TelemetryReservoirDto(level, station.second, 3_730.0 * (level / station.second)),
            flows = TelemetryFlowsDto(1_150.0 + random.nextDouble(-60.0, 60.0), 1_020.0, if (level > station.second) 180.0 else 0.0),
            units = units,
            history = (0 until hours step maxOf(1, hours / 12)).map { h -> LevelPointDto(h, level - 0.02 * h + 0.05 * sin(h / 3.0)) },
            generatedAt = System.currentTimeMillis()
        )
        return raw(request, 200, gson.toJson(body))
    }

    private fun http(request: Request, code: Int, error: String, retryAfter: Long? = null): Response {
        val response = raw(request, code, """{"error":"$error","code":$code}""")
        return if (retryAfter != null) response.newBuilder().header("Retry-After", retryAfter.toString()).build() else response
    }

    private fun raw(request: Request, code: Int, json: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "HTTP $code")
        .body(json.toResponseBody("application/json; charset=utf-8".toMediaType()))
        .build()

    companion object {
        val STATIONS = mapOf(
            "KYV-01" to ("Київська ГЕС" to 103.0),
            "KNV-01" to ("Канівська ГЕС" to 91.5),
            "KRM-01" to ("Кременчуцька ГЕС" to 81.0),
            "DNS-01" to ("Дністровська ГЕС" to 121.0)
        )
    }
}
