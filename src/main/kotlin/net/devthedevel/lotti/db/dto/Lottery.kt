package net.devthedevel.lotti.db.dto

import net.devthedevel.lotti.utils.delta
import java.util.*

class Lottery {

    private var lastIndex: Int = 0
    val userTicketList: MutableList<UserTickets> = mutableListOf()

    fun addUserTicket(user: Long, tickets: Int) {
        userTicketList.add(UserTickets(user, IntRange(lastIndex, tickets + lastIndex - 1)))
        lastIndex += tickets
    }

    fun getWinners(numOfWinners: Int): List<Long> {
        val winners = mutableListOf<Long>()
        for (i in 0 until numOfWinners) {
            winners.add(random().userId)
        }
        return winners
    }

    private fun random(): UserTickets {
        var ret = UserTickets()
        var hasWinner = false
        val rand = Random().nextInt(userTicketList.last().range.last)
        for (user in userTicketList) {
            if (!hasWinner) {
                if (rand in user.range) {
                    ret = user
                    hasWinner = true
                }
            } else {
                user.range = IntRange(user.range.start - ret.range.delta(), user.range.endInclusive - ret.range.delta())
            }
        }
        userTicketList.remove(ret)
        return ret
    }
}