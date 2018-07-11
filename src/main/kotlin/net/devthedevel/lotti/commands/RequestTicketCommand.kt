package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageBuilder

class RequestTicketCommand(context: CommandContext): Command(context) {
    private var numTickets: Int = 0
    private var targetUser: IUser = context.sender

    init {
        //Determine if sender is gifting tickets
        if (context.arguments.size == 2) {
            val user = context.arguments.removeAt(0)

            if (user.startsWith("<@") && user.endsWith(">")) {
                targetUser = context.guild.getUserByID(user.removePrefix("<@").removeSuffix(">").toLong())
            } else {
                val users = context.guild.getUsersByName(user, true)

                if (users.isNotEmpty()) {
                    targetUser = users[0]
                }
            }
        }
        if (context.arguments.isNotEmpty()) {
            numTickets = parseToInt(context.arguments.removeAt(0), 0) ?: 0
        }
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
                val (op, approved, requested) = LotteryDatabase.userBuyTickets(context.guild, context.channel, targetUser, numTickets, context.isAdmin)

                when (op) {
                    OperationStatus.COMPLETED -> {
                        if (targetUser == context.sender) {
                            appendContent("You bought $numTickets tickets. You have $approved approved tickets, and $requested requested tickets.")
                        } else {
                            appendContent("You bought ${targetUser.mention(true)} $numTickets tickets! They have $approved approved tickets, and $requested requested tickets.")
                        }
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