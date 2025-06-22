package de.chasenet.eco.trade

data class Trade(
    val trader: String,
    val store: String,
    val transactions: List<Transaction>,
)

enum class Direction {
    BUY,
    SELL
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
