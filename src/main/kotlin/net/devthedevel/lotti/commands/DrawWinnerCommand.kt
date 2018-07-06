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

    private val numOfWinners: Int = if (context.arguments.isNotEmpty()) context.arguments.removeAt(0).toInt() else 1

    override fun execute() {
        val (op, result) = LotteryDatabase.getApprovedTickets(context.guild, context.channel)

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    if (result.userTicketList.isNotEmpty()) {
                        withContent("Drawing $numOfWinners winners...\n\n")
                        result.getWinners(numOfWinners).mapNotNull { context.guild.getUserByID(it) }.forEach {
                            appendContent("- ${it.mention(true)}!")
                        }
                        LotteryDatabase.deleteLotto(context.guild, context.channel)
                    } else {
                        withContent("Huh, looks like no one has any approved tickets...")
                    }
                }
                else -> return sendInvalidCommandMessage()
            }
            send()
        }
    }
}