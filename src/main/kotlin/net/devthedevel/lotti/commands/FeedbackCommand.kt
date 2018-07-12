package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class FeedbackCommand(context: CommandContext) : Command(context) {
    companion object {
        const val COMMAND_NAME: String = "feedback"
    }

    var feedback: String = String()

    init {
       if (context.arguments.isNotEmpty()) {
           context.arguments.forEach {
               feedback += "$it "
           }
           feedback.trim()
       }
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            appendContent(context.sender.mention(true) + "\n")

            if (feedback.isNotBlank()) {
                val op = LotteryDatabase.sendFeedback(context.sender, feedback)
                when (op) {
                    OperationStatus.COMPLETED -> appendContent("Feedback recieved! Thanks for criticizing me!")
                    else -> return sendInvalidCommandMessage()
                }
            } else {
                appendContent("I would love to hear your thoughts on the wonderful, amazing me, buuuuuuuut you need to actually write something about me first!")
            }
            send()
        }
    }
}