package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class RequestTicketCommand(context: CommandContext): Command(context) {
    var numTickets: Int = 0

    init {
        if (context.arguments.isNotEmpty()) numTickets = context.arguments[0].toInt()
    }

    companion object {
        const val COMMAND_NAME: String = "buy"
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")

            //Check if numTickets is positive. Cannot buy 'negative' tickets
            if (numTickets > 0) {
                val (_, _, adminRoles) = LotteryDatabase.getAdminOptions(context.guild)
                val isAdmin = adminRoles.intersect(context.sender.getRolesForGuild(context.guild)).isNotEmpty()

                val (op, approved, requested) = LotteryDatabase.userBuyTickets(context.guild, context.channel, context.sender, numTickets, isAdmin)

                when (op) {
                    OperationStatus.COMPLETED -> {
                        appendContent("You bought $numTickets tickets. You have $approved approved tickets, and $requested requested tickets.")
                    }
                    OperationStatus.DOES_NOT_EXIST -> {
                        appendContent("I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                    }
                    else -> return InvalidCommand(context).execute()
                }
            } else if (numTickets < 0) {
                appendContent("You can't buy negative tickets silly goose. Try that again.")
            } else {
                appendContent("How does one buy no tickets? If you know, can you please update my code so I can handle it in the future? K thanks.")
            }
            send()
        }
    }
}