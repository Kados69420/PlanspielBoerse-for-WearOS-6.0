package de.pbwear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.pbwear.data.Instrument
import de.pbwear.data.Item
import kotlin.math.max
import kotlin.math.round

private fun roundCent(v: Double): Double = round(v * 100.0) / 100.0

// ============================================================================ Login

@Composable
fun LoginScreen(vm: MainViewModel) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    ScreenList {
        item { Title("Planspiel Börse") }
        item { InputField(user, { user = it }, "Benutzername") }
        item { InputField(pass, { pass = it }, "Passwort", password = true, imeAction = ImeAction.Done) }
        vm.loginError?.let { err -> item { Body(err, PB.Loss) } }
        item {
            if (vm.loginBusy) {
                Body("Anmelden …")
            } else {
                PrimaryChip("Anmelden") { vm.login(user, pass) }
            }
        }
    }
}

// ============================================================================ Home

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onDepot: () -> Unit,
    onWatchlist: () -> Unit,
    onSearch: () -> Unit,
    onOrders: () -> Unit,
    onRanking: () -> Unit,
) {
    ScreenList {
        item { Title("Planspiel Börse") }
        item { PrimaryChip("Depot", onDepot) }
        item { GreenChip("Aktie kaufen (Watchlist)", onWatchlist) }
        item { PBChip("Aktie suchen", onClick = onSearch) }
        item { PBChip("Offene Orders", onClick = onOrders) }
        item { PBChip("Rangliste", onClick = onRanking) }
        item { Hint(vm.userName) }
        item { PBChip("Abmelden", background = Color.Black, content = PB.Loss) { vm.logout() } }
    }
}

// ============================================================================ Depot

@Composable
fun DepotScreen(vm: MainViewModel, onPosition: () -> Unit) {
    LaunchedEffect(Unit) {
        if (vm.depot is Load.Idle || vm.depot is Load.Err) vm.loadDepot()
    }
    when (val s = vm.depot) {
        is Load.Idle, is Load.Loading -> Spinner()
        is Load.Err -> ScreenList {
            item { Body(s.message, PB.Loss) }
            item { PrimaryChip("Erneut laden") { vm.loadDepot() } }
        }
        is Load.Ok -> {
            val p = s.data.portfolio
            ScreenList {
                item { Title("Depot") }
                if (p != null) {
                    item { Hint("Gesamtwert") }
                    item { Body(money(p.value, p.isoCurrency), bold = true) }
                    item { Body(percent(p.performanceRelTotal), perfColor(p.performanceRelTotal), bold = true) }
                    item { Hint("Verfügbar: ${money(p.availableCredit, p.isoCurrency)}") }
                    if (p.rank > 0) item { Hint("Rang ${p.rank}") }
                }
                val positions = s.data.items
                if (positions.isEmpty()) {
                    item { Hint("Keine Positionen") }
                }
                items(count = positions.size) { i ->
                    val pos = positions[i]
                    PBChip(
                        label = pos.instrument?.name ?: pos.instrument?.isin ?: "Position",
                        secondary = "${quantity(pos.quantity)} Stk · ${money(pos.currentValue, pos.isoCurrency)} · ${percent(pos.performanceRelTotal)}",
                    ) {
                        vm.selectItem(pos)
                        onPosition()
                    }
                }
                item { PBChip("Aktualisieren") { vm.loadDepot() } }
            }
        }
    }
}

// ============================================================================ Position

@Composable
fun PositionScreen(vm: MainViewModel, onOrder: () -> Unit, onChart: () -> Unit) {
    val pos: Item? = vm.selectedItem
    val inst: Instrument? = pos?.instrument
    val isin: String? = inst?.isin
    if (pos == null || inst == null || isin == null) {
        ScreenList { item { Body("Keine Position gewählt") } }
        return
    }
    val price = inst.price ?: if (pos.quantity > 0) pos.currentValue / pos.quantity else 0.0
    val isStock = inst.instrumentType == "STOCK"
    val maxQty = pos.quantity.toLong()

    fun start(mode: OrderMode) {
        vm.startOrder(
            OrderDraft(
                mode = mode,
                isin = isin,
                name = inst.name ?: isin,
                idExternal = inst.idExternal,
                price = price,
                maxQuantity = if (mode == OrderMode.BUY) null else maxQty,
                currency = pos.isoCurrency ?: inst.isoCurrency ?: "EUR",
            ),
        )
        onOrder()
    }

    ScreenList {
        item { Title(inst.name ?: "Position") }
        item { Body("${quantity(pos.quantity)} Stück") }
        item { Body("Wert ${money(pos.currentValue, pos.isoCurrency)}") }
        item { Body(percent(pos.performanceRelTotal), perfColor(pos.performanceRelTotal), bold = true) }
        if (price > 0.0) item { Hint("Kurs ${money(price, pos.isoCurrency)}") }
        item { PBChip("Kursverlauf") { vm.openChart(isin, inst.name ?: isin); onChart() } }
        if (maxQty >= 1) {
            item { PrimaryChip("Verkaufen") { start(OrderMode.SELL) } }
            item { PBChip("Stop-Loss einrichten") { start(OrderMode.STOP_LOSS) } }
        }
        if (isStock) item { GreenChip("Nachkaufen") { start(OrderMode.BUY) } }
    }
}

// ============================================================================ Watchlist

@Composable
fun WatchlistScreen(vm: MainViewModel, onOrder: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadWatchlist() }
    when (val s = vm.watchlist) {
        is Load.Idle, is Load.Loading -> Spinner()
        is Load.Err -> ScreenList {
            item { Body(s.message, PB.Loss) }
            item { PrimaryChip("Erneut laden") { vm.loadWatchlist() } }
        }
        is Load.Ok -> {
            val stocks = s.data.mapNotNull { it.instrument }.filter { it.instrumentType == "STOCK" }
            ScreenList {
                item { Title("Watchlist") }
                if (stocks.isEmpty()) item { Hint("Keine Aktien in der Watchlist") }
                items(count = stocks.size) { i ->
                    val inst = stocks[i]
                    InstrumentChip(inst) { startBuy(vm, inst); onOrder() }
                }
            }
        }
    }
}

private fun instrumentPrice(inst: Instrument): Double =
    inst.price ?: inst.quoteList.firstOrNull { it.price != null }?.price ?: 0.0

private fun startBuy(vm: MainViewModel, inst: Instrument) {
    val isin = inst.isin ?: return
    vm.startOrder(
        OrderDraft(
            mode = OrderMode.BUY,
            isin = isin,
            name = inst.name ?: isin,
            idExternal = inst.idExternal,
            price = instrumentPrice(inst),
            maxQuantity = null,
            currency = inst.isoCurrency ?: "EUR",
        ),
    )
}

@Composable
private fun InstrumentChip(inst: Instrument, onClick: () -> Unit) {
    val price = instrumentPrice(inst)
    val perf = inst.performanceRel
    val secondary = buildString {
        append(if (price > 0.0) money(price, inst.isoCurrency) else "Kurs –")
        if (perf != null) append(" · ").append(percent(perf))
    }
    PBChip(label = inst.name ?: inst.isin ?: "Aktie", secondary = secondary, onClick = onClick)
}

// ============================================================================ Suche

@Composable
fun SearchScreen(vm: MainViewModel, onOrder: () -> Unit) {
    var query by remember { mutableStateOf("") }
    ScreenList {
        item { Title("Aktie suchen") }
        item { InputField(query, { query = it }, "Name oder WKN", imeAction = ImeAction.Search) }
        item { PrimaryChip("Suchen") { vm.searchStocks(query) } }
        when (val s = vm.search) {
            is Load.Idle -> {}
            is Load.Loading -> item { Body("Suche …") }
            is Load.Err -> item { Body(s.message, PB.Loss) }
            is Load.Ok -> {
                if (s.data.isEmpty()) item { Hint("Keine Treffer") }
                items(count = s.data.size) { i ->
                    val inst = s.data[i]
                    InstrumentChip(inst) { startBuy(vm, inst); onOrder() }
                }
            }
        }
    }
}

// ============================================================================ Offene Orders

@Composable
fun OrdersScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.loadOpenOrders() }
    var confirmId by remember { mutableLongStateOf(-1L) }
    when (val s = vm.openOrders) {
        is Load.Idle, is Load.Loading -> Spinner()
        is Load.Err -> ScreenList {
            item { Body(s.message, PB.Loss) }
            item { PrimaryChip("Erneut laden") { vm.loadOpenOrders() } }
        }
        is Load.Ok -> ScreenList {
            item { Title("Offene Orders") }
            if (s.data.isEmpty()) item { Hint("Keine offenen Orders") }
            items(count = s.data.size) { i ->
                val o = s.data[i]
                val kind = when (o.orderSubType) {
                    "STOP" -> if (o.orderType == "SELL") "Stop-Loss" else "Stop-Buy"
                    "LIMIT" -> "Limit"
                    "BEST" -> "Bestens"
                    else -> o.orderSubType ?: ""
                }
                val side = if (o.orderType == "SELL") "Verkauf" else "Kauf"
                val price = o.stopLimit ?: o.priceLimit
                val secondary = buildString {
                    append(quantity(o.quantity)).append(" Stk")
                    if (price != null && price > 0.0) append(" · ").append(money(price, o.isoCurrency))
                }
                if (confirmId == o.id) {
                    PBChip(
                        label = "Wirklich löschen?",
                        secondary = "Tippen zum Bestätigen",
                        background = PB.Red,
                        content = Color.White,
                    ) {
                        confirmId = -1L
                        vm.cancelOrder(o.id)
                    }
                } else {
                    PBChip(
                        label = "$side $kind · ${o.instrument?.name ?: ""}",
                        secondary = secondary,
                    ) { confirmId = o.id }
                }
            }
        }
    }
}

// ============================================================================ Rangliste

@Composable
fun RankingScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.loadRanking() }
    when (val s = vm.ranking) {
        is Load.Idle, is Load.Loading -> Spinner()
        is Load.Err -> ScreenList {
            item { Body(s.message, PB.Loss) }
            item { PrimaryChip("Erneut laden") { vm.loadRanking() } }
        }
        is Load.Ok -> ScreenList {
            item { Title("Rangliste") }
            s.data.userRankStandard?.let { rank ->
                item { Body("Mein Rang: $rank", bold = true) }
                item { Body(percent(s.data.performance), perfColor(s.data.performance)) }
            }
            if (s.data.content.isEmpty()) item { Hint("Keine Einträge") }
            items(count = s.data.content.size) { i ->
                val r = s.data.content[i]
                PBChip(
                    label = "${r.rankStandard?.toString() ?: "–"}. ${r.name ?: r.depotName ?: ""}",
                    secondary = percent(r.performance),
                ) {}
            }
        }
    }
}

// ============================================================================ Order

@Composable
fun OrderScreen(vm: MainViewModel, onClose: () -> Unit, onChart: () -> Unit) {
    val d = vm.draft
    if (d == null) {
        Spinner()
        return
    }
    val step = max(0.01, roundCent(d.price * 0.01))
    var qty by remember(d) { mutableLongStateOf(if (d.mode == OrderMode.BUY) 1L else (d.maxQuantity ?: 1L)) }
    var stop by remember(d) { mutableStateOf(roundCent(d.price * 0.95)) }
    var days by remember(d) { mutableIntStateOf(14) }

    fun setQty(v: Long) {
        val upper = d.maxQuantity ?: Long.MAX_VALUE
        qty = v.coerceIn(1L, upper)
    }

    val title = when (d.mode) {
        OrderMode.BUY -> "Kaufen"
        OrderMode.SELL -> "Verkaufen"
        OrderMode.STOP_LOSS -> "Stop-Loss"
    }

    when (vm.stage) {
        OrderStage.LOADING, OrderStage.SENDING -> Spinner()

        OrderStage.FORM -> ScreenList {
            item { Title(title) }
            item { Body(d.name, bold = true) }
            if (d.price > 0.0) item { Hint("Kurs ${money(d.price, d.currency)}") }
            vm.orderError?.let { err -> item { Body(err, PB.Loss) } }
            item { PBChip("Kursverlauf") { vm.openChart(d.isin, d.name); onChart() } }

            item { Hint("Menge") }
            item { Stepper("$qty", onMinus = { setQty(qty - 1) }, onPlus = { setQty(qty + 1) }) }
            item {
                Stepper("±10", minusLabel = "−10", plusLabel = "+10",
                    onMinus = { setQty(qty - 10) }, onPlus = { setQty(qty + 10) })
            }
            d.maxQuantity?.let { m -> item { PBChip("Alle ($m)") { setQty(m) } } }

            if (d.mode == OrderMode.STOP_LOSS) {
                item { Hint("Verkaufen, sobald der Kurs fällt auf") }
                item {
                    Stepper(
                        money(stop, d.currency),
                        onMinus = { stop = max(0.01, roundCent(stop - step)) },
                        onPlus = { stop = roundCent(stop + step) },
                    )
                }
                if (d.price > 0.0) {
                    item { Hint(percent((stop / d.price - 1.0) * 100.0) + " zum Kurs") }
                }
                item {
                    PBChip("Gültig: ${if (days == 1) "1 Tag" else "$days Tage"}") {
                        days = when (days) { 1 -> 7; 7 -> 14; else -> 1 }
                    }
                }
            } else if (d.price > 0.0) {
                item { Hint("ca. ${money(qty * d.price, d.currency)} (Bestens)") }
            }

            if (vm.exchangeId != null) {
                item {
                    when (d.mode) {
                        OrderMode.BUY -> GreenChip("Weiter") { vm.toConfirm() }
                        else -> PrimaryChip("Weiter") { vm.toConfirm() }
                    }
                }
            }
            item { PBChip("Abbrechen", background = Color.Black, content = PB.Loss) { vm.finishOrder(); onClose() } }
        }

        OrderStage.CONFIRM -> ScreenList {
            item { Title(title) }
            item { Body("$qty × ${d.name}", bold = true) }
            if (d.mode == OrderMode.STOP_LOSS) {
                item { Body("Stop bei ${money(stop, d.currency)}") }
                item { Hint("Gültig: ${if (days == 1) "1 Tag" else "$days Tage"}") }
            } else {
                item { Body("Bestens (zum nächsten Kurs)") }
                if (d.price > 0.0) item { Hint("ca. ${money(qty * d.price, d.currency)}") }
            }
            item {
                when (d.mode) {
                    OrderMode.BUY -> GreenChip("Jetzt kaufen") { vm.sendOrder(qty, stop, days) }
                    OrderMode.SELL -> PrimaryChip("Jetzt verkaufen") { vm.sendOrder(qty, stop, days) }
                    OrderMode.STOP_LOSS -> PrimaryChip("Stop-Loss setzen") { vm.sendOrder(qty, stop, days) }
                }
            }
            item { PBChip("Zurück") { vm.backToForm() } }
        }

        OrderStage.DONE -> ScreenList {
            item { Title(if (vm.orderSuccess) "Erledigt" else "Nicht ausgeführt") }
            item { Body(vm.orderResult ?: "", if (vm.orderSuccess) PB.Gain else PB.Loss) }
            item { PrimaryChip("OK") { vm.finishOrder(); onClose() } }
        }
    }
}


// ============================================================================ Chart

@Composable
fun ChartScreen(vm: MainViewModel) {
    ScreenList {
        item { Title(vm.chartName) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    PBChip(
                        label = "Tag",
                        background = if (!vm.chartWeek) PB.Red else PB.Gray,
                        content = Color.White,
                    ) { vm.selectChartPeriod(false) }
                }
                Box(Modifier.weight(1f)) {
                    PBChip(
                        label = "Woche",
                        background = if (vm.chartWeek) PB.Red else PB.Gray,
                        content = Color.White,
                    ) { vm.selectChartPeriod(true) }
                }
            }
        }
        when (val c = vm.chart) {
            is Load.Idle, is Load.Loading -> item { Body("Lade Chart …") }
            is Load.Err -> {
                item { Body(c.message, PB.Loss) }
                item { PBChip("Erneut laden") { vm.loadChart() } }
            }
            is Load.Ok -> item {
                Image(
                    bitmap = c.data.asImageBitmap(),
                    contentDescription = "Kursverlauf",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}
