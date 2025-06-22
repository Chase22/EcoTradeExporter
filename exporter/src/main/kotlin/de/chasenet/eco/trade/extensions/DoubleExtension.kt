package de.chasenet.eco.trade.extensions

import kotlin.math.floor
import kotlin.math.pow

fun Double.round(decimals: Int = 2): Double {
    val factor = 10.0.pow(decimals)
    return floor(this * factor) / factor
}