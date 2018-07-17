package net.devthedevel.lotti.utils

import java.util.*

fun <T : Any> List<T>.random(): T {
    return this[Random().nextInt(this.size)]
}

fun IntRange.delta(): Int = this.endInclusive - this.start + 1

fun String.isDiscordId(): Boolean = this.startsWith("<@") && this.endsWith(">")

fun String.getDiscordId(): Long = this.removeSurrounding("<@", ">").toLong()

fun String.parseToInt(lower: Int = Int.MIN_VALUE, higher: Int = Int.MAX_VALUE): Int? {
    var ret: Int? = null
    if (this.isNotEmpty()) {
        try {
            val num = this.toDouble().toInt()
            ret = if (num < lower) lower else num
            ret = if (num > higher) higher else num
        } catch (e: NumberFormatException) { }
    }
    return ret
}