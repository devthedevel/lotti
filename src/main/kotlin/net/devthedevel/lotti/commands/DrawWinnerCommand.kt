package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class DrawWinnerCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {
    private var numOfWinners: Int = 1

    init {
        if (parameters.isNotEmpty()) {
            numOfWinners = parseToInt(parameters.removeAt(0), 0, sendInvalidMessage = true, invalidMessage = "Defaulting to one winner...") ?: 1
        }
    }

    override fun validate(): Boolean {
        return numOfWinners > 0 && numOfWinners < Int.MAX_VALUE
    }

    override fun sendInvalidMessage() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")
            appendContent("Well I need to know how many winners to select...")

            send()
        }
    }

    override fun execute() {
        sendMessage(context.channel, context.sender) {
            if (context.isAdmin) {
                val (op, result) = LotteryDatabase.getApprovedTickets(context.guild, context.channel)

                when (op) {
                    OperationStatus.COMPLETED -> {
                        if (result.userTicketList.isNotEmpty()) {
                            val boundedNumOfWinners = if (numOfWinners > result.userTicketList.size) result.userTicketList.size else numOfWinners
                            appendContent("Drawing $boundedNumOfWinners winners...\n\n")
                            result.getWinners(boundedNumOfWinners).mapNotNull { context.guild.getUserByID(it) }.forEach {
                                appendContent("- ${it.mention(true)}!\n")
                            }
                            LotteryDatabase.deleteLotto(context.guild, context.channel)
                        } else {
                            appendContent("Huh, looks like no one has any approved tickets...")
                        }
                    }
                    OperationStatus.DOES_NOT_EXIST -> {
                        appendContent("If there's no lottery then how can anyone be a winner?")
                    }
                    else -> sendInvalidCommandMessage()
                }
            } else {
                appendContent("Well this is awkward. You need to be an admin to actually draw from the lottery.")
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "draw"
    }
}