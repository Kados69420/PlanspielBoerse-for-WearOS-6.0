package de.pbwear.data

import kotlinx.serialization.Serializable

/** Alle Felder sind nullable bzw. mit Defaults versehen, weil der Server viele Felder weglässt. */

@Serializable
data class BaseResponse<T>(
    val success: Boolean = false,
    val result: T? = null,
    val messages: List<Message> = emptyList(),
)

@Serializable
data class Message(
    val summary: String? = null,
    val detail: String? = null,
)

@Serializable
data class LoginRequest(val userName: String, val password: String)

@Serializable
data class Tokens(val accessToken: String = "", val refreshToken: String = "")

@Serializable
data class TokenAndUser(val user: User? = null, val token: Tokens? = null)

@Serializable
data class User(
    val id: Int = 0,
    val username: String? = null,
    val firstName: String? = null,
    val userPortfolios: List<UserPortfolio> = emptyList(),
)

@Serializable
data class UserPortfolio(
    val id: Int = 0,
    val depotName: String? = null,
)

@Serializable
data class Portfolio(
    val id: Int = 0,
    val name: String? = null,
    val credit: Double = 0.0,
    val availableCredit: Double = 0.0,
    val value: Double = 0.0,
    val valueOfInstruments: Double = 0.0,
    val performanceAbs: Double = 0.0,
    val performanceRel: Double = 0.0,
    val performanceAbsTotal: Double = 0.0,
    val performanceRelTotal: Double = 0.0,
    val rank: Int = 0,
    val numTrades: Int = 0,
    val isoCurrency: String? = null,
)

@Serializable
data class PortfolioWithItems(
    val portfolio: Portfolio? = null,
    val items: List<Item> = emptyList(),
)

@Serializable
data class Item(
    val instrument: Instrument? = null,
    val quantity: Double = 0.0,
    val currentValue: Double = 0.0,
    val totalAmount: Double = 0.0,
    val performanceAbsTotal: Double = 0.0,
    val performanceRelTotal: Double = 0.0,
    val isoCurrency: String? = null,
)

@Serializable
data class Instrument(
    val isin: String? = null,
    val wkn: String? = null,
    val name: String? = null,
    val idExternal: String? = null,
    val instrumentType: String? = null,
    val isoCurrency: String? = null,
    val price: Double? = null,
    val bid: Double? = null,
    val ask: Double? = null,
    val performanceRel: Double? = null,
    val quoteList: List<QuoteList> = emptyList(),
)

@Serializable
data class QuoteList(
    val idExchange: String? = null,
    val exchangeName: String? = null,
    val price: Double? = null,
    val bid: Double? = null,
    val ask: Double? = null,
    val valid: Boolean = false,
)

@Serializable
data class Watchlist(
    val instrument: Instrument? = null,
    val note: String? = null,
)

@Serializable
data class Order(
    val id: Long = 0,
    val instrument: Instrument? = null,
    val orderType: String? = null,
    val orderSubType: String? = null,
    val orderStatus: String? = null,
    val quantity: Double = 0.0,
    val priceLimit: Double? = null,
    val stopLimit: Double? = null,
    val expirationDate: String? = null,
    val isoCurrency: String? = null,
)

@Serializable
data class PlaceOrder(
    val isin: String,
    val orderSubType: String,
    val orderType: String,
    val quantity: Long,
    val stop: Double,
    val limit: Double,
    val validUntil: String,
    val portfolioId: Int,
    val idExchange: Int,
    /** Die Original-App sendet im Planspiel-Theme immer ein Notiz-Feld mit (leer, wenn nichts eingegeben). */
    val noteText: String,
)

@Serializable
data class PlaceOrderResponse(
    val success: Boolean = false,
    val messages: List<Message> = emptyList(),
)

@Serializable
data class RankingRequest(
    val additionalRanking: Boolean = false,
    val filter: String,
    val page: Int,
    val pageSize: Int,
    val periodDetail: Int,
    val periodYear: Int,
    val periodType: String,
    val rankingColumn: String,
    val phaseId: String,
)

@Serializable
data class RankingResponse(
    val content: List<Ranking> = emptyList(),
    val userRankStandard: Int? = null,
    val performance: Double = 0.0,
    val depotValue: Double = 0.0,
    val totalElements: Int = 0,
)

@Serializable
data class Ranking(
    val name: String? = null,
    val depotName: String? = null,
    val rankStandard: Int? = null,
    val performance: Double = 0.0,
    val depotValue: Double = 0.0,
    val depotId: Int = 0,
)

@Serializable
data class PhaseResponse(
    val code: String? = null,
    val label: String? = null,
    val value: String? = null,
)

@Serializable
data class SearchRequest(
    val searchString: String,
    val simpleSearch: Boolean = true,
    val instrumentUniverseType: String = "STOCKS",
    val page: Int = 0,
    val size: Int = 20,
)

@Serializable
data class SearchResponse(
    val content: List<Instrument> = emptyList(),
    val totalElements: Int = 0,
)
