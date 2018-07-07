package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.utils.random
import sx.blah.discord.util.MessageBuilder

class DrawWinnerCommand(context: CommandContext): Command(context) {
    companion object {
        const val COMMAND_NAME: String = "draw"
    }

    private var numOfWinners: Int = 1

    init {
        val arg: String
        if (context.arguments.isNotEmpty()) {
            try {
                arg = context.arguments.removeAt(0)
                val num = arg.toDouble().toInt()
                numOfWinners = if (num > 0) num else 1
            } catch (e: NumberFormatException) {
                sendInvalidCommandMessage(false, "Defaulting to one winner.." )
            }
        }
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")

            if (context.isAdmin) {
                val (op, result) = LotteryDatabase.getApprovedTickets(context.guild, context.channel)

                when (op) {
                    OperationStatus.COMPLETED -> {
                        if (result.userTicketList.isNotEmpty()) {
                            appendContent("Drawing $numOfWinners winners...\n\n")
                            result.getWinners(numOfWinners).mapNotNull { context.guild.getUserByID(it) }.forEach {
                                appendContent("- ${it.mention(true)}!")
                            }
                            LotteryDatabase.deleteLotto(context.guild, context.channel)
                        } else {
                            withContent("Huh, looks like no one has any approved tickets...")
                        }
                    }
                    OperationStatus.DOES_NOT_EXIST -> {
                        appendContent("If there's no lottery then how can anyone be a winner?")
                    }
                    else -> return sendInvalidCommandMessage()
                }
            } else {
                appendContent("Well this is awkward. You need to be an admin to actually draw from the lottery.")
            }
            send()
        }
    }
}