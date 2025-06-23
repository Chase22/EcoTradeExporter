package de.chasenet.eco.trade

import de.chasenet.eco.trade.extensions.round
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.time.OffsetDateTime
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val json =
    Json {
        ignoreUnknownKeys = true
        serializersModule =
            SerializersModule {
                contextual(OffsetDateTimeSerializer)
            }
    }

@OptIn(ExperimentalSerializationApi::class)
fun parseDiscordExport(file: Path) = file.inputStream().use<InputStream, DiscordExport>(json::decodeFromStream)

private val titleRegex = Regex("^(.+) traded at (.+)$")
private val descriptionRegex = Regex("^([\\d.]+) X (.+) \\* ([\\d.]+) = ([\\d.]+)$")

fun parseEmbed(
    embed: DiscordEmbed,
    timestamp: OffsetDateTime,
): Trade {
    val title = embed.title!!
    val (trader, store) =
        titleRegex.matchEntire(title)?.destructured
            ?: throw IllegalArgumentException("Title does not match expected format: $title")

    val transactions =
        embed.fields.filter { it.name == "Bought" || it.name == "Sold" }.map { field ->
            val tradeLine = field.value.lines().first()
            val matchResult =
                descriptionRegex.matchEntire(tradeLine)
                    ?: throw IllegalArgumentException("Field value does not match expected format: $tradeLine")

            val (amount, item, pricePerUnit, total) = matchResult.destructured
            val amountDouble = amount.toDouble().round(3)
            val pricePerUnitDouble = pricePerUnit.toDouble().round(3)
            val totalDouble = total.toDouble().round(3)

            val totalPrice = (amountDouble * pricePerUnitDouble).round(2)
            /*if (totalDouble != totalPrice) {
                throw IllegalArgumentException(
                    "Inconsistent values in field: Expected: $totalDouble got: $amountDouble * $pricePerUnitDouble = $totalPrice",
                )
            }*/

            val direction = if (field.name == "Bought") Direction.BUY else Direction.SELL

            Transaction(
                item = item,
                amount = amount.toInt(),
                price = pricePerUnit.toDouble(),
                direction = direction,
            )
        }

    return Trade(
        trader = trader,
        store = store,
        timestamp = timestamp.toZonedDateTime(),
        transactions = transactions,
    )
}

fun parseTrades(): List<Trade> {
    val dataDir = Path.of("data")
    val jsons = dataDir.listDirectoryEntries("*.json")

    val json = jsons.maxBy { it.name }

    val export = parseDiscordExport(json)

    return export.messages.flatMap { message -> message.embeds.map { parseEmbed(it, message.timestamp) } }
}

data class UserToUserTransaction(
    val from: String,
    val to: String,
    val amount: Double,
)

fun main() {
    val stores =
        File("stores.txt")
            .takeIf { it.exists() }
            ?.readLines()
            ?.filter { it.isNotBlank() && !it.startsWith("#") }
            ?.map { it.split("=").map(String::trim) }
            ?.associate { it[0] to it[1] } ?: emptyMap()

    val trades = parseTrades()

    val unknownStores =
        trades
            .map { it.store }
            .toSet()
            .sorted()
            .filterNot { stores.contains(it) }

    if (unknownStores.isNotEmpty()) {
        println("Unknown stores:")
        unknownStores.forEach { println(it) }
        (unknownStores.associateWith { null } + stores)
            .toList()
            .sortedBy { it.first }
            .joinToString("\n") {
                "${it.first} = ${it.second ?: ""}"
            }.let { File("stores.txt").writeText(it) }
    } else {
        println("All stores are known.")
    }

    val userToUserTransactions =
        trades.flatMap { trade ->
            trade.transactions.map { transaction ->
                val storeOwner =
                    stores[trade.store]
                        ?: throw IllegalArgumentException("Store ${trade.store} not found in stores.txt")

                if (transaction.direction == Direction.BUY) {
                    UserToUserTransaction(
                        from = trade.trader,
                        to = storeOwner,
                        amount = transaction.total,
                    )
                } else {
                    UserToUserTransaction(
                        from = storeOwner,
                        to = trade.trader,
                        amount = transaction.total,
                    )
                }
            }
        }

    val grouped =
        userToUserTransactions
            .groupBy(UserToUserTransaction::from)
            .mapValues {
                it.value.groupBy(UserToUserTransaction::to).mapValues { it.value.sumOf { it.amount } }
            }.toSortedMap()

    val names = grouped.keys.sorted()

    val matrix =
        names.associateWith { from ->
            names.associateWith { to ->
                grouped[from]?.get(to) ?: 0.0
            }
        }

    println(
        matrix.entries.joinToString(",\n", "[", "]") { it.value.values.joinToString(",", "[", "]") { it.round(2).toString() } },
    )

    println()
}
