package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class UserRequestTicketCommand(context: CommandContext): Command(context) {
    var numTickets: Int = 0

    init {
        if (context.arguments.isNotEmpty()) numTickets = context.arguments[0].toInt()
    }

    companion object {
        const val COMMAND_NAME: String = "buy"
    }

    override fun execute() {
        val (_, _, adminRoles) = LotteryDatabase.getAdminOptions(context.guild)

        val isAdmin = adminRoles.intersect(context.sender.getRolesForGuild(context.guild)).isNotEmpty()

        val (op, approved, requested) = LotteryDatabase.userBuyTickets(context.guild, context.channel, context.sender, numTickets, isAdmin)

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    withContent("${context.sender.mention(true)} has bought $numTickets tickets. You have $approved approved tickets, and $requested requested tickets.")
                }
                OperationStatus.DOES_NOT_EXIST -> {
                    withContent("Hey ${context.sender.mention(true)}, I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                }
                else -> return InvalidCommand(context).execute()
            }
            send()
        }
    }
}