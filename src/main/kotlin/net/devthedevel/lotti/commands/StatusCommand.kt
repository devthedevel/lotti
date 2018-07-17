package net.devthedevel.lotti.commands

import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus

class StatusCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun execute() {
        val (creatorId, currencyName, ticketPrice, users, result) = LotteryDatabase.getChannelStatus(context.guild, context.channel)

        sendMessage(context.channel, context.sender) {
            when (result) {
                OperationStatus.COMPLETED -> {
                    val creatorUser = context.guild.getUserByID(creatorId ?: 0)
                    val creatorName = creatorUser.getNicknameForGuild(context.guild)?: creatorUser.getDisplayName(context.guild)
                    +"Here's this channel's lottery status:\n\n"
                    +"Creator: $creatorName, Currency: $currencyName, Ticket Price: $ticketPrice\n"
                    if (users.isNotEmpty()) {
                        +"People entered:\n"
                        for (userTicket: Pair<Long, Int> in users) {
                            val user = context.guild.getUserByID(userTicket.first)
                            val userName = user.getNicknameForGuild(context.guild)?: user.getDisplayName(context.guild)
                            +"$userName: ${userTicket.second} tickets\n"
                        }
                    } else {
                        +"Oh...no one wants to play $creatorName's game. Is it because they smell?"
                    }
                }
                OperationStatus.DOES_NOT_EXIST -> +"Hey I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one."
                else -> sendInvalidMessage()
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "status"
    }
}