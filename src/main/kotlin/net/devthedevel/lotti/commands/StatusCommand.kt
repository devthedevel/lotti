package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class StatusCommand(context: CommandContext): Command(context) {
    companion object {
        const val COMMAND_NAME: String = "status"
    }

    private val scope: String? = if (context.arguments.size == 0) null else context.arguments[0]

    override fun execute() {
        if (scope != null) {

        }

        val (creatorId, currencyName, ticketPrice, users, result) = LotteryDatabase.getChannelStatus(context.guild, context.channel)

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            when (result) {
                OperationStatus.COMPLETED -> {
                    val creatorUser = context.guild.getUserByID(creatorId ?: 0)
                    val creatorName = creatorUser.getNicknameForGuild(context.guild)?: creatorUser.getDisplayName(context.guild)
                    withContent("Here's this channel's lottery status:\n\n")
                    appendContent("Creator: $creatorName, Currency: $currencyName, Ticket Price: $ticketPrice\n")
                    appendContent("People entered:\n")
                    for (userTicket: Pair<Long, Int> in users) {
                        val user = context.guild.getUserByID(userTicket.first)
                        val userName = user.getNicknameForGuild(context.guild)?: user.getDisplayName(context.guild)
                        appendContent("$userName: ${userTicket.second} requested")
                    }
                }
                OperationStatus.DOES_NOT_EXIST -> withContent("Hey ${context.sender.mention(true)}, I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                else -> return InvalidCommand(context).execute()
            }
            send()
        }

    }
}