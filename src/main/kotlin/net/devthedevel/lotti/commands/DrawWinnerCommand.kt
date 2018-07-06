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

    override fun execute() {
        val (op, result) = LotteryDatabase.getUsersInLotto(context.guild, context.channel)

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    if (result.isNotEmpty()) {
                        val winner = context.guild.getUserByID(result.random())
                        LotteryDatabase.deleteLotto(context.guild, context.channel)
                        withContent("Drawing winner...\n\n")
                        appendContent("And our winner is: ${winner.mention(true)}! Congrats!")
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