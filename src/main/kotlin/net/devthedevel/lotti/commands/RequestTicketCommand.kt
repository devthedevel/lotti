package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.utils.getDiscordId
import net.devthedevel.lotti.utils.isDiscordId
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageBuilder

class RequestTicketCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {
    private var numTickets: Int = 0
    private var targetUser: IUser = context.sender

    init {
        //Determine if sender is gifting tickets
        if (parameters.size == 2) {
            val user = parameters.removeAt(0)

            if (user.isDiscordId()) {
                targetUser = context.guild.getUserByID(user.getDiscordId())
            } else {
                val users = context.guild.getUsersByName(user, true)

                if (users.isNotEmpty()) {
                    targetUser = users[0]
                }
            }
        }
        if (parameters.isNotEmpty()) {
            numTickets = parseToInt(parameters.removeAt(0), 0) ?: 0
        }
    }

    override fun validate(): Boolean {
        return numTickets > 0 && numTickets < Int.MAX_VALUE
    }

    override fun sendInvalidMessage() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")

            when (numTickets) {
                0 -> appendContent("You need to tell me how many tickets you want to buy!")
                in Int.MIN_VALUE..-1 -> appendContent("You can't buy negative tickets silly goose. Try that again.")
            }

            send()
        }
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")

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
                else -> return InvalidCommand(context, parameters).execute()
            }
            send()
        }
    }

    companion object {
        const val COMMAND_NAME: String = "buy"
    }
}