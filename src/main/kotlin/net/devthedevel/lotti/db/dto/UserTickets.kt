package net.devthedevel.lotti.db.dto

data class UserTickets(val userId: Long = -1L, var range: IntRange = IntRange.EMPTY)