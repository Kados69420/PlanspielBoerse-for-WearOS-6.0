package de.pbwear.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Fehler mit Text, der direkt auf der Uhr angezeigt werden kann. */
class ApiException(message: String) : Exception(message)

object Config {
    const val BASE = "https://backstage.planspiel-boerse.de/stockcontest/services/api/"
    const val CHART_BASE = "https://charts2.byteworx.de/bwcharts/images/MMC/"
    const val STAGE = "2e088ab22c495acabdedd3abab34235e"
    const val USER_AGENT = "AndroidTraderByteWorx-4.12.0"

    fun url(version: Int, path: String): HttpUrl = "${BASE}v-ms$version/$path".toHttpUrl()
}

/**
 * Sitzung: Tokens + Depot-ID, lokal gespeichert (App-privat).
 * Die Zugangsdaten selbst werden nicht gespeichert.
 */
class Session(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pb_session", Context.MODE_PRIVATE)

    var accessToken: String
        get() = prefs.getString("access", "") ?: ""
        set(v) = prefs.edit().putString("access", v).apply()

    var refreshToken: String
        get() = prefs.getString("refresh", "") ?: ""
        set(v) = prefs.edit().putString("refresh", v).apply()

    var portfolioId: Int
        get() = prefs.getInt("portfolio", 0)
        set(v) = prefs.edit().putInt("portfolio", v).apply()

    var userName: String
        get() = prefs.getString("user", "") ?: ""
        set(v) = prefs.edit().putString("user", v).apply()

    val isLoggedIn: Boolean get() = accessToken.isNotEmpty() && portfolioId != 0

    fun clear() {
        prefs.edit().clear().apply()
    }
}

class Api(private val session: Session) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ---------------------------------------------------------------- Low level

    private fun baseRequest(url: HttpUrl, withAuth: Boolean): Request.Builder {
        val b = Request.Builder()
            .url(url)
            .header("X-Angular-Connect-String", Config.STAGE)
            .header("User-Agent", Config.USER_AGENT)
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .header("Accept", "application/json")
        // Die Original-App sendet immer "Authorization: Bearer <token>", beim Login ohne Token "Bearer null".
        val token = if (withAuth) session.accessToken else ""
        b.header("Authorization", "Bearer ${token.ifEmpty { "null" }}")
        return b
    }

    private fun execute(build: () -> Request, withAuth: Boolean, path: String = ""): String {
        try {
            var response = client.newCall(build()).execute()
            if (withAuth && (response.code == 401 || response.code == 403) && session.refreshToken.isNotEmpty()) {
                response.close()
                if (refresh()) {
                    response = client.newCall(build()).execute()
                }
            }
            val code = response.code
            val text = response.use { it.body?.string().orEmpty() }
            if (code == 401 || code == 403) {
                throw ApiException(
                    if (withAuth) "Sitzung abgelaufen. Bitte neu anmelden."
                    else errorText(text) ?: "Benutzername oder Passwort falsch",
                )
            }
            if (code !in 200..299) {
                throw ApiException(
                    errorText(text) ?: "Serverfehler ($code) $path ${text.take(80).replace('\n', ' ')}".trim(),
                )
            }
            return text
        } catch (e: IOException) {
            throw ApiException("Keine Verbindung")
        }
    }

    private fun errorText(body: String): String? = try {
        val r = json.decodeFromString(BaseResponse.serializer(kotlinx.serialization.json.JsonElement.serializer()), body)
        r.messages.firstOrNull()?.let { it.detail ?: it.summary }
    } catch (e: Exception) {
        null
    }

    private fun get(version: Int, path: String, query: Map<String, String?> = emptyMap()): String {
        val builder = Config.url(version, path).newBuilder()
        query.forEach { (k, v) -> if (v != null) builder.addQueryParameter(k, v) }
        val url = builder.build()
        return execute({ baseRequest(url, true).get().build() }, true, path)
    }

    private fun post(version: Int, path: String, body: String, withAuth: Boolean = true): String {
        val url = Config.url(version, path)
        return execute(
            { baseRequest(url, withAuth).post(body.toRequestBody(jsonType)).build() },
            withAuth,
            path,
        )
    }

    /** Erneuert die Tokens (POST v-ms1/refresh-token). */
    @Synchronized
    private fun refresh(): Boolean {
        return try {
            val body = json.encodeToString(Tokens.serializer(), Tokens(session.accessToken, session.refreshToken))
            val url = Config.url(1, "refresh-token")
            val req = baseRequest(url, false).post(body.toRequestBody(jsonType)).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return false
                val parsed = json.decodeFromString(
                    BaseResponse.serializer(Tokens.serializer()),
                    r.body?.string().orEmpty(),
                )
                val t = parsed.result ?: return false
                if (t.accessToken.isEmpty()) return false
                session.accessToken = t.accessToken
                session.refreshToken = t.refreshToken
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- Endpunkte

    suspend fun login(userName: String, password: String) = withContext(Dispatchers.IO) {
        val body = json.encodeToString(LoginRequest.serializer(), LoginRequest(password = password, userName = userName))
        val text = post(7, "authenticate", body, withAuth = false)
        val resp = json.decodeFromString(BaseResponse.serializer(TokenAndUser.serializer()), text)
        val failure = ApiException(
            resp.messages.firstOrNull()?.let { it.detail ?: it.summary } ?: "Anmeldung fehlgeschlagen",
        )
        val result = resp.result ?: throw failure
        val token = result.token ?: throw failure
        if (!resp.success || token.accessToken.isEmpty()) throw failure
        val user = result.user ?: throw failure
        val portfolio = user.userPortfolios.firstOrNull() ?: throw ApiException("Kein Depot gefunden")
        session.accessToken = token.accessToken
        session.refreshToken = token.refreshToken
        session.portfolioId = portfolio.id
        session.userName = user.username ?: userName
    }

    suspend fun portfolio(): PortfolioWithItems = withContext(Dispatchers.IO) {
        val text = get(
            1, "portfolio/getPortfolioWithItems",
            mapOf(
                "portfolioId" to session.portfolioId.toString(),
                "withItems" to "true",
                "withOrderIds" to "false",
            ),
        )
        json.decodeFromString(PortfolioWithItems.serializer(), text)
    }

    suspend fun watchlist(): List<Watchlist> = withContext(Dispatchers.IO) {
        val text = get(1, "watchlist/getWatchlist")
        json.decodeFromString(ListSerializer(Watchlist.serializer()), text)
    }

    suspend fun openOrders(): List<Order> = withContext(Dispatchers.IO) {
        val text = get(
            1, "order/getOrders",
            mapOf("portfolioId" to session.portfolioId.toString(), "status" to "OPEN"),
        )
        json.decodeFromString(ListSerializer(Order.serializer()), text)
    }

    suspend fun cancelOrder(orderId: Long) = withContext(Dispatchers.IO) {
        // Original: Body ist Map<String,Int> -> {"id": <orderId>}
        post(1, "order/cancelOrder", "{\"id\":$orderId}")
        Unit
    }

    /** Instrument inkl. Börsenplätzen (quoteList), nötig für die Order. */
    suspend fun instrumentWithQuotes(isin: String, idExternal: String?): Instrument =
        withContext(Dispatchers.IO) {
            // Der Server verlangt idExternal. Fehlt es (z. B. bei Depotpositionen), erst über getInstrument holen.
            var ext = idExternal?.takeIf { it.isNotBlank() }
            var base: Instrument? = null
            if (ext == null) {
                val t = get(1, "instrument/getInstrument", mapOf("isin" to isin))
                base = json.decodeFromString(Instrument.serializer(), t)
                ext = base.idExternal?.takeIf { it.isNotBlank() }
            }
            if (ext == null) {
                return@withContext base ?: throw ApiException("Instrument nicht gefunden")
            }
            val text = get(1, "instrument/getInstrumentWithQuotes", mapOf("isin" to isin, "idExternal" to ext))
            json.decodeFromString(Instrument.serializer(), text)
        }

    /** Aktiensuche (v-ms4/instrument/getSearchInstrumentsPage), wie in der Original-App. */
    suspend fun searchStocks(query: String): List<Instrument> = withContext(Dispatchers.IO) {
        val body = json.encodeToString(SearchRequest.serializer(), SearchRequest(searchString = query))
        val text = post(4, "instrument/getSearchInstrumentsPage", body)
        json.decodeFromString(SearchResponse.serializer(), text).content
    }

    suspend fun placeOrder(order: PlaceOrder): PlaceOrderResponse = withContext(Dispatchers.IO) {
        val text = post(3, "order/placeOrder", json.encodeToString(PlaceOrder.serializer(), order))
        json.decodeFromString(PlaceOrderResponse.serializer(), text)
    }

    suspend fun ranking(): RankingResponse = withContext(Dispatchers.IO) {
        // Phase "Depot 1" aus ranking/phase-data (Fallback "2" wie in der Original-App)
        val phaseText = get(1, "ranking/phase-data")
        val phases = json.decodeFromString(ListSerializer(PhaseResponse.serializer()), phaseText)
        val phaseId = phases.firstOrNull { it.code == "Depot 1" }?.value ?: "2"
        val req = RankingRequest(
            additionalRanking = false,
            filter = "INSTITUTE",
            page = 0,
            pageSize = 10,
            periodDetail = 0,
            periodYear = Calendar.getInstance().get(Calendar.YEAR),
            periodType = "TOTAL",
            rankingColumn = "PERFORMANCE",
            phaseId = phaseId,
        )
        val text = post(7, "ranking/getRanking", json.encodeToString(RankingRequest.serializer(), req))
        json.decodeFromString(RankingResponse.serializer(), text)
    }

    /**
     * Kurs-Chart als PNG vom Chart-Server der Original-App.
     * Tag: intraday/intraday.png (1D), Woche: tw/tw.png (5D); Börsenplatz Stuttgart ("_STU").
     */
    suspend fun chartImage(isin: String, week: Boolean, width: Int, height: Int): Bitmap =
        withContext(Dispatchers.IO) {
            val path = if (week) "tw/tw.png" else "intraday/intraday.png"
            val url = (Config.CHART_BASE + path).toHttpUrl().newBuilder()
                .addQueryParameter("key.isin", isin)
                .addQueryParameter("key.codeMarket", "_STU")
                .addQueryParameter("timeSpan", if (week) "5D" else "1D")
                .addQueryParameter("chart.width", width.toString())
                .addQueryParameter("chart.height", height.toString())
                .build()
            // Kein Bearer-Token an den Chart-Server senden
            val req = Request.Builder().url(url)
                .header("User-Agent", Config.USER_AGENT)
                .header("X-Angular-Connect-String", Config.STAGE)
                .get().build()
            try {
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) throw ApiException("Chart nicht verfügbar (${r.code})")
                    val bytes = r.body?.bytes() ?: ByteArray(0)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw ApiException("Chart nicht lesbar")
                }
            } catch (e: IOException) {
                throw ApiException("Keine Verbindung")
            }
        }

    fun logout() = session.clear()

    companion object {
        /** Gültigkeit der Order als Datum (yyyy-MM-dd), wie in der Original-App. */
        fun validUntil(days: Int): String {
            val cal = Calendar.getInstance()
            // 1 Tag = heute, 7 Tage = heute + 7, 14 Tage = heute + 14 (wie in der Original-App)
            if (days > 1) cal.add(Calendar.DAY_OF_YEAR, days)
            return SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(cal.time)
        }
    }
}
