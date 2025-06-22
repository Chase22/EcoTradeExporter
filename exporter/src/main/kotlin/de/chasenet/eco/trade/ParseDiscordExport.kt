package de.chasenet.eco.trade

import de.chasenet.eco.trade.extensions.round
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


private val json = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        contextual(OffsetDateTimeSerializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun parseDiscordExport(file: Path) =
    file.inputStream().use<InputStream, DiscordExport>(json::decodeFromStream)

private val titleRegex = Regex("^(.+) traded at (.+)$")
private val descriptionRegex = Regex("^([\\d.]+) X (.+) \\* ([\\d.]+) = ([\\d.]+)$")

fun parseEmbed(embed: DiscordEmbed): Trade {
    val title = embed.title!!
    val (trader, store) = titleRegex.matchEntire(title)?.destructured
        ?: throw IllegalArgumentException("Title does not match expected format: $title")

    val transactions = embed.fields.filter { it.name == "Bought" || it.name == "Sold" }.map { field ->
        val tradeLine = field.value.lines().first()
        val matchResult = descriptionRegex.matchEntire(tradeLine)
            ?: throw IllegalArgumentException("Field value does not match expected format: $tradeLine")

        val (amount, item, pricePerUnit, total) = matchResult.destructured
        val amountDouble = amount.toDouble().round(3)
        val pricePerUnitDouble = pricePerUnit.toDouble().round(3)
        val totalDouble = total.toDouble().round(3)

        val totalPrice = (amountDouble * pricePerUnitDouble).round(3)
        if (totalDouble != totalPrice) {
            throw IllegalArgumentException("Inconsistent values in field: Expected: $totalDouble got: $amountDouble * $pricePerUnitDouble = $totalPrice")
        }

        val direction = if (field.name == "Bought") Direction.BUY else Direction.SELL

        Transaction(
            item = item,
            amount = amount.toInt(),
            price = pricePerUnit.toDouble(),
            direction = direction
        )
    }

    return Trade(
        trader = trader,
        store = store,
        transactions = transactions
    )
}

fun parseTrades(): List<Trade> {
    val dataDir = Path.of("data")
    val jsons = dataDir.listDirectoryEntries("*.json")

    val json = jsons.maxBy { it.name }

    val export = parseDiscordExport(json)

    return export.messages.flatMap { it.embeds.map(::parseEmbed) }
}

fun main() {
    parseTrades()
    println()
}
