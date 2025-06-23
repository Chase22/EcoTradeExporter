package de.chasenet.eco.trade

import java.time.ZonedDateTime

data class Trade(
    val trader: String,
    val store: String,
    val timestamp: ZonedDateTime,
    val transactions: List<Transaction>,
)

enum class Direction {
    BUY,
    SELL,
}

data class Transaction(
    val item: String,
    val amount: Int,
    val price: Double,
    val direction: Direction,
) {
    val total: Double
        get() = amount * price
}
