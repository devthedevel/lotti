package net.devthedevel.lotti.utils

import java.util.*

fun <T : Any> List<T>.random(): T {
    return this[Random().nextInt(this.size)]
}

fun IntRange.delta(): Int = this.endInclusive - this.start + 1

fun String.isDiscordId(): Boolean = this.startsWith("<@") && this.endsWith(">")

fun String.getDiscordId(): Long = this.removeSurrounding("<@", ">").toLong()
