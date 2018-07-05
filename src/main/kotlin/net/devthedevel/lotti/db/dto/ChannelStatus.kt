package net.devthedevel.lotti.db.dto

import net.devthedevel.lotti.db.OperationStatus

data class ChannelStatus(var creatorId: Long? = null,
                         var currencyName: String? = null,
                         var ticketPrice: Int? = null,
                         var userTickets: MutableList<Pair<Long, Int>> = mutableListOf(),
                         override var operationStatus: OperationStatus = OperationStatus.FAILED) : IOperationStatus