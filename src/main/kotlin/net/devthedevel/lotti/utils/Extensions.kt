package net.devthedevel.lotti.utils

import java.util.*

fun <T : Any> MutableList<T>.random(): T {
    return this[Random().nextInt(this.size)]
}