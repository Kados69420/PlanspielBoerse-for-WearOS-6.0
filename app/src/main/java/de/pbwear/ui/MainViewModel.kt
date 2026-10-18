package de.pbwear.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pbwear.data.Api
import de.pbwear.data.ApiException
import de.pbwear.data.Instrument
import de.pbwear.data.Item
import de.pbwear.data.Order
import de.pbwear.data.PlaceOrder
import de.pbwear.data.PortfolioWithItems
import de.pbwear.data.RankingResponse
import de.pbwear.data.Session
import de.pbwear.data.Watchlist
import kotlinx.coroutines.launch

/** Lade-Zustand eines Bildschirms. */
sealed interface Load<out T> {
    data object Idle : Load<Nothing>
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

enum class OrderMode { BUY, SELL, STOP_LOSS }

enum class OrderStage { LOADING, FORM, CONFIRM, SENDING, DONE }

/** Alles, was für eine Order auf der Uhr gebraucht wird. */
data class OrderDraft(
    val mode: OrderMode,
    val isin: String,
    val name: String,
    val idExternal: String?,
    val price: Double,
    val maxQuantity: Long?,
    val currency: String,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val session = Session(app)
    private val api = Api(session)

    var loggedIn by mutableStateOf(session.isLoggedIn)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set
    var loginBusy by mutableStateOf(false)
        private set

    var depot by mutableStateOf<Load<PortfolioWithItems>>(Load.Idle)
        private set
    var watchlist by mutableStateOf<Load<List<Watchlist>>>(Load.Idle)
        private set
    var openOrders by mutableStateOf<Load<List<Order>>>(Load.Idle)
        private set
    var ranking by mutableStateOf<Load<RankingResponse>>(Load.Idle)
        private set
    var search by mutableStateOf<Load<List<Instrument>>>(Load.Idle)
        private set

    // ---- Chart-Zustand
    var chartName by mutableStateOf("")
        private set
    var chartWeek by mutableStateOf(false)
        private set
    var chart by mutableStateOf<Load<Bitmap>>(Load.Idle)
        private set
    private var chartIsin: String? = null

    /** Aktuell gewählte Depotposition (Detailansicht). */
    var selectedItem by mutableStateOf<Item?>(null)
        private set

    // ---- Order-Zustand
    var draft by mutableStateOf<OrderDraft?>(null)
        private set
    var stage by mutableStateOf(OrderStage.LOADING)
        private set
    var exchangeId by mutableStateOf<Int?>(null)
        private set
    var orderError by mutableStateOf<String?>(null)
        private set
    var orderResult by mutableStateOf<String?>(null)
        private set
    var orderSuccess by mutableStateOf(false)
        private set

    val userName: String get() = session.userName

    // ------------------------------------------------------------------ Login

    fun login(user: String, password: String) {
        if (user.isBlank() || password.isEmpty()) {
            loginError = "Benutzername und Passwort eingeben"
            return
        }
        loginBusy = true
        loginError = null
        viewModelScope.launch {
            try {
                api.login(user.trim(), password)
                loggedIn = true
            } catch (e: ApiException) {
                loginError = e.message
            } catch (e: Exception) {
                loginError = "Anmeldung fehlgeschlagen"
            } finally {
                loginBusy = false
            }
        }
    }

    fun logout() {
        api.logout()
        loggedIn = false
        depot = Load.Idle
        watchlist = Load.Idle
        openOrders = Load.Idle
        ranking = Load.Idle
        search = Load.Idle
        selectedItem = null
        draft = null
    }

    private fun handleAuthFailure(message: String) {
        if (message.startsWith("Sitzung abgelaufen")) {
            api.logout()
            loggedIn = false
            loginError = message
        }
    }

    // ------------------------------------------------------------------ Laden

    fun loadDepot() {
        depot = Load.Loading
        viewModelScope.launch {
            depot = try {
                Load.Ok(api.portfolio())
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Daten nicht lesbar")
            }
        }
    }

    fun loadWatchlist() {
        watchlist = Load.Loading
        viewModelScope.launch {
            watchlist = try {
                Load.Ok(api.watchlist())
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Daten nicht lesbar")
            }
        }
    }

    fun loadOpenOrders() {
        openOrders = Load.Loading
        viewModelScope.launch {
            openOrders = try {
                Load.Ok(api.openOrders())
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Daten nicht lesbar")
            }
        }
    }

    fun loadRanking() {
        ranking = Load.Loading
        viewModelScope.launch {
            ranking = try {
                Load.Ok(api.ranking())
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Daten nicht lesbar")
            }
        }
    }

    fun searchStocks(query: String) {
        if (query.trim().length < 3) {
            search = Load.Err("Mindestens 3 Zeichen")
            return
        }
        search = Load.Loading
        viewModelScope.launch {
            search = try {
                Load.Ok(api.searchStocks(query.trim()).filter { it.instrumentType == null || it.instrumentType == "STOCK" })
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Daten nicht lesbar")
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            try {
                api.cancelOrder(orderId)
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
            } catch (e: Exception) {
                // wird durch das Neuladen sichtbar
            }
            loadOpenOrders()
        }
    }

    fun openChart(isin: String, name: String) {
        chartIsin = isin
        chartName = name
        chartWeek = false
        loadChart()
    }

    fun selectChartPeriod(week: Boolean) {
        if (chartWeek == week && chart is Load.Ok) return
        chartWeek = week
        loadChart()
    }

    fun loadChart() {
        val isin = chartIsin ?: return
        chart = Load.Loading
        val week = chartWeek
        viewModelScope.launch {
            val result: Load<Bitmap> = try {
                Load.Ok(api.chartImage(isin, week, 400, 300))
            } catch (e: ApiException) {
                Load.Err(e.message ?: "Fehler")
            } catch (e: Exception) {
                Load.Err("Chart nicht ladbar")
            }
            // Nur übernehmen, wenn der Zeitraum inzwischen nicht gewechselt wurde
            if (chartWeek == week && chartIsin == isin) chart = result
        }
    }

    fun selectItem(item: Item) {
        selectedItem = item
    }

    // ------------------------------------------------------------------ Order

    /** Startet eine Order und lädt die Börsenplätze des Instruments. */
    fun startOrder(d: OrderDraft) {
        draft = d
        stage = OrderStage.LOADING
        exchangeId = null
        orderError = null
        orderResult = null
        orderSuccess = false
        viewModelScope.launch {
            try {
                val inst = api.instrumentWithQuotes(d.isin, d.idExternal)
                // Wie die Original-App: Börsenplatz Stuttgart bevorzugen, sonst erster gültiger Platz
                val usable = inst.quoteList.filter { it.idExchange?.toIntOrNull() != null }
                val quote = usable.firstOrNull { it.exchangeName?.contains("stuttgart", ignoreCase = true) == true }
                    ?: usable.firstOrNull { it.valid }
                    ?: usable.firstOrNull()
                val id = quote?.idExchange?.toIntOrNull()
                if (id == null) {
                    orderError = "Kein Handelsplatz verfügbar"
                } else {
                    exchangeId = id
                }
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                orderError = e.message
            } catch (e: Exception) {
                orderError = "Handelsplatz nicht ladbar"
            }
            stage = OrderStage.FORM
        }
    }

    fun toConfirm() {
        stage = OrderStage.CONFIRM
    }

    fun backToForm() {
        stage = OrderStage.FORM
    }

    /**
     * Sendet die Order.
     * KAUF/VERKAUF = "BEST" (stop=0, limit=0), STOP-LOSS = Verkauf mit Untertyp "STOP" (stop=limit=Preis).
     */
    fun sendOrder(quantity: Long, stopPrice: Double, validDays: Int) {
        val d = draft ?: return
        val exchange = exchangeId ?: return
        stage = OrderStage.SENDING
        val order = when (d.mode) {
            OrderMode.BUY -> PlaceOrder(
                isin = d.isin, orderSubType = "BEST", orderType = "BUY", quantity = quantity,
                stop = 0.0, limit = 0.0, validUntil = Api.validUntil(1),
                portfolioId = portfolioId(), idExchange = exchange, noteText = "",
            )
            OrderMode.SELL -> PlaceOrder(
                isin = d.isin, orderSubType = "BEST", orderType = "SELL", quantity = quantity,
                stop = 0.0, limit = 0.0, validUntil = Api.validUntil(1),
                portfolioId = portfolioId(), idExchange = exchange, noteText = "",
            )
            OrderMode.STOP_LOSS -> PlaceOrder(
                isin = d.isin, orderSubType = "STOP", orderType = "SELL", quantity = quantity,
                stop = stopPrice, limit = stopPrice, validUntil = Api.validUntil(validDays),
                portfolioId = portfolioId(), idExchange = exchange, noteText = "",
            )
        }
        viewModelScope.launch {
            try {
                val r = api.placeOrder(order)
                orderSuccess = r.success
                orderResult = r.messages.firstOrNull()?.let { it.detail ?: it.summary }
                    ?: if (r.success) "Order aufgegeben" else "Order abgelehnt"
            } catch (e: ApiException) {
                handleAuthFailure(e.message ?: "")
                orderSuccess = false
                orderResult = e.message ?: "Fehler"
            } catch (e: Exception) {
                orderSuccess = false
                orderResult = "Order fehlgeschlagen"
            }
            stage = OrderStage.DONE
        }
    }

    private fun portfolioId(): Int = session.portfolioId

    fun finishOrder() {
        draft = null
        stage = OrderStage.LOADING
        // Depot + offene Orders sind nach einer Order veraltet
        depot = Load.Idle
        openOrders = Load.Idle
    }
}
