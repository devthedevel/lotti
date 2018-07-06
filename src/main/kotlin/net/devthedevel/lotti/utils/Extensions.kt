package net.devthedevel.lotti.utils

import java.util.*

fun <T : Any> List<T>.random(): T {
    return this[Random().nextInt(this.size)]
}

fun IntRange.delta(): Int = this.endInclusive - this.start + 1